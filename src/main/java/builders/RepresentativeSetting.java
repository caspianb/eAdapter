package builders;

import eAdapter.Representative;

public class RepresentativeSetting {
    private String representativeColumn;
    private String representativeName = "default";
    private Representative.Type representativeType;

    public void setColumn(String column) {
        this.representativeColumn = column;
    }

    public void setName(String name) {
        this.representativeName = name;
    }

    public void setType (Representative.Type type) {
        this.representativeType = type;
    }

    public String getColumn() {
        return this.representativeColumn;
    }

    public String getName() {
        return this.representativeName;
    }

    public Representative.Type getType() {
        return this.representativeType;
    }
}
