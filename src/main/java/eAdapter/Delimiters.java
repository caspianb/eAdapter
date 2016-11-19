package eAdapter;

public final class Delimiters {

    private final char fieldSeparator;
    private final char textQualifier;
    private final char newRecord;
    private final char escapeCharacter;
    private final char flattenedNewLine;

    public static final Delimiters COMMA_QUOTE = of(',', '"', '\n', '"', '\0');
    public static final Delimiters COMMA_DELIMITED = of(',', '\0', '\n', '\0', '\0');
    public static final Delimiters TAB_DELIMITED = of('\t', '\0', '\n', '\0', '\0');
    public static final Delimiters PIPE_CARET = of('|', '^', '\n', '^', '\0');
    public static final Delimiters CONCORDANCE = of((char) 20, (char) 254, '\n', (char) 254, (char) 174);

    public static Delimiters of(char fieldSeparator, char textQualifier, char newRecord, char escapeCharacter, char flattenedNewLine) {
        return new Delimiters(fieldSeparator, textQualifier, newRecord, escapeCharacter, flattenedNewLine);
    }

    public static Delimiters of(char fieldSeparator, char textQualifier, char newRecord, char escapeCharacter) {
        return of(fieldSeparator, textQualifier, newRecord, escapeCharacter, '\0');
    }

    public static Delimiters of(char fieldSeparator, char textQualifier, char newRecord) {
        return of(fieldSeparator, textQualifier, newRecord, textQualifier);
    }

    public static Delimiters of(char fieldSeparator, char textQualifier) {
        return of(fieldSeparator, textQualifier, '\n');
    }

    public static Delimiters of(char fieldSeparator) {
        return of(fieldSeparator, '\0');
    }

    private Delimiters(char fieldSeparator, char textQualifier, char newRecord, char escapeCharacter, char flattenedNewLine) {
        this.fieldSeparator = fieldSeparator;
        this.textQualifier = textQualifier;
        this.newRecord = newRecord;
        this.escapeCharacter = escapeCharacter;
        this.flattenedNewLine = flattenedNewLine;
        checkForExceptions();
    }

    public char getFieldSeparator() {
        return this.fieldSeparator;
    }

    public char getTextQualifier() {
        return this.textQualifier;
    }

    public char getNewRecord() {
        return this.newRecord;
    }

    public char getEscapeCharacter() {
        return this.escapeCharacter;
    }

    public char getFlattenedNewLine() {
        return this.flattenedNewLine;
    }

    private void checkForExceptions() {
        if (this.fieldSeparator == this.textQualifier)
            throw new RuntimeException("The field separator and text qualifier delimiter can not have the same value.");
        else if (this.fieldSeparator == this.newRecord)
            throw new RuntimeException("The field separator and the new record delimiter can not have the same value.");
        else if (this.textQualifier == this.newRecord)
            throw new RuntimeException("The text qualifier and the new record delimiter can not have the same value.");
        else if (this.escapeCharacter == this.fieldSeparator)
            throw new RuntimeException("The escape character and the field separator delimiter can not have the same value.");
        else if (this.escapeCharacter == this.newRecord)
            throw new RuntimeException("The escape character and the new record delimiter can not have the same value.");
    }
}
