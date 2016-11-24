package eAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Document {

    private String key;
    private Document parent;
    private List<Document> children;
    private Map<String, String> metadata;
    private Set<Representative> representatives;

    public Document getParent() {
        return parent;
    }

    public void setParent(Document parent) {
        this.parent = parent;
    }

    public List<Document> getChildren() {
        return children;
    }

    public void setChildren(List<Document> children) {
        this.children = children;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Set<Representative> getRepresentatives() {
        return representatives;
    }

    public void setRepresentatives(Set<Representative> representatives) {
        this.representatives = representatives;
    }

    public String getKey() {
        return key;
    }

    public void addField(String fieldName, String value) {
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<String, String>();
        }

        this.metadata.put(fieldName, value);
    }

}
