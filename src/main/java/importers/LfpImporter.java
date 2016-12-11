package importers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import builders.LfpBuilder;
import builders.StructuredRepresentativeSetting;
import eAdapter.Document;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Imports documents from a LFP file
 */
public class LfpImporter {
    @Autowired
    protected LfpBuilder builder;

    /**
     * Imports a LFP file using the default representative names with no text representative
     * @param filePath the path to the LFP file
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return builder.buildDocuments(lines);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Imports a LPF file using the default representative names
     * @param filePath the path to the LFP file
     * @param textSetting the text representative settings
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath, StructuredRepresentativeSetting textSetting) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return builder.buildDocuments(lines, textSetting);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Imports a LFP file
     * @param filePath the path to the LFP file
     * @param imagesName the name of the images document representative
     * @param nativeName the name of the native document representative
     * @param textName the name of the document text representative
     * @param textSetting the text representative settings
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath, String imagesName, String nativeName, String textName, StructuredRepresentativeSetting textSetting) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return builder.buildDocuments(lines, imagesName, nativeName, textName, textSetting);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
