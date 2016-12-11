package importers;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import builders.OpticonBuilder;
import builders.StructuredRepresentativeSetting;
import csvparser.CSVParser;
import eAdapter.Document;
import parsers.Delimiters;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Imports documents from an opticon file.
 */
public class OpticonImporter {
    @Autowired
    protected CSVParser parser;
    @Autowired
    protected OpticonBuilder builder;

    /**
     * Imports an Opticon file
     * @param filePath the file path to the opticon file
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath) {
        Delimiters delimiters = Delimiters.COMMA_DELIMITED;
        List<String[]> parsedData = parser.parse(filePath, delimiters);
        return builder.buildDocuments(parsedData);
    }

    /**
     * Imports an Opticon file
     * @param filePath the file path to the opticon file
     * @param imagesName the text representative settings
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath, String imagesName) {
        Delimiters delimiters = Delimiters.COMMA_DELIMITED;
        List<String[]> parsedData = parser.parse(filePath, delimiters);
        return builder.buildDocuments(parsedData, imagesName);
    }

    /**
     * Imports an Opticon file
     * @param filePath the file path to the opticon file
     * @param textSetting the text representative settings
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath, StructuredRepresentativeSetting textSetting) {
        Delimiters delimiters = Delimiters.COMMA_DELIMITED;
        List<String[]> parsedData = parser.parse(filePath, delimiters);
        return builder.buildDocuments(parsedData, textSetting);
    }

    /**
     * Imports an Opticon file
     * @param filePath the file path to the opticon file
     * @param imagesName the name of the images representative
     * @param textName the name of the text representative
     * @param textSetting the text representative settings
     * @return returns a list of documents
     */
    public List<Document> importDocuments(Path filePath, String imagesName, String textName, StructuredRepresentativeSetting textSetting) {
        Delimiters delimiters = Delimiters.COMMA_DELIMITED;
        List<String[]> parsedData = parser.parse(filePath, delimiters);
        return builder.buildDocuments(parsedData, imagesName, textName, textSetting);
    }

}
