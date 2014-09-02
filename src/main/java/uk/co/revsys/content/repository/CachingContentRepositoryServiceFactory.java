package uk.co.revsys.content.repository;

import java.util.HashMap;
import java.util.Map;

public abstract class CachingContentRepositoryServiceFactory implements ContentRepositoryServiceFactory {

    private Map<String, ContentRepositoryService> instances = new HashMap<String, ContentRepositoryService>();

    @Override
    public ContentRepositoryService getInstance(String workspace) {
        ContentRepositoryService instance = instances.get(workspace);
        if (instance == null) {
            instance = createInstance(workspace);
            instances.put(workspace, instance);
        }
        return instance;
    }

    public abstract ContentRepositoryService createInstance(String workspace);
}
