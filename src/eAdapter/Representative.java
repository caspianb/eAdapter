package eAdapter;

import java.util.LinkedHashSet;
import java.util.Set;

public class Representative {

    public enum Type {
        IMAGE, NATIVE, TEXT
    }

    private Type type;
    private String name;
    private Set<String> files = new LinkedHashSet<>();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getFiles() {
        return files;
    }

    public void setFiles(Set<String> files) {
        this.files = files;
    }
}
