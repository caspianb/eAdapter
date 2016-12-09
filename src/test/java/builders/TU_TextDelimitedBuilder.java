package builders;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eAdapter.Document;
import eAdapter.Representative;

public class TU_TextDelimitedBuilder {
        
    private TextDelimitedBuilder builder;
    
    @Before
    public void testSetup() {
        builder = new TextDelimitedBuilder();
    }

    @Test
    public void test() {
        boolean HAS_HEADER = true;
        String KEY_COL_NAME = "DocID";
        String PARENT_COL_NAME = "ParentID";
        String CHILD_COL_NAME = null;
        String CHILD_COL_DELIM = ";";        
        String NATIVE_REP_COL = "Native Link";
        String TEXT_REP_COL = "Extracted Text";
        String HASH_FIELD = "Hash";
        
        List<String[]> parsedData = new ArrayList<>();
        parsedData.add(new String[] { "DocID", "ParentID", "Hash", "Extracted Text", "Native Link" });
        parsedData.add(new String[] { "D001", "D001", "", "", "" });
        parsedData.add(new String[] { "D002", "D001", "", "", "V001\\NATIVE\\0001\\D001.msg" });
        parsedData.add(new String[] { "D003", "D001", "", "", "" });
        parsedData.add(new String[] { "D004", "D004", "", "V001\\TEXT\\0001\\D004.txt", "" });
        parsedData.add(new String[] { "D005", "", "3E2F1DB06FB1DD42C421ECA8CC0C330D", "", "" });
        
        List<UnstructuredRepresentativeSetting> reps = new ArrayList<>();
        UnstructuredRepresentativeSetting nativeRep = new UnstructuredRepresentativeSetting();
        nativeRep.setColumn(NATIVE_REP_COL);
        nativeRep.setType(Representative.Type.NATIVE);
        UnstructuredRepresentativeSetting textRep = new UnstructuredRepresentativeSetting();
        textRep.setColumn(TEXT_REP_COL);
        textRep.setType(Representative.Type.TEXT);
        reps.add(nativeRep);
        reps.add(textRep);
        List<Document> docs = builder.buildDocuments(parsedData, HAS_HEADER, KEY_COL_NAME, PARENT_COL_NAME, CHILD_COL_NAME, CHILD_COL_DELIM, reps);
        assertEquals(5,docs.size()); 
        assertEquals(parsedData.get(1)[0], docs.get(0).getMetadata().get(KEY_COL_NAME));
        assertEquals(parsedData.get(5)[2], docs.get(4).getMetadata().get(HASH_FIELD));
        assertEquals(docs.get(0), docs.get(2).getParent());
        assertTrue(docs.get(0).getChildren().contains(docs.get(2))); 
        String nativeFile = "";
        String textFile = "";
        
        for (Representative rep : docs.get(1).getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.NATIVE)) {
                for (String file : rep.getFiles()) {
                    nativeFile = file;
                }
            }
        }
        
        for (Representative rep : docs.get(3).getRepresentatives()) {
            if (rep.getType().equals(Representative.Type.TEXT)) {
                for (String file : rep.getFiles()) {
                    textFile = file;
                }
            }
        }
        
        assertEquals(parsedData.get(2)[4], nativeFile);
        assertEquals(parsedData.get(4)[3], textFile);
    }

}
