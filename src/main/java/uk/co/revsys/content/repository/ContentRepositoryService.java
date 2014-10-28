package uk.co.revsys.content.repository;

import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import uk.co.revsys.content.repository.model.AbstractNode;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.BinaryNode;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Status;
import uk.co.revsys.content.repository.model.Version;

public interface ContentRepositoryService {

    public AbstractNode get(String path, boolean published) throws RepositoryException;
    
    public ContentNode create(String path, String name, Status status, String tags, String contentType, Map<String, String> properties) throws RepositoryException;
    
    public ContentNode update(String path, Status status, String tags, Map<String, String> properties) throws RepositoryException;
    
    public List<SearchResult> find(String expression, boolean published, int offset, int limit) throws RepositoryException;
    
    public void delete(String path) throws RepositoryException;
    
    public List<Version> getVersionHistory(String path) throws RepositoryException;
    
    public BinaryNode saveBinary(String path, String tags, Binary attachment) throws RepositoryException;
    
    public Binary getBinary(String path) throws RepositoryException;
    
}
