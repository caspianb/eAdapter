package parsers;

import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import eAdapter.Document;

public class TextDelimitedParser implements Parser {

    private final char NULL_CHAR = '\0';
    private final char NEW_LINE = '\n';
    private final String LINE_RETURN = "\r\n";

    @Override
    public Document parseDocument(ParserArguments parserArgs) {
        Scanner scanner = parserArgs.getScanner();
        Delimiters delimiters = parserArgs.getDelimiters();
        String[] header = parserArgs.getHeader();
        String pattern = Character.toString(delimiters.getNewRecord());
        scanner.useDelimiter(pattern);
        Document document = new Document();
        String line = "";
        // get line
        if (scanner.hasNext()) {
            line = scanner.next();
        }

        if (delimiters.getTextQualifier() == NULL_CHAR) {
            // no text qualifier specified, so split on the field separator	
            String fieldSeparator = Character.toString(delimiters.getFieldSeparator());
            String[] values = line.split(fieldSeparator);
            // populate fields
            for (int i = 0; i < values.length; i++) {
                String fieldName = header[i];
                String value = values[i];
                document.addField(fieldName, value);
            }
        }
        else {
            int colIndex = 0;
            // scan through the line looking for each field break based on text qualifiers and field separator
            for (int pos = 0; pos < line.length(); pos++) {
                StringBuilder valueBuilder = new StringBuilder(line.length());

                if (line.charAt(pos) == delimiters.getFieldSeparator()) {
                    // empty field, do nothing here
                }
                else if (line.charAt(pos) != delimiters.getTextQualifier()) {
                    //  parse unqualified data
                    pos = parseUnqualifiedData(line, pos, valueBuilder, parserArgs);
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
                String fieldName = header[colIndex];
                document.addField(fieldName, value);
                colIndex++;
            }
        }

        return document;
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

    private int parseUnqualifiedData(String line, int pos, StringBuilder valueBuilder, ParserArguments parserArgs) {
        Delimiters delimiters = parserArgs.getDelimiters();
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
