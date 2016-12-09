package builders;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import eAdapter.Document;
import eAdapter.Representative;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Builds documents from lfp records
 */
public class LfpBuilder {
    private final String PAGE_REGEX_SPLITTER = ",|;";
    private final int TOKEN_INDEX = 0;
    private final int KEY_INDEX = 1;
    private final int IMAGE_BOUNDARY_FLAG_INDEX = 2;
    @SuppressWarnings("unused")
    private final int IMAGE_OFFSET_INDEX = 3;
    private final int IMAGE_VOLUME_NAME_INDEX = 4;
    private final int IMAGE_FILE_PATH_INDEX = 5;
    private final int IMAGE_FILE_NAME_INDEX = 6;
    @SuppressWarnings("unused")
    /**
     * TIF = 2
     * JPG = 4
     * PDF = 7
     */
    private final int IMAGE_TYPE_INDEX = 7;
    @SuppressWarnings("unused")
    private final int IMAGE_ROTATION_INDEX = 8;
    private final int NATIVE_VOLUME_NAME_INDEX = 2;
    private final int NATIVE_FILE_PATH_INDEX = 3;
    private final int NATIVE_FILE_NAME_INDEX = 4;
    @SuppressWarnings("unused")
    private final int NATIVE_OFFSET_INDEX = 5;
    private final String KEY_FIELD = "DocID";
    private final String VOLUME_NAME_FIELD = "Volume Name";
    private final String PAGE_COUNT_FIELD = "Page Count";
    private final String EMPTY_STRING = "";
    private final String VOLUME_TRIM_REGEX = "^@";
    private final String DEFAULT_IMAGE_REP_NAME = "default";
    private final String DEFAULT_TEXT_REP_NAME = "default";
    private final String DEFAULT_NATIVE_REP_NAME = "default";
    
    /**
     * LFP record type
     */
    private enum Token {
        /**
         * LFP image record
         */
        IM,
        /**
         * LFP native record
         */
        OF
    }
    
    /**
     * Document boundary type
     */
    private enum BoundaryFlag {
        /**
         * Parent or stand alone document
         */
        D,
        /**
         * Child document which is preceded by it's parent
         */
        C
    }
    
    /**
     * Builds a list of documents from a LFP file
     * @param lines a list of lines from a LFP file
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String> lines) {
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_NATIVE_REP_NAME, DEFAULT_TEXT_REP_NAME, null);
    }
    
    /**
     * Builds a list of documents from a LFP file
     * @param lines a list of lines from a LFP file
     * @param textSetting the text representative settings
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String> lines, StructuredRepresentativeSetting textSetting) {
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_NATIVE_REP_NAME, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a list of documents from a LFP file
     * @param lines a list of lines from a LFP file
     * @param imagesName the name of the image representative
     * @param nativeName the name of the native representative
     * @param textName the name of the text representative
     * @param textSetting the text representative settings
     * @return returns a list documents
     */
    public List<Document> buildDocuments(List<String> lines, String imagesName, String nativeName, String textName, StructuredRepresentativeSetting textSetting) {
        // setup for building
        Map<String, Document> docs = new LinkedHashMap<>(); // maps key to document
        List<String[]> docPages = new ArrayList<>(); // all page records for a single document
        String[] nativeLine = null; // native record
        Document lastParent = null;
        // build the documents
        for(String line : lines) {
            String[] lineSegments = line.split(PAGE_REGEX_SPLITTER);
            Token token = Token.valueOf(lineSegments[TOKEN_INDEX]);
            // determine if the line is an image or native
            switch(token) {
                case IM:
                    // check for a doc break
                    if (StringUtils.isNotBlank(lineSegments[IMAGE_BOUNDARY_FLAG_INDEX])) {
                        // send data to make a document if there is data to send
                        // this is a guard against the first line in the list
                        if (docPages.size() > 0) {
                            Document doc = buildDocument(docPages, imagesName, nativeName, nativeLine, textName, textSetting);
                            String key = doc.getMetadata().get(KEY_FIELD);
                            BoundaryFlag docBreak = BoundaryFlag.valueOf(lineSegments[IMAGE_BOUNDARY_FLAG_INDEX]);
                            // check if document is a child
                            if (docBreak.equals(BoundaryFlag.C)) {
                                setRelationships(doc, lastParent);
                            }
                            else {
                                // document is a parent
                                lastParent = doc;
                            }
                                
                            docs.put(key, doc);
                        }
                        // clear docPages and add new first page
                        docPages = new ArrayList<>();                        
                        docPages.add(lineSegments);
                        // check if native belongs to this doc
                        // this is a guard against the native appearing before the first image
                        if (nativeLine != null && !nativeLine[KEY_INDEX].equals(lineSegments[KEY_INDEX])) {
                            nativeLine = null;
                        }                        
                    }
                    else {
                        // add page to document pages
                        docPages.add(lineSegments);
                    }
                    break;
                case OF:
                    // check if native line is blank
                    // it should be blank after an image line with a doc break is read that doesn't match the key
                    if (nativeLine == null) {
                        nativeLine = lineSegments;
                    }
                    else {
                        // this is a guard against a native with no corresponding images
                        // send data to make a document
                        Document doc = buildDocument(docPages, imagesName, nativeName, nativeLine, textName, textSetting);
                        String key = doc.getMetadata().get(KEY_FIELD);
                        docs.put(key,  doc);
                        // add the current line to native line
                        nativeLine = lineSegments;
                    }
                    break;
                default:
                    throw new RuntimeException("Invalid LFP token encountered.");
            }
        }
        // add last doc to the collection
        Document doc = buildDocument(docPages, imagesName, nativeName, nativeLine, textName, textSetting);
        String key = doc.getMetadata().get(KEY_FIELD);
        // check if a relationship needs to be set
        if (docPages.get(0)[TOKEN_INDEX].equals(Token.IM.toString()) && docPages.get(0)[IMAGE_BOUNDARY_FLAG_INDEX].equals(BoundaryFlag.C.toString())) {
            setRelationships(doc, lastParent);
        }
        docs.put(key, doc);        
        // return documents
        return new ArrayList<>(docs.values());
    }
    
    /**
     * Build a single document using the default representative name and with an image representative only
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_NATIVE_REP_NAME, null, DEFAULT_TEXT_REP_NAME, null);
    }
    
    /**
     * Build a single document with an image representative only
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param imagesName the name of the image representative
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName) {
        return buildDocument(docPages, imagesName, DEFAULT_NATIVE_REP_NAME, null, DEFAULT_TEXT_REP_NAME, null);
    }
    
    /**
     * Build a single document using the default representative names and with no native representative
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param textSetting the text representative settings
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, StructuredRepresentativeSetting textSetting) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_NATIVE_REP_NAME, null, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Build a single document with no native representative
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param imagesName the name of the image representative
     * @param textName the name of the text representative
     * @param textSetting the text representative settings
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName, String textName, StructuredRepresentativeSetting textSetting) {
        return buildDocument(docPages, imagesName, DEFAULT_NATIVE_REP_NAME, null, textName, textSetting);
    }
    
    /**
     * Build a single document using the default representative names and with no text representative
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param nativeLine the LFP native record split on a comma or a semicolon
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, String[] nativeLine) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_NATIVE_REP_NAME, nativeLine, DEFAULT_TEXT_REP_NAME, null);
    }
    
    /**
     * Builds a single document with no text representative 
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param imagesName the name of the image representative
     * @param nativeName the name of the native representative
     * @param nativeLine the LFP native record split on a comma or a semicolon
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName, String nativeName, String[] nativeLine) {
        return buildDocument(docPages, imagesName, nativeName, nativeLine, DEFAULT_TEXT_REP_NAME, null);
    }
    
    /**
     * Builds a single document 
     * @param docPages a list of LFP page records split on a comma or a semicolon
     * @param imagesName the name of the image representative
     * @param nativeName the name of the native representative
     * @param nativeLine the LFP native record split on a comma or a semicolon
     * @param textName the name of the text representative
     * @param textSetting the text representative settings
     * @return returns a document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName, String nativeName, String[] nativeLine, 
            String textName, StructuredRepresentativeSetting textSetting) {
        // setup for building
        Document doc = new Document();
        Representative imageRep = null;
        Representative textRep = null;
        Representative nativeRep = null;
        StructuredRepresentativeSetting.TextLevel textLevel = (textSetting != null) ? textSetting.getTextLevel() : StructuredRepresentativeSetting.TextLevel.None;
        // check if this doc has images or is native only then get document properties
        String key = (docPages.size() > 0) ? docPages.get(0)[KEY_INDEX] : nativeLine[KEY_INDEX];
        String vol = (docPages.size() > 0) ? docPages.get(0)[IMAGE_VOLUME_NAME_INDEX] : nativeLine[NATIVE_VOLUME_NAME_INDEX];
        vol = vol.replaceAll(VOLUME_TRIM_REGEX, EMPTY_STRING);
        int pages = docPages.size(); // this could be zero
        String pagesValue = (pages > 0) ? Integer.toString(pages) : EMPTY_STRING;
        // set document properties
        doc.setKey(key);
        doc.addField(KEY_FIELD, key);
        doc.addField(VOLUME_NAME_FIELD, vol);
        doc.addField(PAGE_COUNT_FIELD, pagesValue);
        // get image representative
        if (docPages.size() > 0) {
            Set<String> imageFiles = new LinkedHashSet<>();
            // note: in the case of a multi-page reference duplicates aren't inserted due to the linked hash set
            docPages.forEach(page -> imageFiles.add(Paths.get(page[IMAGE_FILE_PATH_INDEX], page[IMAGE_FILE_NAME_INDEX]).toString())); 
            imageRep = new Representative();
            imageRep.setName(imagesName);
            imageRep.setType(Representative.Type.IMAGE);
            imageRep.setFiles(imageFiles);
        }
        // get text representative
        if (!textLevel.equals(StructuredRepresentativeSetting.TextLevel.None)) {
            Set<String> textFiles = new LinkedHashSet<>();
            // add textFiles
            if (textLevel.equals(StructuredRepresentativeSetting.TextLevel.Page)) {
                docPages.forEach(page -> {
                    String imagePath = Paths.get(page[IMAGE_FILE_PATH_INDEX], page[IMAGE_FILE_NAME_INDEX]).toString();
                    String textFile = textSetting.getTextPathFromImagePath(imagePath); 
                    textFiles.add(textFile);
                });                
            }
            else if (textLevel.equals(StructuredRepresentativeSetting.TextLevel.Doc)) {
                String[] firstPageInfo = docPages.get(0);
                String imagePath = Paths.get(firstPageInfo[IMAGE_FILE_PATH_INDEX], firstPageInfo[IMAGE_FILE_NAME_INDEX]).toString();
                String textFile = textSetting.getTextPathFromImagePath(imagePath);
                textFiles.add(textFile);
            }
            // set text properties
            textRep = new Representative();
            textRep.setName(textName);
            textRep.setType(Representative.Type.TEXT);
            textRep.setFiles(textFiles);
        }
        // get native representative
        if (nativeLine != null) {
            Set<String> nativeFiles = new LinkedHashSet<>();
            String nativeFile = Paths.get(nativeLine[NATIVE_FILE_PATH_INDEX], nativeLine[NATIVE_FILE_NAME_INDEX]).toString();
            nativeFiles.add(nativeFile);
            nativeRep = new Representative();            
            nativeRep.setName(nativeName);
            nativeRep.setType(Representative.Type.NATIVE);
            nativeRep.setFiles(nativeFiles);
        }
        // prep representatives
        Set<Representative> reps = new HashSet<>();
        // check if an image representative exists
        if (imageRep != null) {
            reps.add(imageRep);            
        }
        // check if a text representative exists
        if (textRep != null) {
            reps.add(textRep);
        }
        // check if a native representative exists
        if (nativeRep != null) {
            reps.add(nativeRep);
        }
        // set representatives
        doc.setRepresentatives(reps);
        // return built doc
        return doc;
    }
    
    private void setRelationships(Document doc, Document parent) {
        doc.setParent(parent);
        // now add this document as a child to the parent
        List<Document> children = parent.getChildren();
        children.add(doc);
        parent.setChildren(children);
    }
}
