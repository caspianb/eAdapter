package builders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import eAdapter.Document;
import eAdapter.Representative;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Builds documents from opticon image records 
 */
public class OpticonBuilder {
    private final int IMAGE_KEY_INDEX = 0;
    private final int VOLUME_NAME_INDEX = 1;
    private final int FULL_PATH_INDEX = 2;
    private final int DOC_BREAK_INDEX = 3;
    private final int BOX_BREAK_INDEX = 4;
    private final int FOLDER_BREAK_INDEX = 5;
    @SuppressWarnings("unused")
    private final int PAGE_COUNT_INDEX = 6;
    private final String TRUE_VALUE = "Y";
    private final String IMAGE_KEY_FIELD = "DocID";
    private final String VOLUME_NAME_FIELD = "Volume Name";
    private final String PAGE_COUNT_FIELD = "Page Count";
    private final String BOX_BREAK_FIELD = "Box Break";
    private final String FOLDER_BREAK_FIELD = "Folder Break";
    private final String DEFAULT_IMAGE_REP_NAME = "default";
    private final String DEFAULT_TEXT_REP_NAME = "default";
        
    
    /**
     * Builds a list of documents from an opticon file with no text representatives and uses the default image representative name
     * @param lines the lines read from an opt file split on a comma
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines) {
        TextRepresentativeSetting textSetting = new TextRepresentativeSetting();
        textSetting.setTextLevel(TextRepresentativeSetting.TextLevel.None);
        textSetting.setTextLocation(TextRepresentativeSetting.TextLocation.None);
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a list of documents from an opticon file with no text representatives.
     * @param lines the lines read from an opt file split on a comma
     * @param imagesName the name of the image representative
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, String imagesName) {
        TextRepresentativeSetting textSetting = new TextRepresentativeSetting();
        textSetting.setTextLevel(TextRepresentativeSetting.TextLevel.None);
        textSetting.setTextLocation(TextRepresentativeSetting.TextLocation.None);
        return buildDocuments(lines, imagesName, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a list of documents from an opticon file using the default image and text representative names
     * @param lines the lines read from an opt file split on a comma
     * @param textSetting the setting used to construct the text representative
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, TextRepresentativeSetting textSetting) {        
        return buildDocuments(lines, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textSetting);        
    } 
    
    /**
     * Builds a list of documents from an opticon file
     * @param lines the lines read from an opt file split on a comma
     * @param imagesName the name of images representative
     * @param textName the name of the text representative
     * @param textSetting the setting used to construct the text representative
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, String imagesName, String textName, TextRepresentativeSetting textSetting) {
        // setup for building
        Map<String, Document> docs = new LinkedHashMap<>();
        List<String[]> docPages = new ArrayList<>();
        // build the documents
        for (String[] line : lines) {
            if (line[DOC_BREAK_INDEX].toUpperCase().equals(TRUE_VALUE)) {
                // send data to make a document
                if (docPages.size() > 0) {
                    Document doc = buildDocument(docPages, imagesName, textName, textSetting);
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
        Document doc = buildDocument(docPages, imagesName, textName, textSetting);
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
        TextRepresentativeSetting textSetting = new TextRepresentativeSetting();
        textSetting.setTextLevel(TextRepresentativeSetting.TextLevel.None);
        textSetting.setTextLocation(TextRepresentativeSetting.TextLocation.None);
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a single document, which has no text representative
     * @param docPages a list of opticon page records split on a comma
     * @param imagesName the name of the image representative
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName) {
        TextRepresentativeSetting textSetting = new TextRepresentativeSetting();
        textSetting.setTextLevel(TextRepresentativeSetting.TextLevel.None);
        textSetting.setTextLocation(TextRepresentativeSetting.TextLocation.None);
        return buildDocument(docPages, imagesName, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a single document using the default image and text representative names
     * @param docPages a list of opticon page records split on a comma
     * @param textSetting the setting used to construct the text representative
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, TextRepresentativeSetting textSetting) {
        return buildDocument(docPages, DEFAULT_IMAGE_REP_NAME, DEFAULT_TEXT_REP_NAME, textSetting);
    }
    
    /**
     * Builds a single document
     * @param docPages a list of opticon page records split on a comma
     * @param imagesName the name of the images representative
     * @param textName the name of the text representative
     * @param textSetting the setting used to construct the text representative
     * @return returns a single document
     */
    public Document buildDocument(List<String[]> docPages, String imagesName, String textName, TextRepresentativeSetting textSetting) {
        // setup for building
        Document doc = new Document();
        TextRepresentativeSetting.TextLevel textLevel = textSetting.getTextLevel();        
        // get document properties
        String[] pageOne = docPages.get(0);
        String key = pageOne[IMAGE_KEY_INDEX];
        String vol = pageOne[VOLUME_NAME_INDEX];
        String box = pageOne[BOX_BREAK_INDEX];
        String dir = pageOne[FOLDER_BREAK_INDEX];
        int pages = docPages.size(); // do we need to check the field value here?
        // set document properties
        doc.setKey(key);
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
        Set<String> textFiles = new LinkedHashSet<>();
        // add textFiles
        switch (textLevel) {
            case None:
                // do nothing here
                break;
            case Page:
                docPages.forEach(page -> {
                    String textFile = textSetting.getTextPathFromImagePath(page[FULL_PATH_INDEX]);
                    textFiles.add(textFile);
                });
                break;
            case Doc:
                String[] firstPageInfo = docPages.get(0);
                String textFile = textSetting.getTextPathFromImagePath(firstPageInfo[FULL_PATH_INDEX]);
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
}
