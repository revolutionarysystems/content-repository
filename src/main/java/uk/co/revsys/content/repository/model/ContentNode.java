package uk.co.revsys.content.repository.model;

import java.util.HashMap;
import java.util.Map;

public class ContentNode extends ContainerNode {

   private Map<String, String> properties = new HashMap<String, String>();

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
}
