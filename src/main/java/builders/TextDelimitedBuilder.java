package builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import eAdapter.Document;
import eAdapter.Representative;

public class TextDelimitedBuilder {
    public List<Document> build(List<String[]> lines, boolean hasHeader,
            String keyColumnName, String parentColumnName, String childColumnName, String childColumnDelimiter,
            List<RepresentativeSetting> repSettings) {
        // setup for building
        List<String> header = getHeader(lines.get(0), hasHeader);
        Map<String, Document> docs = new LinkedHashMap<>();
        Map<String, Document> paternity = new HashMap<>(); // childKey >> parentDoc
        String childSeparator = (childColumnDelimiter != null && !StringUtils.isBlank(childColumnDelimiter))
                ? childColumnDelimiter
                : ";";
        // build the documents
        for (String[] line : lines) {
            if (hasHeader) {
                continue; // skip header line
            }
            // build a document
            Document doc = build(line, header, keyColumnName, repSettings);
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

    public Document build(String[] line, List<String> header, String keyColumnName, List<RepresentativeSetting> representativeSettings) {
        // setup for building
        Document document = new Document();
        List<String> values = Arrays.asList(line);
        // populate metadata
        if (header.size() == values.size()) {
            for (int i = 0; i < values.size(); i++) {
                String fieldName = header.get(i);
                String value = values.get(i);
                document.addField(fieldName, value);
            }
        }
        else {
            throw new RuntimeException("The value size does not match the header size.");
        }
        // populate key, if there is no key column name the value in the first column is expected to be the key
        String keyValue = (keyColumnName != null && !StringUtils.isBlank(keyColumnName))
                ? document.getMetadata().get(keyColumnName)
                : document.getMetadata().get(header.get(0));
        document.setKey(keyValue);
        // populate representatives
        if (representativeSettings != null) {
            // setup for populating representatives
            Set<Representative> reps = new LinkedHashSet<>();
            for (RepresentativeSetting setting : representativeSettings) {
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
        if (parentColumnName != null && !StringUtils.isBlank(parentColumnName)) {
            // if we have a parent column name
            String parentKey = doc.getMetadata().get(parentColumnName);
            // check that the parentKey doesn't refer to itself
            if (parentKey.equals(doc.getKey()) || StringUtils.isBlank(parentKey)) {
                // the parentid value refers to itself or there is no parent
                // do nothing here
            }
            else {
                Document parent = docs.get(parentKey);
                // check that a parent exists
                if (parent != null) {
                    setRelationships(doc, parent);
                    // validate relationships if both parent & child fields exists
                    if (childColumnName != null && !StringUtils.isBlank(childColumnName)) {
                        // log paternity so we can check for children who disown their parent
                        String childrenLine = doc.getMetadata().get(childColumnName);
                        if (StringUtils.isBlank(childrenLine)) {
                            String[] childKeys = childrenLine.split(childSeparator);
                            // the child docs haven't been added yet so we'll record the relationships and add them later
                            for (String childKey : childKeys) {
                                paternity.put(childKey, doc); // paternity maps childKey >> parentDoc
                            }
                        }
                        // check for reciprocal relationships
                        if (parent.getMetadata().get(childColumnName).contains(parentKey)) {
                            // the relationship is reciprocal
                            // we'll check for orphans later
                            paternity.remove(doc.getKey());
                        }
                        else {
                            throw new RuntimeException("Broken families, the parent disowns a child document.");
                        }
                    }
                }
                else {
                    throw new RuntimeException("Broken families, the parent is missing.");
                }
            }
        }
        else if (childColumnName != null && !StringUtils.isBlank(childColumnName)) {
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

    public List<String> getHeader(String[] headerValues, boolean hasHeader) {
        List<String> header = new ArrayList<>();
        // check if the supplied values are the header
        if (hasHeader) {
            header = Arrays.asList(headerValues);
        }
        else {
            // create arbitrary column names
            for (int i = 0; i < headerValues.length; i++) {
                header.add("Column " + i);
            }
        }
        return header;
    }
}
