package uk.co.revsys.content.repository;

public class ContentRepositoryServiceFactoryImpl extends CachingContentRepositoryServiceFactory {

    @Override
    public ContentRepositoryService createInstance(String workspace) {
        return new ContentRepositoryServiceImpl(workspace);
    }

}
