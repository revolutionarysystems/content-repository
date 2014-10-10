package uk.co.revsys.content.repository;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.modeshape.jcr.api.query.Query;
import org.modeshape.jcr.api.query.QueryResult;
import uk.co.revsys.content.repository.model.AbstractNode;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.BinaryNode;
import uk.co.revsys.content.repository.model.ChildNode;
import uk.co.revsys.content.repository.model.ContainerNode;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Status;
import uk.co.revsys.content.repository.model.Version;
import uk.co.revsys.user.manager.model.User;

public class ContentRepositoryServiceImpl implements ContentRepositoryService {

    private static final String JCR_PROPERTY_PREFIX = "jcr:";
    private static final String INTERNAL_PROPERTY_PREFIX = "rcr:";
    private static final String INTERNAL_CONTENT_TYPE_PROPERTY = INTERNAL_PROPERTY_PREFIX + "content-type";
    private static final String INTERNAL_CREATED_BY_ID_PROPERTY = INTERNAL_PROPERTY_PREFIX + "createdBy-id";
    private static final String INTERNAL_CREATED_BY_NAME_PROPERTY = INTERNAL_PROPERTY_PREFIX + "createdBy-name";
    private static final String INTERNAL_CREATED_PROPERTY = INTERNAL_PROPERTY_PREFIX + "created";
    private static final String INTERNAL_MODIFIED_PROPERTY = INTERNAL_PROPERTY_PREFIX + "modified";
    private static final String INTERNAL_MODIFIED_BY_ID_PROPERTY = INTERNAL_PROPERTY_PREFIX + "modifiedBy-id";
    private static final String INTERNAL_MODIFIED_BY_NAME_PROPERTY = INTERNAL_PROPERTY_PREFIX + "modifiedBy-name";
    private static final String INTERNAL_STATUS_PROPERTY = INTERNAL_PROPERTY_PREFIX + "status";
    private static final String INTERNAL_CONTAINER_CONTENT_TYPE = "rcr/container";
    private static final String INTERNAL_BINARY_CONTENT_TYPE = "rcr/binary";
    private static final String INTERNAL_BINARY_FILE_NODE_NAME = INTERNAL_PROPERTY_PREFIX + "file";

    private final String workspace;

    public ContentRepositoryServiceImpl(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public AbstractNode get(String path, boolean published) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node node;
            if (path == null || path.isEmpty()) {
                node = root;
            } else {
                node = root.getNode(path);
            }
            AbstractNode nodeWrapper = createNodeWrapper(node, published);
            if(published && !nodeWrapper.getStatus().equals(Status.published)){
                throw new PathNotFoundException(path);
            }
            return nodeWrapper;
        } finally {
            session.logout();
        }
    }

    @Override
    public ContentNode create(String path, String name, Status status, String contentType, Map<String, String> properties) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node parentNode;
            if (path == null || path.isEmpty()) {
                parentNode = root;
            } else {
                if (root.hasNode(path)) {
                    parentNode = root.getNode(path);
                } else {
                    parentNode = root.addNode(path);
                }
            }
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkout(parentNode.getPath());
            }
            Node node = createNode(parentNode, name, status);
            for (Entry<String, String> property : properties.entrySet()) {
                node.setProperty(property.getKey(), property.getValue());
            }
            node.setProperty(INTERNAL_CONTENT_TYPE_PROPERTY, contentType);
            session.save();
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkin(parentNode.getPath());
            }
            return createContentNodeWrapper(node, false);
        } finally {
            session.logout();
        }
    }

    private Node createNode(Node parentNode, String name, Status status) throws RepositoryException {
        Node node = parentNode.addNode(name);
        node.addMixin("mix:versionable");
        node.setProperty(INTERNAL_CREATED_PROPERTY, Calendar.getInstance());
        node.setProperty(INTERNAL_MODIFIED_PROPERTY, Calendar.getInstance());
        node.setProperty(INTERNAL_STATUS_PROPERTY, status.name());
        try {
            User user = SecurityUtils.getSubject().getPrincipals().oneByType(User.class);
            node.setProperty(INTERNAL_CREATED_BY_ID_PROPERTY, user.getId());
            node.setProperty(INTERNAL_CREATED_BY_NAME_PROPERTY, user.getName());
            node.setProperty(INTERNAL_MODIFIED_BY_ID_PROPERTY, user.getId());
            node.setProperty(INTERNAL_MODIFIED_BY_NAME_PROPERTY, user.getName());
        } catch (UnavailableSecurityManagerException ex) {
            node.setProperty(INTERNAL_CREATED_BY_ID_PROPERTY, "");
            node.setProperty(INTERNAL_CREATED_BY_NAME_PROPERTY, "");
            node.setProperty(INTERNAL_MODIFIED_BY_ID_PROPERTY, "");
            node.setProperty(INTERNAL_MODIFIED_BY_NAME_PROPERTY, "");
        }
        return node;
    }

    @Override
    public ContentNode update(String path, Status status, Map<String, String> properties) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node node = root.getNode(path);
            VersionManager manager = session.getWorkspace().getVersionManager();
            manager.checkout(node.getPath());
            updateNode(node, status);
            for (Entry<String, String> property : properties.entrySet()) {
                node.setProperty(property.getKey(), property.getValue());
            }
            node.setProperty(INTERNAL_MODIFIED_PROPERTY, Calendar.getInstance());
            session.save();
            manager.checkin(node.getPath());
            return createContentNodeWrapper(node, false);
        } finally {
            session.logout();
        }
    }

    private void updateNode(Node node, Status status) throws RepositoryException {
        node.setProperty(INTERNAL_MODIFIED_PROPERTY, Calendar.getInstance());
        node.setProperty(INTERNAL_STATUS_PROPERTY, status.name());
        try {
            User user = SecurityUtils.getSubject().getPrincipals().oneByType(User.class);
            node.setProperty(INTERNAL_MODIFIED_BY_ID_PROPERTY, user.getId());
            node.setProperty(INTERNAL_MODIFIED_BY_NAME_PROPERTY, user.getName());
        } catch (UnavailableSecurityManagerException ex) {
            node.setProperty(INTERNAL_MODIFIED_BY_ID_PROPERTY, "");
            node.setProperty(INTERNAL_MODIFIED_BY_NAME_PROPERTY, "");
        }
    }

    @Override
    public void delete(String path) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node node = root.getNode(path);
            Node parentNode = node.getParent();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkout(parentNode.getPath());
            }
            node.remove();
            session.save();
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkin(parentNode.getPath());
            }
        } finally {
            session.logout();
        }
    }

    @Override
    public List<SearchResult> find(String expression, boolean published, int offset, int limit) throws RepositoryException {
        Session session = getSession();
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            //String language = Query.FULL_TEXT_SEARCH;
            //Query query = (Query) queryManager.createQuery(expression, language);
            String language = Query.JCR_SQL2;
            String queryString = "SELECT * FROM [nt:unstructured] WHERE CONTAINS(., '" + expression + "')";
            Query query = (Query) queryManager.createQuery(queryString, language);
            if (offset >= 0) {
                query.setOffset(offset);
            }
            if (limit > 0) {
                query.setLimit(limit);
            }
            QueryResult result = (QueryResult) query.execute();
            RowIterator rowIterator = result.getRows();
            List<SearchResult> results = new LinkedList<SearchResult>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.nextRow();
                Node node = row.getNode();
                AbstractNode nodeWrapper = createNodeWrapper(node, published);
                if (!published || nodeWrapper.getStatus().equals(Status.published)) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setScore(row.getScore());
                    searchResult.setNode(createNodeWrapper(node, published));
                    results.add(searchResult);
                }
            }
            return results;
        } finally {
            session.logout();
        }
    }

    @Override
    public List getVersionHistory(String path) throws RepositoryException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        Session session = getSession();
        try {
            List<Version> versions = new LinkedList<Version>();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            VersionHistory versionHistory = versionManager.getVersionHistory(path);
            VersionIterator iterator = versionHistory.getAllVersions();
            while (iterator.hasNext()) {
                javax.jcr.version.Version jcrVersion = iterator.nextVersion();
                Node node = jcrVersion.getFrozenNode();
                Version version = new Version();
                version.setName(node.getName());
                version.setPath(node.getPath());
                version.setCreationTime(jcrVersion.getCreated().getTime());
                versions.add(version);
            }
            return versions;
        } finally {
            session.logout();
        }
    }

    @Override
    public BinaryNode saveBinary(String path, Binary binary) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            Node parentNode;
            if (path == null || path.isEmpty()) {
                parentNode = root;
            } else {
                parentNode = root.getNode(path);
            }
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkout(parentNode.getPath());
            }
            Node node;
            Node contentNode;
            if (parentNode.hasNode(binary.getName())) {
                node = parentNode.getNode(binary.getName());
                if (!node.hasProperty(INTERNAL_CONTENT_TYPE_PROPERTY) || !node.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString().equals(INTERNAL_BINARY_CONTENT_TYPE)) {
                    throw new RepositoryException(path + "/" + binary.getName() + " already exists and is not a binary");
                }
                updateNode(node, Status.published);
                contentNode = node.getNode(INTERNAL_BINARY_FILE_NODE_NAME).getNode("jcr:content");
            } else {
                node = createNode(parentNode, binary.getName(), Status.published);
                node.setProperty(INTERNAL_CONTENT_TYPE_PROPERTY, INTERNAL_BINARY_CONTENT_TYPE);
                contentNode = node.addNode(INTERNAL_BINARY_FILE_NODE_NAME, NodeType.NT_FILE).addNode("jcr:content", NodeType.NT_RESOURCE);
            }
            contentNode.setProperty("jcr:data", session.getValueFactory().createBinary(binary.getContent()));
            contentNode.setProperty("jcr:mimeType", binary.getMimeType());
            session.save();
            if (parentNode.isNodeType("mix:versionable")) {
                versionManager.checkin(parentNode.getPath());
            }
            return createBinaryNodeWrapper(node);
        } finally {
            session.logout();
        }
    }

    @Override
    public Binary getBinary(String path) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node node = root.getNode(path);
            Node contentNode = node.getNode(INTERNAL_BINARY_FILE_NODE_NAME).getNode("jcr:content");
            Binary binary = new Binary();
            binary.setName(node.getName());
            binary.setMimeType(contentNode.getProperty("jcr:mimeType").getString());
            binary.setContent(contentNode.getProperty("jcr:data").getBinary().getStream());
            return binary;
        } finally {
            session.logout();
        }
    }

    private Session getSession() throws RepositoryException {
        Session session;
        try {
            session = JCRFactory.getRepository().login(workspace);
        } catch (NoSuchWorkspaceException ex) {
            session = JCRFactory.getRepository().login();
            session.getWorkspace().createWorkspace(workspace);
            session = JCRFactory.getRepository().login(workspace);
        }
        return session;
    }

    private AbstractNode createNodeWrapper(Node node, boolean published) throws RepositoryException {
        if (!node.hasProperty(INTERNAL_CONTENT_TYPE_PROPERTY)) {
            return createContainerNodeWrapper(node, published);
        } else {
            String contentType = node.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString();
            if (contentType.equals(INTERNAL_BINARY_CONTENT_TYPE)) {
                return createBinaryNodeWrapper(node);
            } else {
                return createContentNodeWrapper(node, published);
            }
        }
    }

    private ContentNode createContentNodeWrapper(Node node, boolean published) throws RepositoryException {
        ContentNode contentNode = new ContentNode();
        updateContentNodeWrapper(contentNode, node, published);
        return contentNode;
    }

    private ContainerNode createContainerNodeWrapper(Node node, boolean published) throws RepositoryException {
        ContainerNode containerNode = new ContainerNode();
        updateContainerNodeWrapper(containerNode, node, published);
        return containerNode;
    }

    private BinaryNode createBinaryNodeWrapper(Node node) throws RepositoryException {
        BinaryNode binaryNode = new BinaryNode();
        updateBinaryNodeWrapper(binaryNode, node);
        return binaryNode;
    }

    private void updateContainerNodeWrapper(ContainerNode containerNode, Node node, boolean published) throws RepositoryException {
        updateAbstractNodeWrapper(containerNode, node);
        NodeIterator iterator = node.getNodes();
        while (iterator.hasNext()) {
            Node child = iterator.nextNode();
            if (!child.getName().startsWith(JCR_PROPERTY_PREFIX) && (!published || child.getProperty(INTERNAL_STATUS_PROPERTY).getString().equals(Status.published.name()))) {
                ChildNode childNode = new ChildNode();
                childNode.setName(child.getName());
                childNode.setPath(child.getPath());
                if(child.hasProperty(INTERNAL_STATUS_PROPERTY)){
                    childNode.setStatus(Status.valueOf(child.getProperty(INTERNAL_STATUS_PROPERTY).getString()));
                }
                if (child.hasProperty(INTERNAL_CONTENT_TYPE_PROPERTY)) {
                    childNode.setContentType(child.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString());
                } else {
                    childNode.setContentType(INTERNAL_CONTAINER_CONTENT_TYPE);
                }
                containerNode.getChildren().add(childNode);
            }
        }
    }

    private void updateContentNodeWrapper(ContentNode contentNode, Node node, boolean published) throws RepositoryException {
        updateContainerNodeWrapper(contentNode, node, published);
        PropertyIterator iterator = node.getProperties();
        while (iterator.hasNext()) {
            Property property = iterator.nextProperty();
            if (!property.getName().startsWith(JCR_PROPERTY_PREFIX) && !property.getName().startsWith(INTERNAL_PROPERTY_PREFIX)) {
                contentNode.getProperties().put(property.getName(), property.getString());
            }
        }
    }

    private void updateBinaryNodeWrapper(BinaryNode binaryNode, Node node) throws RepositoryException {
        updateAbstractNodeWrapper(binaryNode, node);
        binaryNode.setMimeType(node.getNode(INTERNAL_BINARY_FILE_NODE_NAME).getNode("jcr:content").getProperty("jcr:mimeType").getString());
    }

    private void updateAbstractNodeWrapper(AbstractNode abstractNode, Node node) throws RepositoryException {
        abstractNode.setPath(node.getPath());
        abstractNode.setName(node.getName());
        try {
            abstractNode.setParent(node.getParent().getPath());
        } catch (ItemNotFoundException ex) {
            // Ignore
        }
        if(node.hasProperty(INTERNAL_STATUS_PROPERTY)){
            abstractNode.setStatus(Status.valueOf(node.getProperty(INTERNAL_STATUS_PROPERTY).getString()));
        }
        if (node.hasProperty(INTERNAL_CONTENT_TYPE_PROPERTY)) {
            abstractNode.setContentType(node.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString());
            abstractNode.setCreated(node.getProperty(INTERNAL_CREATED_PROPERTY).getDate().getTime());
            abstractNode.setModified(node.getProperty(INTERNAL_MODIFIED_PROPERTY).getDate().getTime());
            abstractNode.setCreatedBy(new uk.co.revsys.content.repository.model.User(node.getProperty(INTERNAL_CREATED_BY_ID_PROPERTY).getString(), node.getProperty(INTERNAL_CREATED_BY_NAME_PROPERTY).getString()));
            abstractNode.setModifiedBy(new uk.co.revsys.content.repository.model.User(node.getProperty(INTERNAL_MODIFIED_BY_ID_PROPERTY).getString(), node.getProperty(INTERNAL_MODIFIED_BY_NAME_PROPERTY).getString()));
        } else {
            abstractNode.setContentType(INTERNAL_CONTAINER_CONTENT_TYPE);
        }
    }
}
