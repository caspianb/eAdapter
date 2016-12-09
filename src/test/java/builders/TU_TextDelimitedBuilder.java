package builders;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import csvparser.CSVParser;
import eAdapter.Document;
import eAdapter.Representative;
import parsers.Delimiters;

public class TU_TextDelimitedBuilder {
    
    private final String IN_FILE = "X:\\dev\\java\\eAdapter\\src\\test\\java\\builders\\sample.dat";
    private final String KEY_COL_NAME = "BegDoc";
    private final String PARENT_COL_NAME = "BegDocAttach";
    private final String CHILD_COL_NAME = null;
    private final String CHILD_COL_DELIM = ";";
    private final boolean HAS_HEADER = true;
    private final String NATIVE_REP_COL = "Native Link";
    private final String TEXT_REP_COL = "Extracted Text";
    private final String HASH_FIELD = "MD5 Hash";
    private final String EXPECTED_HASH_VALUE = "3E2F1DB06FB1DD42C421ECA8CC0C330D";
    private final int EXPECTED_HASH_INDEX = 281;
    private final String EXPECTED_KEY_VALUE = "RS00150";
    private final int EXPECTED_KEY_INDEX = 72;
    private final String EXPECTED_NATIVE_REP = "RS001\\NATIVE\\0001\\RS00185.msg";
    private final int EXPECTED_NATIVE_INDEX = 96;
    private final String EXPECTED_TEXT_REP = "RS001\\TEXT\\0001\\RS00195.txt";
    private final int EXPECTED_TEXT_INDEX = 100;
    private final int EXPECTED_PARENT_INDEX = 16;
    private final int EXPECTED_CHILD_INDEX = 20;
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
        Path path = Paths.get(IN_FILE); 
        List<String[]> parsedData = parser.parse(path, delimiters);
        List<RepresentativeSetting> reps = new ArrayList<>();
        RepresentativeSetting nativeRep = new RepresentativeSetting();
        nativeRep.setColumn(NATIVE_REP_COL);
        nativeRep.setType(Representative.Type.NATIVE);
        RepresentativeSetting textRep = new RepresentativeSetting();
        textRep.setColumn(TEXT_REP_COL);
        textRep.setType(Representative.Type.TEXT);
        reps.add(nativeRep);
        reps.add(textRep);
        List<Document> docs = builder.buildDocuments(parsedData, HAS_HEADER, KEY_COL_NAME, PARENT_COL_NAME, CHILD_COL_NAME, CHILD_COL_DELIM, reps);
        assertEquals(282,docs.size()); 
        assertEquals(EXPECTED_KEY_VALUE, docs.get(EXPECTED_KEY_INDEX).getMetadata().get(KEY_COL_NAME));
        assertEquals(EXPECTED_HASH_VALUE, docs.get(EXPECTED_HASH_INDEX).getMetadata().get(HASH_FIELD));
        assertEquals(docs.get(EXPECTED_PARENT_INDEX), docs.get(EXPECTED_CHILD_INDEX).getParent());
        assertTrue(docs.get(EXPECTED_PARENT_INDEX).getChildren().contains(docs.get(EXPECTED_CHILD_INDEX))); 
        String nativeFile = "";
        String textFile = "";
        
        for (Representative rep : docs.get(EXPECTED_NATIVE_INDEX).getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.NATIVE)) {
                for (String file : rep.getFiles()) {
                    nativeFile = file;
                }
            }
        }
        
        for (Representative rep : docs.get(EXPECTED_TEXT_INDEX).getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.TEXT)) {
                for (String file : rep.getFiles()) {
                    textFile = file;
                }
            }
        }
        
        assertEquals(EXPECTED_NATIVE_REP, nativeFile);
        assertEquals(EXPECTED_TEXT_REP, textFile);
    }

}
