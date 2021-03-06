package builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import eAdapter.Document;
import eAdapter.Representative;

/**
 * 
 * @author Jeff Gillispie
 * @version December 2016
 *
 * Purpose: Builds documents from a text delimited file.
 */
public class TextDelimitedBuilder {
    /**
     * Builds a list of documents
     * @param lines the lines parsed from a text delimited file
     * @param hasHeader indicates if the first line is a header
     *        if there is no header the arbitrary column names will be assigned
     *        in the format "Column 1, Column 2, ..."
     * @param keyColumnName the name of the column that contains the key
     *        if no header exists the key must be in the first column
     * @param parentColumnName the name of the column that contains the parent key or blank if none
     * @param childColumnName the name of the column that contains the child key or blank if none
     * @param childColumnDelimiter the delimiter used to split child key values
     * @param repSettings representative settings
     * @return returns a list of documents
     */
    public List<Document> buildDocuments(List<String[]> lines, boolean hasHeader,
            String keyColumnName, String parentColumnName, String childColumnName, String childColumnDelimiter,
            List<UnstructuredRepresentativeSetting> repSettings) {
        // setup for building
        String[] header = getHeader(lines.get(0), hasHeader);
        Map<String, Document> docs = new LinkedHashMap<>();
        Map<String, Document> paternity = new HashMap<>(); // childKey >> parentDoc        
        String childSeparator = StringUtils.defaultIfBlank(childColumnDelimiter, ";");
        // build the documents
        for (String[] line : lines) {
            if (hasHeader && lines.indexOf(line) == 0) {
                continue; // skip header line
            }
            // build a document
            Document doc = buildDocument(line, header, keyColumnName, repSettings);
            // set the parent and child values
            settleFamilyDrama(parentColumnName, childColumnName, childSeparator, doc, docs, paternity);
            // add the document to the collection
            docs.put(doc.getKey(), doc);
        }
        // check for children that have disowned their parent
        // this can only be known after all children have been imported
        if (paternity.size() > 0) {
            throw new RuntimeException("Broken families, children have disowned their parent.");
        }
        return new ArrayList<>(docs.values());
    }

    /**
     * Builds a document
     * @param line a text delimited line representing a document
     * @param header the header which contains the ordered field names
     * @param keyColumnName the name of the key column
     * @param representativeSettings representative settings
     * @return returns a document
     */
    public Document buildDocument(String[] line, String[] header, String keyColumnName, List<UnstructuredRepresentativeSetting> representativeSettings) {
        // setup for building
        Document document = new Document();
        // check value size matches the header size
        if (header.length != line.length) {
            throw new RuntimeException("The value size does not match the header size.");
        }        
        // populate metadata
        for (int i = 0; i < line.length; i++) {
            String fieldName = header[i];
            String value = line[i];
            document.addField(fieldName, value);
        }        
        // populate key, if there is no key column name the value in the first column is expected to be the key
        String keyValue = (!StringUtils.isBlank(keyColumnName))
                ? document.getMetadata().get(keyColumnName)
                : document.getMetadata().get(header[0]);
        document.setKey(keyValue);
        // populate representatives
        if (representativeSettings != null) {
            // setup for populating representatives
            Set<Representative> reps = new LinkedHashSet<>();
            for (UnstructuredRepresentativeSetting setting : representativeSettings) {
                // setup for creating rep properties
                Representative rep = new Representative();
                Set<String> files = new LinkedHashSet<>();
                // this format will only have one file per rep
                String file = document.getMetadata().get(setting.getColumn());
                files.add(file);
                // set rep values
                rep.setType(setting.getType());
                rep.setName(setting.getName());
                rep.setFiles(files);
                reps.add(rep); // add rep to the collection
            }
            document.setRepresentatives(reps);
        }
        // return built document
        return document;
    }

    private void settleFamilyDrama(String parentColumnName, String childColumnName, String childSeparator,
            Document doc, Map<String, Document> docs, Map<String, Document> paternity) {
        if (StringUtils.isNotBlank(parentColumnName)) {
            // if we have a parent column name
            String parentKey = doc.getMetadata().get(parentColumnName);
            // check that the parentKey doesn't refer to itself
            if (StringUtils.isBlank(parentKey) || parentKey.equals(doc.getKey())) {
                // the parentid value refers to itself or there is no parent
                // do nothing here
            }
            else {
                Document parent = docs.get(parentKey);
                // check if there is no parent
                if (parent == null) {
                    throw new RuntimeException("Broken families, the parent is missing.");
                }
                else {
                    // a parent exists
                    setRelationships(doc, parent);
                    // validate relationships if both parent & child fields exists
                    if (StringUtils.isNotBlank(childColumnName)) {
                        // log paternity so we can check for children who disown their parent
                        String childrenLine = doc.getMetadata().get(childColumnName);
                        if (StringUtils.isNotBlank(childrenLine)) {
                            String[] childKeys = childrenLine.split(childSeparator);
                            // the child docs haven't been added yet so we'll record the relationships and add them later
                            for (String childKey : childKeys) {
                                paternity.put(childKey, doc); // paternity maps childKey >> parentDoc
                            }
                        }
                        // check for relationships that are not reciprocal
                        if (!parent.getMetadata().get(childColumnName).contains(parentKey)) {
                            throw new RuntimeException("Broken families, the parent disowns a child document.");                            
                        }
                        else {
                            // the relationship is reciprocal
                            // we'll check for orphans later
                            paternity.remove(doc.getKey());
                        }
                    }                    
                }
            }
        }
        else if (StringUtils.isNotBlank(childColumnName)) {
            // if we don't have a parent column name but we have a child column name
            String childrenLine = doc.getMetadata().get(childColumnName);
            if (StringUtils.isBlank(childrenLine)) {
                // no childrenLine
                // do nothing here
            }
            else {
                String[] childKeys = childrenLine.split(childSeparator);
                // the child docs haven't been added yet so we'll record the relationship and add them later
                for (String childKey : childKeys) {
                    paternity.put(childKey, doc); // paternity maps childKey >> parentDoc
                }
                // now check for the paternity of this document and add the parent
                // paternity maps childKey >> parentDoc
                if (paternity.containsKey(doc.getKey())) {
                    Document parent = paternity.get(doc.getKey()); // note: the parent doc has already been confirmed
                    setRelationships(doc, parent);
                    paternity.remove(doc.getKey()); // needs to be removed for the disowned parent check
                }
            }
        }
        else {
            // no family data
            // do nothing here
        }
    }

    private void setRelationships(Document doc, Document parent) {
        doc.setParent(parent);
        // now add this document as a child to the parent
        List<Document> children = parent.getChildren();
        children.add(doc);
        parent.setChildren(children);
    }

    /**
     * Gets the header values.
     * @param headerValues the ordered column names or a line from the text delimited file
     * @param hasHeader indicates if the headerValues are the header
     *        if they are not the header the column names will be given an arbitrary name
     *        in the following format "Column 1, Column 2, Column 3, ..."
     * @return returns the ordered column names
     */
    public String[] getHeader(String[] headerValues, boolean hasHeader) {
        String[] header = new String[headerValues.length];
        // check if the supplied values are the header
        if (hasHeader) {
            header = headerValues;
        }
        else {
            // create arbitrary column names
            for (int i = 0; i < headerValues.length; i++) {                
                header[i] = "Column " + i;
            }
        }
        return header;
    }
}
