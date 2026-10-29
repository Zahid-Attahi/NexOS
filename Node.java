import java.util.Date;

public abstract class Node {
    private String name;
    private final Date created;
    private Date lastModified;

    public Node(String name) {
        this.name = name;
        this.created = new Date();
        this.lastModified = this.created;
    }

    public String getName() {
        return name;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void touch() {
        this.lastModified = new Date();
    }

}