package uk.co.revsys.content.repository;

public interface ContentRepositoryServiceFactory {

    public ContentRepositoryService getInstance(String workspace);
    
}
