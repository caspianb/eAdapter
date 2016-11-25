package parsers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import eAdapter.Document;
import eAdapter.Representative;

public class TextDelimitedParser {

    private final char NULL_CHAR = '\0';
    private final char NEW_LINE = '\n';
    private final String LINE_RETURN = "\r\n";

    public List<Document> parse(Path path, Delimiters delimiters, boolean hasHeader,
            String keyColumnName, String parentColumnName, String childColumnName, char childColumnDelimiter,
            List<Triple<String, String, Representative.Type>> representativeArgs) {
        Map<String, Document> docs = new LinkedHashMap<>();
        // parse the file
        try (Scanner scanner = new Scanner(path)) {
            String pattern = String.valueOf(delimiters.getNewRecord());
            scanner.useDelimiter(pattern);
            String headerLine = "";
            List<String> header = null;
            // get the header line
            if (scanner.hasNext()) {
                headerLine = scanner.next();
            }
            else {
                throw new RuntimeException("The file has no data.");
            }
            // get the header
            header = getHeader(headerLine, hasHeader, delimiters);
            // if no header exists add the parsed line to docs
            if (!hasHeader) {
                Scanner firstLine = new Scanner(headerLine);
                Document firstDoc = parse(firstLine, delimiters, header, keyColumnName, representativeArgs);
                docs.put(header.get(0), firstDoc);
            }
            // parse the file
            while (scanner.hasNext()) {
                Document doc = parse(scanner, delimiters, header, keyColumnName, representativeArgs);
                // get the parent
                if (parentColumnName != null && !StringUtils.isBlank(parentColumnName)) {
                    String parentKey = doc.getMetadata().get(parentColumnName);
                    Document parentDoc = docs.get(parentKey);
                    doc.setParent(parentDoc);
                }
                //else if (child)
                // finish adding parents here
                // add children here too
                docs.put(doc.getKey(), doc);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(docs.values());
    }

    public Document parse(Scanner scanner, Delimiters delimiters, List<String> header, String keyColumnName,
            List<Triple<String, String, Representative.Type>> repArgs) {
        Document document = new Document();
        List<String> values = parseValues(scanner, delimiters);
        // populate metadata
        if (header.size() == values.size()) {
            for (int i = 0; i < values.size(); i++) {
                String fieldName = header.get(i);
                String value = values.get(i);
                document.addField(fieldName, value);
            }
        }
        else {
            throw new RuntimeException("The value size does not match the header size.");
        }
        // populate key
        String keyValue = (keyColumnName != null && !StringUtils.isBlank(keyColumnName))
                ? document.getMetadata().get(keyColumnName)
                : document.getMetadata().get(header.get(0));
        document.setKey(keyValue);
        // populate representatives
        if (repArgs != null) {
            Set<Representative> reps = new LinkedHashSet<>();
            for (Triple<String, String, Representative.Type> repArg : repArgs) {
                Representative rep = new Representative();
                String fileColumnName = repArg.getLeft();
                Set<String> files = new LinkedHashSet<>();
                String file = document.getMetadata().get(fileColumnName);
                files.add(file);
                rep.setType(repArg.getRight());
                rep.setName(repArg.getMiddle());
                rep.setFiles(files);
            }
            document.setRepresentatives(reps);
        }

        return document;
    }

    private List<String> getHeader(String headerLine, boolean hasHeader, Delimiters delimiters) {
        Scanner firstLine = new Scanner(headerLine);
        List<String> header = new ArrayList<String>();
        List<String> values = parseValues(firstLine, delimiters);

        if (hasHeader) {
            header = values;
        }
        else {
            for (int i = 0; i < values.size(); i++) {
                header.add("Column " + i);
            }
        }

        return header;
    }

    private List<String> parseValues(Scanner scanner, Delimiters delimiters) {
        String pattern = Character.toString(delimiters.getNewRecord());
        // check if the pattern is set correctly
        if (!scanner.delimiter().equals(String.valueOf(delimiters.getNewRecord()))) {
            scanner.useDelimiter(pattern);
        }
        List<String> parsedValues = new ArrayList<String>();
        String line = "";
        // get line
        if (scanner.hasNext()) {
            line = scanner.next();
        }

        if (delimiters.getTextQualifier() == NULL_CHAR) {
            // no text qualifier specified, so split on the field separator 
            String fieldSeparator = Character.toString(delimiters.getFieldSeparator());
            parsedValues = Arrays.asList(line.split(fieldSeparator));
        }
        else {
            // scan through the line looking for each field break based on text qualifiers and field separator
            for (int pos = 0; pos < line.length(); pos++) {
                StringBuilder valueBuilder = new StringBuilder(line.length());

                if (line.charAt(pos) == delimiters.getFieldSeparator()) {
                    // empty field, do nothing here
                }
                else if (line.charAt(pos) != delimiters.getTextQualifier()) {
                    //  parse unqualified data
                    pos = parseUnqualifiedData(line, pos, valueBuilder, delimiters);
                }
                else {
                    // parse qualified data
                    pos = parseQualifiedData(line, pos, valueBuilder, delimiters, scanner);
                }

                String value = valueBuilder.toString();

                // if the flattened new line character is not null insert new lines in the field value
                if (delimiters.getFlattenedNewLine() != NULL_CHAR) {
                    value = value.replace(delimiters.getFlattenedNewLine(), NEW_LINE);
                }
                // insert data
                parsedValues.add(value);
            }
        }

        return parsedValues;
    }

    private int parseQualifiedData(String line, int pos, StringBuilder valueBuilder, Delimiters delimiters, Scanner scanner) {
        // qualified field, so skip over the text qualifier character and read all characters up until closing qualifier
        for (pos = pos + 1; pos < line.length(); pos++) {
            // if no ending text qualifier exists read in the next line of data
            if (pos == line.length() - 1 && line.charAt(pos) != delimiters.getTextQualifier()) {
                String endingChar = (delimiters.getNewRecord() == NEW_LINE) ? LINE_RETURN : Character.toString(delimiters.getNewRecord());
                valueBuilder.append(Character.toString(line.charAt(pos)) + endingChar);
                pos = 0;

                if (scanner.hasNext()) {
                    line = scanner.next();
                }
                else {
                    throw new RuntimeException("The line data ended abruptly.");
                }

            }

            // if an escape character is detected check if the next character is a text qualifier
            // if true skip the escape character and add the text qualifier to the field value
            // if false check if the current character is a text qualifier
            // if the current character is a text qualifier stop reading
            // if the current character is not a text qualifier continue reading
            if (pos < line.length() - 1 && line.charAt(pos) == delimiters.getEscapeCharacter() && line.charAt(pos + 1) == delimiters.getTextQualifier()) {
                pos++;
            }
            else if (line.charAt(pos) == delimiters.getTextQualifier()) {
                // consume the ending text qualifier
                pos++;
                break;
            }

            valueBuilder.append(line.charAt(pos));
        }

        // check for a field separator		
        if (line.charAt(pos) != delimiters.getFieldSeparator()) {
            throw new RuntimeException("Trailing orphan detected, invalid format.");
        }

        return pos;
    }

    private int parseUnqualifiedData(String line, int pos, StringBuilder valueBuilder, Delimiters delimiters) {
        // not a qualified field, so read all characters up to the field separator
        for (; pos < line.length(); pos++) {
            if (line.charAt(pos) == delimiters.getFieldSeparator()) {
                break;
            }
            valueBuilder.append(line.charAt(pos));
        }

        if (valueBuilder.toString().contains(String.valueOf(delimiters.getTextQualifier()))) {
            // scans for orphans or qualifiers in unqualified data and re-parses if needed
            String escapeSequence = Character.toString(delimiters.getEscapeCharacter()) + Character.toString(delimiters.getTextQualifier());
            int qualifierCount = StringUtils.countMatches(valueBuilder.toString(), delimiters.getTextQualifier());
            int escapedQualifiers = StringUtils.countMatches(valueBuilder.toString(), escapeSequence);
            qualifierCount -= escapedQualifiers * 2;

            if (qualifierCount - escapedQualifiers >= 1) {
                throw new RuntimeException("An unescaped qualifier was found, format invalid.");
            }
            else {
                // value only contains escaped qualifiers
                // replace the escape sequence with the text qualifier
                String replacedValue = valueBuilder.toString().replace(escapeSequence, Character.toString(delimiters.getTextQualifier()));
                valueBuilder = new StringBuilder(replacedValue);
            }
        }
        return pos;
    }
}
