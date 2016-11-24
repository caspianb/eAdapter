package eAdapter;

import java.util.Scanner;

public class ParserArguments {
    private final Delimiters delimiters;
    private final Scanner scanner;
    private final String[] header;

    private ParserArguments(Scanner scanner, Delimiters delimiters, String[] header) {
        this.scanner = scanner;
        this.delimiters = delimiters;
        this.header = header;
    }

    public static ParserArguments of(Scanner scanner, Delimiters delimiters, String[] header) {
        return new ParserArguments(scanner, delimiters, header);
    }

    public Delimiters getDelimiters() {
        return this.delimiters;
    }

    public Scanner getScanner() {
        return this.scanner;
    }

    public String[] getHeader() {
        return this.header;
    }
}
