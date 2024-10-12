package gitlet;

import java.io.Serializable;
/** Represents a gitlet Branch object.
 *  @author mohamed abdellatif
 */
public class Branch implements Serializable {
    private String name;
    private String id;
    boolean headOrNot;

    public Branch(String name, String id, boolean headOrNot) {
        this.name = name;
        this.id = id;
        this.headOrNot = headOrNot;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setID(String i) {
        this.id = i;
    }

    public void setHeadOrNot(boolean headOrNot) {
        this.headOrNot = headOrNot;
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public boolean getHeadOrNot() {
        return headOrNot;
    }
}
