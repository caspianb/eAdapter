package importers;

import java.nio.file.Path;
import java.util.List;

import builders.TextDelimitedBuilder;
import builders.UnstructuredRepresentativeSetting;
import csvparser.CSVParser;
import eAdapter.Document;
import parsers.Delimiters;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Imports documents from a text delimited file. 
 */
public class TextDelimitedImporter {
    /**
     * Imports documents from a text delimited file
     * @param filePath path to the text delimited file
     * @param delimiters delimiters that should be used for parsing the text delimited file
     * @param hasHeader indicates if the text delimited file has a header
     * @param keyColumnName the column name of the key field
     * @param parentColumnName the column name of the parent id field
     * @param childColumnName the column name of the field that contains a delimited list of child documents
     * @param childColumnDelimiter the delimited used to parse the child ids
     * @param repSettings a list of representative settings
     * @return returns a list of documents
     */
    public List<Document> importTextDelimited(Path filePath, Delimiters delimiters, boolean hasHeader,
            String keyColumnName, String parentColumnName, String childColumnName, String childColumnDelimiter,
            List<UnstructuredRepresentativeSetting> repSettings) {
        CSVParser parser = new CSVParser();
        List<String[]> parsedData = parser.parse(filePath, delimiters);
        TextDelimitedBuilder builder = new TextDelimitedBuilder();
        return builder.buildDocuments(parsedData, hasHeader, keyColumnName, parentColumnName, childColumnName, childColumnDelimiter, repSettings);
    }

}
