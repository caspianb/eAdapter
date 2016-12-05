package builders;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 *
 */
public class TextRepresentativeSetting {
    
    private final String TEXT_EXT = ".txt";
    
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
    
    private TextLevel textLevel = TextLevel.None;
    private TextLocation textLocation = TextLocation.None;
    private Pair<Pattern, String> textPathFindReplace = null;

    public TextLevel getTextLevel() {
        return this.textLevel;
    }
    
    public TextLocation getTextLocation() {
        return this.textLocation;
    }
    
    public Pair<Pattern, String> getTextPathFindReplace() {
        return this.textPathFindReplace;
    }
    
    /**
     * 
     * @param textLevel the level of the text representative
     */
    public void setTextLevel(TextLevel textLevel) {
        this.textLevel = textLevel;
    }
    
    /**
     * 
     * @param textLocation the location of the text representative
     */
    public void setTextLocation(TextLocation textLocation) {
        this.textLocation = textLocation;
    }
    
    /**
     * 
     * @param textPathFindReplace a pair containing a pattern to find all image path specific 
     *        elements in the image path and the string to replace them with. The result of this 
     *        operation should be the transformation of the image path into the text path.
     *        The file extension of the image file name will automatically be updated to '.txt'.
     *        The base name of the image file and text file are expected to be identical.
     */
    public void setTextPathFindReplace(Pair<Pattern, String> textPathFindReplace) {
        this.textPathFindReplace = textPathFindReplace;
    }
    
    public String getTextPathFromImagePath(String imagePath) {        
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
