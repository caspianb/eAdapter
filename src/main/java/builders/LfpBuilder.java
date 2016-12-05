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

public class LfpBuilder {
    private final String PAGE_REGEX_SPLITTER = ",|;";
    private final int TOKEN_INDEX = 0;
    private final int KEY_INDEX = 1;
    private final int IMAGE_BOUNDARY_FLAG_INDEX = 2;
    private final int IMAGE_OFFSET_INDEX = 3;
    private final int IMAGE_VOLUME_NAME_INDEX = 4;
    private final int IMAGE_FILE_PATH_INDEX = 5;
    private final int IMAGE_FILE_NAME_INDEX = 6;
    private final int IMAGE_TYPE_INDEX = 7;
    private final int IMAGE_ROTATION_INDEX = 8;
    private final int NATIVE_VOLUME_NAME_INDEX = 2;
    private final int NATIVE_FILE_PATH_INDEX = 3;
    private final int NATIVE_FILE_NAME_INDEX = 4;
    private final int NATIVE_OFFSET_INDEX = 5;
    private final String KEY_FIELD = "DocID";
    private final String VOLUME_NAME_FIELD = "Volume Name";
    private final String PAGE_COUNT_FIELD = "Page Count";
    private final String EMPTY_STRING = "";
    private final String VOLUME_TRIM_REGEX = "^@";
    
    private enum Token {
        IM,
        OF
    }
    
    private enum ImageType {
        TIF(2),
        JPG(4),
        PDF(7);
        
        private int value;
        
        ImageType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return this.value;
        }
    }
    
    private enum BoundaryFlag {
        D,
        C
    }
    
    public List<Document> buildDocuments(List<String> lines, String imagesName, String nativeName, String textName, TextRepresentativeSetting textSetting) {
        // setup for building
        Map<String, Document> docs = new LinkedHashMap<>(); // maps key to document
        List<String[]> docPages = new ArrayList<>(); // all page records for a single document
        String[] nativeLine = null; 
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
        docs.put(key, doc);        
        // return documents
        return new ArrayList<>(docs.values());
    }
    
    public Document buildDocument(List<String[]> docPages, String imagesName, String nativeName, String[] nativeLine, 
            String textName, TextRepresentativeSetting textSetting) {
        // setup for building
        Document doc = new Document();
        Representative imageRep = null;
        Representative textRep = null;
        Representative nativeRep = null;
        TextRepresentativeSetting.TextLevel textLevel = textSetting.getTextLevel();
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
            docPages.forEach(page -> imageFiles.add(Paths.get(page[IMAGE_FILE_PATH_INDEX], page[IMAGE_FILE_NAME_INDEX]).toString())); 
            imageRep = new Representative();
            imageRep.setName(imagesName);
            imageRep.setType(Representative.Type.IMAGE);
            imageRep.setFiles(imageFiles);
        }
        // get text representative
        if (!textLevel.equals(TextRepresentativeSetting.TextLevel.None)) {
            Set<String> textFiles = new LinkedHashSet<>();
            // add textFiles
            if (textLevel.equals(TextRepresentativeSetting.TextLevel.Page)) {
                docPages.forEach(page -> {
                    String imagePath = Paths.get(page[IMAGE_FILE_PATH_INDEX], page[IMAGE_FILE_NAME_INDEX]).toString();
                    String textFile = textSetting.getTextPathFromImagePath(imagePath); 
                    textFiles.add(textFile);
                });                
            }
            else if (textLevel.equals(TextRepresentativeSetting.TextLevel.Doc)) {
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
}
