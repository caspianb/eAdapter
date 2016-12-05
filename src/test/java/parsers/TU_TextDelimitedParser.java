package parsers;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eAdapter.Document;
import eAdapter.Representative;
import parsers.ParserArguments;
import parsers.TextDelimitedParser;

public class TU_TextDelimitedParser {

    private TextDelimitedParser parser;

    @Before
    public void testSetup() {
        parser = new TextDelimitedParser();
    }

    @Test
    public void commaDelimitedTest() {
        Scanner scanner = new Scanner("one,two,buckle,my,shoe");
        Delimiters delimiters = Delimiters.COMMA_DELIMITED;
        String[] header = new String[] { "1", "2", "3", "4", "5" };
        List<String> fields = Arrays.asList(header);
        List<Triple<String, String, Representative.Type>> reps = null;
        Document doc = parser.parse(scanner, delimiters, fields, "1", reps);
        Assert.assertEquals("one", doc.getMetadata().get("1"));
        Assert.assertEquals("two", doc.getMetadata().get("2"));
        Assert.assertEquals("buckle", doc.getMetadata().get("3"));
        Assert.assertEquals("my", doc.getMetadata().get("4"));
        Assert.assertEquals("shoe", doc.getMetadata().get("5"));
    }

}
