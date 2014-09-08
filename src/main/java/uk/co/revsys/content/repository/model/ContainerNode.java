package uk.co.revsys.content.repository.model;

import java.util.LinkedList;
import java.util.List;

public class ContainerNode extends AbstractNode{

    private List<ChildNode> children = new LinkedList<ChildNode>();

    public List<ChildNode> getChildren() {
        return children;
    }

    public void setChildren(List<ChildNode> children) {
        this.children = children;
    }
    
}
