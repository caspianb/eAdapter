package builders;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import csvparser.CSVParser;
import eAdapter.Document;
import parsers.Delimiters;

public class TU_TextDelimitedBuilder {
    
    private CSVParser parser;
    private TextDelimitedBuilder builder;
    private Delimiters delimiters;
    
    @Before
    public void testSetup() {
        parser = new CSVParser();
        builder = new TextDelimitedBuilder();
        delimiters = Delimiters.CONCORDANCE;
    }

    @Test
    public void test() {
        Path path = Paths.get("X:\\dev\\java\\eAdapter\\src\\test\\java\\builders\\sample.dat"); 
        List<String[]> parsedData = parser.parse(path, delimiters);
        List<Document> docs = builder.buildDocuments(parsedData, true, "BegDoc", "BegAttach", null, ";", null);
        Assert.assertEquals(282,docs.size());                
    }

}
