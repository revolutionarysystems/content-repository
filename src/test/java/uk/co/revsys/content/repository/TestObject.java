package uk.co.revsys.content.repository;

import uk.co.revsys.content.repository.annotation.ContentName;
import uk.co.revsys.content.repository.annotation.ContentType;
import uk.co.revsys.content.repository.annotation.Versioned;

@ContentName("name")
@Versioned
@ContentType("test/object")
public class TestObject {

    private String name;
    private String property1;

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
}
