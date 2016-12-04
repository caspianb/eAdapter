package builders;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import eAdapter.Document;
import eAdapter.Representative;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Builds a list of documents from a list of lines split on a comma 
 *          that represent either a single document or a group of documents.
 */
public class OpticonBuilder {
    private final int IMAGE_KEY_INDEX = 0;
    private final int VOLUME_NAME_INDEX = 1;
    private final int FULL_PATH_INDEX = 2;
    private final int DOC_BREAK_INDEX = 3;
    private final int BOX_BREAK_INDEX = 4;
    private final int FOLDER_BREAK_INDEX = 5;
    private final int PAGE_COUNT_INDEX = 6;
    private final String TRUE_VALUE = "Y";
    private final String IMAGE_KEY_FIELD = "DocID";
    private final String VOLUME_NAME_FIELD = "Volume Name";
    private final String PAGE_COUNT_FIELD = "Page Count";
    private final String BOX_BREAK_FIELD = "Box Break";
    private final String FOLDER_BREAK_FIELD = "Folder Break";
    private final String TEXT_EXT = ".txt";
    private final String DEFAULT_IMAGE_REP_NAME = "default";
    private final String DEFAULT_TEXT_REP_NAME = "default";
    
    /**
     * Levels that a text representative can be.
     */
    public enum TextLevel {
        /**
         * No text representative exists.
         */
        None,
        /**
         * The text representative has a text file that corresponds to each page in the document
         * and it must be accompanied by a page level image.
         */
        Page,
        /**
         * The text representative has a single text file that contains the text for all pages 
         * of the document. The text file base name matches the image file base name of the 
         * first page of the document.
         */
        Doc
    }
    
    /**
     * Locations where a text representative can be.
     */
    public enum TextLocation {
        /**
         * No text representative exists.
         */
        None,
        /**
         * The text files reside in the same location as the image files.
         */
        SameAsImages,
        /**
         * The text files reside in an alternate location.
         * A find/replace operation will transform the image path into the text path.
         */
        AlternateLocation,
    }
    
    /**
     * Builds a list of documents from an opticon file with no text representatives and uses the default image representative name
     * @param lines the lines read from an opt file split on a comma
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines) {
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, TextLevel.None, TextLocation.None, null);
    }
    
    /**
     * Builds a list of documents from an opticon file with no text representatives.
     * @param lines the lines read from an opt file split on a comma
     * @param imagesName the name of the image representative
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, String imagesName) {
        return buildDocuments(lines, imagesName, DEFAULT_TEXT_REP_NAME, TextLevel.None, TextLocation.None, null);
    }
    
    /**
     * Builds a list of documents from an opticon file using the default image and text representative names
     * @param lines the lines read from an opt file split on a comma
     * @param textLevel the level of the text representative
     * @param textLocation the location of the text representative
     * @param textPathFindReplace a pair containing a pattern to find all image path specific 
     *        elements in the image path and the string to replace them with. The result of this 
     *        operation should be the transformation of the image path into the text path.
     *        The file extension of the image file name will automatically be updated to '.txt'.
     *        The base name of the image file and text file are expected to be identical.
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, 
            TextLevel textLevel, TextLocation textLocation, Pair<Pattern, String> textPathFindReplace) {
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textLevel, textLocation, textPathFindReplace);        
    } 
    
    /**
     * Builds a list of documents from an opticon file
     * @param lines the lines read from an opt file split on a comma
     * @param imagesName the name of images representative
     * @param textName the name of the text representative
     * @param textLevel the level of the text representative        
     * @param textLocation the location of the text representative        
     * @param textPathFindReplace a pair containing a pattern to find all image path specific 
     *        elements in the image path and the string to replace them with. The result of this 
     *        operation should be the transformation of the image path into the text path.
     *        The file extension of the image file name will automatically be updated to '.txt'.
     *        The base name of the image file and text file are expected to be identical.
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, String imagesName, 
            String textName, TextLevel textLevel, TextLocation textLocation, Pair<Pattern, String> textPathFindReplace) {
        // setup for building
        Map<String, Document> docs = new LinkedHashMap<>();
        List<String[]> docPages = new ArrayList<>();
        // build the documents
        for (String[] line : lines) {
            if (line[DOC_BREAK_INDEX].toUpperCase().equals(TRUE_VALUE)) {
                // send data to make a document
                if (docPages.size() > 0) {
                    Document doc = buildDocument(docPages, imagesName, textName, textLevel, textLocation, textPathFindReplace);
                    String key = doc.getMetadata().get(IMAGE_KEY_FIELD);
                    docs.put(key, doc);
                }
                // clear docPages and add new first page
                docPages = new ArrayList<>();
                docPages.add(line);
            }
            else {
                // add page to document pages
                docPages.add(line);
            }
        }
        // add last doc to the collection
        Document doc = buildDocument(docPages, imagesName, textName, textLevel, textLocation, textPathFindReplace);
        String key = doc.getMetadata().get(IMAGE_KEY_FIELD);
        docs.put(key, doc);

        return new ArrayList<>(docs.values());
    }
    
    /**
     * Builds a single document using the default image representative name, which has no text representatie
     * @param docPages a list of opticon page records split on a comma
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, TextLevel.None, TextLocation.None, null);
    }
    
    /**
     * Builds a single document, which has no text representative
     * @param docPages a list of opticon page records split on a comma
     * @param imagesName the name of the image representative
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName) {
        return buildDocument(docPages, imagesName, DEFAULT_TEXT_REP_NAME, TextLevel.None, TextLocation.None, null);
    }
    
    /**
     * Builds a single document using the default image and text represntative names
     * @param docPages a list of opticon page records split on a comma
     * @param textLevel the level of the text representative
     * @param textLocation the location of the text representative
     * @param textPathFileReplace a pair containing a pattern to find all image path specific 
     *        elements in the image path and the string to replace them with. The result of this 
     *        operation should be the transformation of the image path into the text path.
     *        The file extension of the image file name will automatically be updated to '.txt'.
     *        The base name of the image file and text file are expected to be identical.
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, TextLevel textLevel, TextLocation textLocation, Pair<Pattern, String> textPathFileReplace) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textLevel, textLocation, textPathFileReplace);
    }
    
    /**
     * Builds a single document
     * @param docPages a list of opticon page records split on a comma
     * @param imagesName the name of the images representative
     * @param textName the name of the text representative
     * @param textLevel the level of the text representative
     * @param textLocation the location of the text representative
     * @param textPathFindReplace a pair containing a pattern to find all image path specific 
     *        elements in the image path and the string to replace them with. The result of this 
     *        operation should be the transformation of the image path into the text path.
     *        The file extension of the image file name will automatically be updated to '.txt'.
     *        The base name of the image file and text file are expected to be identical.
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName,
            String textName, TextLevel textLevel, TextLocation textLocation, Pair<Pattern, String> textPathFindReplace) {
        // setup for building
        Document doc = new Document();
        // get document properties
        String[] pageOne = docPages.get(0);
        String key = pageOne[IMAGE_KEY_INDEX];
        String vol = pageOne[VOLUME_NAME_INDEX];
        String box = pageOne[BOX_BREAK_INDEX];
        String dir = pageOne[FOLDER_BREAK_INDEX];
        int pages = docPages.size(); // do we need to check the field value here?
        // set document properties
        doc.addField(IMAGE_KEY_FIELD, key);
        doc.addField(VOLUME_NAME_FIELD, vol);
        doc.addField(PAGE_COUNT_FIELD, Integer.toString(pages));
        doc.addField(BOX_BREAK_FIELD, box);
        doc.addField(FOLDER_BREAK_FIELD, dir);
        // get image representative
        Set<String> representativeFiles = new LinkedHashSet<>();
        docPages.forEach(page -> representativeFiles.add(page[FULL_PATH_INDEX]));
        Representative imageRep = new Representative();
        imageRep.setName(imagesName);
        imageRep.setType(Representative.Type.IMAGE);
        imageRep.setFiles(representativeFiles);
        // get text representative        
        //List<String> textFiles = new ArrayList<>();
        Set<String> textFiles = new LinkedHashSet<>();
        // add textFiles
        switch (textLevel) {
            case None:
                // do nothing here
                break;
            case Page:
                docPages.forEach(page -> {
                    String textFile = getTextFileFromPageInfo(page, textLocation, textPathFindReplace);
                    textFiles.add(textFile);
                });
                break;
            case Doc:
                String[] firstPageInfo = docPages.get(0);
                String textFile = getTextFileFromPageInfo(firstPageInfo, textLocation, textPathFindReplace);
                textFiles.add(textFile);
                break;
            default:
                // do nothing here
                break;
        }
        Representative textRep = new Representative();
        textRep.setName(textName);
        textRep.setType(Representative.Type.TEXT);
        textRep.setFiles(textFiles);
        // set representatives
        Set<Representative> reps = new HashSet<>();
        reps.add(imageRep);
        // if there are text files add the representative
        if (textFiles.size() > 0) {
            reps.add(textRep);
        }            
        doc.setRepresentatives(reps);
        // return built doc
        return doc;
    }
    
    private String getTextFileFromPageInfo(String[] pageInfo, TextLocation textLocation, Pair<Pattern, String> textPathFindReplace) {
        String imagePath = pageInfo[FULL_PATH_INDEX];
        String textFolder = FilenameUtils.getFullPath(imagePath);
        String textFile = FilenameUtils.getBaseName(imagePath) + TEXT_EXT;
        
        
        switch(textLocation) {            
            case SameAsImages:
                // nothing to replace
                // do nothing here
                break;
            case AlternateLocation:
                Matcher m = textPathFindReplace.getLeft().matcher(textFolder);
                textFolder = m.replaceAll(textPathFindReplace.getRight());
                break;
            default:
                // do nothing here
                break;
        }
        
        return Paths.get(textFolder, textFile).toString(); 
    }

}
