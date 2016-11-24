package eAdapter;

import java.io.BufferedReader;

public class ParserArguments {
	private final Delimiters delimiters;
	private final BufferedReader reader;
	private final String[] header;
	
	private ParserArguments(BufferedReader reader, Delimiters delimiters, String[] header) {
		this.reader = reader;
		this.delimiters = delimiters;
		this.header = header;		
	}
	
	public static ParserArguments of(BufferedReader reader, Delimiters delimiters, String[] header) {
		return new ParserArguments(reader, delimiters, header);
	}
	
	
	public Delimiters getDelimiters() { return this.delimiters; }
	public BufferedReader getReader() { return this.reader; }
	public String[] getHeader() { return this.header; }
}
