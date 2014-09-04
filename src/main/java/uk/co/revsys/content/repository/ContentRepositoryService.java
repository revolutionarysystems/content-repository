package uk.co.revsys.content.repository;

import java.util.List;
import javax.jcr.RepositoryException;
import uk.co.revsys.content.repository.model.Attachment;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Version;

public interface ContentRepositoryService<C extends Object> {

    public ContentNode<C> get(String path) throws RepositoryException;
    
    public ContentNode<C> create(String path, Object content) throws RepositoryException;
    
    public ContentNode<C> update(String path, Object content) throws RepositoryException;
    
    public List<SearchResult> find(String expression, int offset, int limit) throws RepositoryException;
    
    public void delete(String path) throws RepositoryException;
    
    public List<Version> getVersionHistory(String path) throws RepositoryException;
    
    public void saveAttachment(String path, Attachment attachment) throws RepositoryException;
    
    public Attachment getAttachment(String path, String name) throws RepositoryException;
    
    public void deleteAttachment(String path, String name) throws RepositoryException;
}
