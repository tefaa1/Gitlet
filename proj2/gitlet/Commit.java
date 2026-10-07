package gitlet;


import java.io.Serializable;
import java.util.*;

/** Represents a gitlet commit object.
 *  @author mohamed abdellatif
 */
public class Commit implements Serializable {
    private String id;
    private String message;
    private String timeStamp;
    private Commit parent;
    private Commit secParent;

    private HashMap<String, String> refs;
    String merge;

    public Commit(String message, Commit parent, Commit secParent, String merge) {
        this.message = message;
        this.parent = parent;
        this.secParent = secParent;
        this.merge = merge;
        refs = new HashMap<>();
        // the initial commit is dated at the Unix epoch, like in the spec
        Date date = parent == null ? new Date(0) : new Date();
        Formatter formatter = new Formatter();
        formatter.format("%ta %tb %td %tT %tY %tz", date, date, date, date, date, date);
        timeStamp = formatter.toString();
    }

    public void setRefs(HashMap<String, String> refs) {
        this.refs = refs;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public Commit getParent() {
        return parent;
    }

    public Commit getSecParent() {
        return secParent;
    }

    public String getId() {
        return id;
    }

    public String getMerge() {
        return merge;
    }

    public HashMap<String, String> getRefs() {
        return refs;
    }
}
