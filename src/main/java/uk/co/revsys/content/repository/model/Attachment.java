package uk.co.revsys.content.repository.model;

import java.io.InputStream;

public class Attachment {

    private String name;
    private String contentType;
    private InputStream content;

    public Attachment() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }
    
}
