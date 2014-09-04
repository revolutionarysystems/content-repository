package uk.co.revsys.content.repository;

import java.util.LinkedList;
import java.util.List;
import uk.co.revsys.content.repository.annotation.ContentName;
import uk.co.revsys.content.repository.annotation.ContentType;

@ContentName("name")
@ContentType("test/object")
public class TestObject {

    private String name;
    private String property1;
    private List<String> tags = new LinkedList<String>();

    public TestObject() {
    }

    public TestObject(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
