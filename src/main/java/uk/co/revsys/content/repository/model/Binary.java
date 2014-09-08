package uk.co.revsys.content.repository.model;

import java.io.InputStream;

public class Binary {

    private String name;
    private String mimeType;
    private InputStream content;

    public Binary() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }

}
