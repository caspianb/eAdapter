package exporters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import eAdapter.Document;
import parsers.Delimiters;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 * 
 * Purpose: Exports documents to a text delimited file.
 */
public class TextDelimitedExporter {
    /**
     * Export a list of documents to a text delimited file
     * @param documents the documents to be exported
     * @param filePath the path to save the file
     * @param delimiters the delimiters to use in the export
     * @param exportFields the fields to include in the order they should be exported
     */
    public void export(List<Document> documents, Path filePath, Delimiters delimiters, List<String> exportFields) {
        // TODO: add support to export relationships and representatives

        BufferedWriter writer = null;

        try {
            // init writer
            writer = new BufferedWriter(new FileWriter(filePath.toString()));
            // get field delimiter
            String fieldDelimiter = new StringBuilder()
                    .append(delimiters.getTextQualifier())
                    .append(delimiters.getFieldSeparator())
                    .append(delimiters.getTextQualifier())
                    .toString();
            // get header
            String header = new StringBuilder()
                    .append(delimiters.getTextQualifier())
                    .append(String.join(fieldDelimiter, exportFields))
                    .append(delimiters.getTextQualifier())
                    .toString();
            // write header
            writer.write(header);
            writer.write(delimiters.getNewRecord());
            // write documents
            for (Document doc : documents) {
                String line = getLine(doc, exportFields, delimiters);
                writer.write(line);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            // close the file
            try {
                writer.close();
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String getLine(Document doc, List<String> exportFields, Delimiters delimiters) {
        // setup for building the line
        List<String> lineElements = new ArrayList<>();
        String fieldSeparator = String.valueOf(delimiters.getFieldSeparator());
        String textQualifier = String.valueOf(delimiters.getTextQualifier());
        String escapeCharacter = String.valueOf(delimiters.getEscapeCharacter());
        String escapeSequence = escapeCharacter + textQualifier;
        String newRecord = String.valueOf(delimiters.getNewRecord());
        // assemble the line elements
        for (String field : exportFields) {
            String fieldValue = doc.getMetadata().get(field);
            // check for null
            if (fieldValue == null) {
                // return an empty field
                fieldValue = new StringBuilder()
                        .append(delimiters.getTextQualifier())
                        .append(delimiters.getTextQualifier())
                        .toString();
            }
            else {
                // check if anything needs to be escaped
                if (fieldValue.contains(textQualifier)) {
                    fieldValue.replace(textQualifier, escapeSequence);
                }
                // encapsulate the field value with text qualifiers
                fieldValue = new StringBuilder()
                        .append(delimiters.getTextQualifier())
                        .append(fieldValue)
                        .append(delimiters.getTextQualifier())
                        .toString();
            }
            // add the field value to line elements
            lineElements.add(fieldValue);
        }
        // join the line elements
        String line = String.join(fieldSeparator, lineElements) + newRecord;
        // return line
        return line;
    }

}
