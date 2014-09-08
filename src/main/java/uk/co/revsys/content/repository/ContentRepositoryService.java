package uk.co.revsys.content.repository;

import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import uk.co.revsys.content.repository.model.AbstractNode;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.BinaryNode;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Version;

public interface ContentRepositoryService {

    public AbstractNode get(String path) throws RepositoryException;
    
    public ContentNode create(String path, String name, String contentType, Map<String, String> properties) throws RepositoryException;
    
    public ContentNode update(String path, Map<String, String> properties) throws RepositoryException;
    
    public List<SearchResult> find(String expression, int offset, int limit) throws RepositoryException;
    
    public void delete(String path) throws RepositoryException;
    
    public List<Version> getVersionHistory(String path) throws RepositoryException;
    
    public BinaryNode saveBinary(String path, Binary attachment) throws RepositoryException;
    
    public Binary getBinary(String path) throws RepositoryException;
    
}
