import java.util.ArrayList;

import java.util.List;

public class Directory extends Node {
    private final List<Node> children;
        private Directory parent;

    public Directory(String name) {
        super(name);
        this.children = new ArrayList<>();
    }

    public List<Node> getChildren() {
        return children;

    }

    public boolean isEmpty() {
        return children.isEmpty();
    }
        public Directory getParent() {
        return parent;
    }

    public Node findChild(String name) {
        for (Node child : children) {
            if (child.getName().equals(name)) {
                return child;
            }

        }
        return null;
    }

    public void addChild(Node child) {
        children.add(child);
        if (child instanceof Directory) {
            ((Directory) child).parent = this;
        }
        touch();
    }

    public boolean removeChild(Node child) {
        boolean removed = children.remove(child);
        if (removed) {
            touch();
        }
        return removed;
    }
}