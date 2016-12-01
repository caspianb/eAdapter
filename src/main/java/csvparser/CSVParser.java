package csvparser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import parsers.CharsetDetector;
import parsers.Delimiters;

public class CSVParser {

    public List<String[]> parse(Path path, Delimiters delimiters) {
        Charset charset = CharsetDetector.detect(path);

        List<String[]> records = new ArrayList<>();

        try (Scanner scanner = new Scanner(path, charset.name())) {
            scanner.useDelimiter(String.valueOf(delimiters.getNewRecord()));

            while (scanner.hasNext()) {
                String line = StringUtils.stripToEmpty(scanner.next());

                if (StringUtils.isNotBlank(line)) {
                    String[] fieldValues = parseLine(line, delimiters);
                    records.add(fieldValues);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return records;
    }

    protected String[] parseLine(String line, Delimiters delimiters) {
        List<String> fieldValues = new ArrayList<>();
        MutableInt startIndex = new MutableInt(0);

        while (startIndex.intValue() < line.length()) {
            String fieldValue = parseField(line, startIndex, delimiters);
            fieldValues.add(fieldValue);
        }

        return fieldValues.toArray(new String[fieldValues.size()]);
    }

    protected String parseField(String line, MutableInt startIndex, Delimiters delimiters) {
        StringBuilder fieldValue = new StringBuilder();
        int currentIndex = startIndex.intValue();
        char currentChar = line.charAt(currentIndex);

        // if this is an emtpy field... simply return empty value
        if (currentChar == delimiters.getFieldSeparator()) {
            currentIndex++;
        }

        // if this is NOT a qualified field, then simply scan through to next field separator
        else if (currentChar != delimiters.getTextQualifier()) {
            int endIndex = line.indexOf(delimiters.getFieldSeparator(), currentIndex);
            endIndex = (endIndex < 0) ? line.length() : endIndex;
            fieldValue.append(line.substring(currentIndex, endIndex));
            currentIndex = endIndex;
        }

        // otherwise, this field must be text qualified... so we're going to have to parse through the possibilities here
        else {
            for (currentIndex = currentIndex + 1; currentIndex < line.length(); currentIndex++) {
                currentChar = line.charAt(currentIndex);
                Character nextChar = (currentIndex + 1 < line.length()) ? line.charAt(currentIndex + 1) : null;

                // So we are a qualified field... we have several possibilities:
                // 1. The character is a properly escaped text qualifier, so we append it as a single character
                //    a. If text qualifier is not escaped, but it doesn't precede a field separator then this
                //       this is an error case, but we're going to be nice and treat it as escaped
                // 3. The character is a text qualifier followed by a field separator, so we're done
                //    b. If the character is a text qualifier at the very end of the line, we're also done
                // 4. None of the above - so we append it to the fieldValue

                // current character is escape character for a text qualifier, append text qualifier as part of field value
                if (currentChar == delimiters.getEscapeCharacter() && nextChar != null && nextChar == delimiters.getTextQualifier()) {
                    fieldValue.append(nextChar);
                    currentIndex++;
                    continue;
                }

                // if current character is text qualifier, next character is the field separator or doesn't exist then we're done with this field
                else if (currentChar == delimiters.getTextQualifier() && (nextChar == null || nextChar == delimiters.getFieldSeparator())) {
                    currentIndex++;
                    break;
                }

                // We might be an unqualified text qualifier, an escape character, or anything else... simply append it to field value
                else {
                    fieldValue.append(currentChar);
                }
            }
        }

        // Update start position to one character after the last one we looked at...
        startIndex.setValue(currentIndex + 1);
        return fieldValue.toString();
    }
}
