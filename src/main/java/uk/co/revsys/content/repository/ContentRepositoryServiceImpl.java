package uk.co.revsys.content.repository;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
import org.apache.commons.beanutils.BeanUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.modeshape.jcr.api.query.Query;
import org.modeshape.jcr.api.query.QueryResult;
import uk.co.revsys.content.repository.annotation.ContentName;
import uk.co.revsys.content.repository.annotation.ContentType;
import uk.co.revsys.content.repository.annotation.Versioned;
import uk.co.revsys.content.repository.model.Attachment;
import uk.co.revsys.content.repository.model.ChildNode;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Version;
import uk.co.revsys.user.manager.model.User;

public class ContentRepositoryServiceImpl implements ContentRepositoryService {

    private static final String JCR_PROPERTY_PREFIX = "jcr:";
    private static final String INTERNAL_PROPERTY_PREFIX = "rcr:";
    private static final String INTERNAL_CONTENT_CLASS_PROPERTY = INTERNAL_PROPERTY_PREFIX + "content-class";
    private static final String INTERNAL_CONTENT_TYPE_PROPERTY = INTERNAL_PROPERTY_PREFIX + "content-type";
    private static final String INTERNAL_USER_PROPERTY = INTERNAL_PROPERTY_PREFIX + "user";
    private static final String INTERNAL_CREATED_PROPERTY = INTERNAL_PROPERTY_PREFIX + "created";
    private static final String INTERNAL_MODIFIED_PROPERTY = INTERNAL_PROPERTY_PREFIX + "modified";
    private static final String INTERNAL_ATTACHMENTS_FOLDER = INTERNAL_PROPERTY_PREFIX + "attachments";
    private static final String INTERNAL_ATTACHMENT_CONTENT_TYPE = "rcr/attachment";

    private final String workspace;

    public ContentRepositoryServiceImpl(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public ContentNode get(String path) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node node;
            if(path == null || path.isEmpty()){
                node = root;
            }else{
                node = root.getNode(path);
            }
            ContentNode contentNode = new ContentNode();
            contentNode = updateContentNode(contentNode, node);
            return contentNode;
        } finally {
            session.logout();
        }
    }

    @Override
    public ContentNode create(String path, Object content) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            Node parentNode;
            if(path == null || path.isEmpty()){
                parentNode = root;
            }else if (root.hasNode(path)) {
                parentNode = root.getNode(path);
            } else {
                parentNode = root.addNode(path);
            }
            boolean versioned = content.getClass().getAnnotation(Versioned.class) != null;
            Map<String, String> properties = BeanUtils.describe(content);
            ContentName contentNameAnnotation = content.getClass().getAnnotation(ContentName.class);
            String name;
            if (contentNameAnnotation == null) {
                name = UUID.randomUUID().toString();
            } else {
                name = properties.get(contentNameAnnotation.value());
            }
            name = name.replace(" ", "_");
            Node node;
            if (parentNode.hasNode(name)) {
                throw new RepositoryException(path + " already exists");
            }
            node = parentNode.addNode(name);
            if (versioned) {
                node.addMixin("mix:versionable");
            }
            for (Entry<String, String> property : properties.entrySet()) {
                node.setProperty(property.getKey(), property.getValue());
            }
            node.setProperty(INTERNAL_CREATED_PROPERTY, Calendar.getInstance());
            node.setProperty(INTERNAL_MODIFIED_PROPERTY, Calendar.getInstance());
            try {
                node.setProperty(INTERNAL_USER_PROPERTY, SecurityUtils.getSubject().getPrincipals().oneByType(User.class).getName());
            } catch (UnavailableSecurityManagerException ex) {
                node.setProperty(INTERNAL_USER_PROPERTY, "");
            }
            ContentType contentTypeAnnotation = content.getClass().getAnnotation(ContentType.class);
            node.setProperty(INTERNAL_CONTENT_TYPE_PROPERTY, contentTypeAnnotation.value());
            node.setProperty(INTERNAL_CONTENT_CLASS_PROPERTY, content.getClass().getName());
            session.save();
            ContentNode contentNode = createContentNode(node);
            return contentNode;
        } catch (InvocationTargetException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } finally {
            session.logout();
        }
    }

    @Override
    public ContentNode update(String path, Object content) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            boolean versioned = content.getClass().getAnnotation(Versioned.class) != null;
            Map<String, String> properties = BeanUtils.describe(content);
            Node node = root.getNode(path);
            if (versioned) {
                versionManager.checkout(node.getPath());
            }
            for (Entry<String, String> property : properties.entrySet()) {
                node.setProperty(property.getKey(), property.getValue());
            }
            node.setProperty(INTERNAL_CONTENT_CLASS_PROPERTY, content.getClass().getName());
            node.setProperty(INTERNAL_MODIFIED_PROPERTY, Calendar.getInstance());
            session.save();
            if (versioned) {
                versionManager.checkin(node.getPath());
            }
            ContentNode contentNode = createContentNode(node);
            return contentNode;
        } catch (InvocationTargetException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RepositoryException(ex);
        } finally {
            session.logout();
        }
    }

    @Override
    public void delete(String path) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            if (root.hasNode(path)) {
                Node node = root.getNode(path);
                node.remove();
            }
        } finally {
            session.logout();
        }
    }

    @Override
    public List<SearchResult> find(String query) throws RepositoryException {
        Session session = getSession();
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String language = Query.FULL_TEXT_SEARCH;
            QueryResult result = (QueryResult) queryManager.createQuery(query, language).execute();
            RowIterator rowIterator = result.getRows();
            List<SearchResult> results = new LinkedList<SearchResult>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.nextRow();
                SearchResult searchResult = new SearchResult();
                searchResult.setScore(row.getScore());
                Node node = row.getNode();
                searchResult.setPath(node.getPath());
                searchResult.setName(node.getName());
                results.add(searchResult);
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
                Version version = new Version();
                version.setName(jcrVersion.getName());
                version.setPath(jcrVersion.getPath());
                version.setCreationTime(jcrVersion.getCreated().getTime());
                versions.add(version);
            }
            return versions;
        } finally {
            session.logout();
        }
    }

    @Override
    public void saveAttachment(String path, Attachment attachment) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            if (!root.hasNode(path)) {
                throw new RepositoryException(path + " does not exist");
            }
            Node parentNode = root.getNode(path);
            Node attachmentsNode;
            if (parentNode.hasNode(INTERNAL_ATTACHMENTS_FOLDER)) {
                attachmentsNode = parentNode.getNode(INTERNAL_ATTACHMENTS_FOLDER);
            } else {
                attachmentsNode = parentNode.addNode(INTERNAL_ATTACHMENTS_FOLDER, NodeType.NT_FOLDER);
            }
            Node node;
            if (attachmentsNode.hasNode(attachment.getName())) {
                node = attachmentsNode.getNode(attachment.getName()).getNode("jcr:content");
            } else {
                node = attachmentsNode.addNode(attachment.getName(), NodeType.NT_FILE).addNode("jcr:content", NodeType.NT_RESOURCE);
            }
            Binary binary = session.getValueFactory().createBinary(attachment.getContent());
            node.setProperty("jcr:data", binary);
            node.setProperty("jcr:mimeType", attachment.getContentType());
            session.save();
        } finally {
            session.logout();
        }
    }

    @Override
    public Attachment getAttachment(String path, String name) throws RepositoryException {
        Session session = getSession();
        try {
            Node root = session.getRootNode();
            if (!root.hasNode(path)) {
                throw new RepositoryException(path + " does not exist");
            }
            Node parentNode = root.getNode(path);
            if (!parentNode.hasNode(INTERNAL_ATTACHMENTS_FOLDER + "/" + name)) {
                throw new RepositoryException("Attachment " + name + " does not exist for " + path);
            }
            Node node = parentNode.getNode(INTERNAL_ATTACHMENTS_FOLDER + "/" + name);
            Node contentNode = node.getNode("jcr:content");
            Attachment attachment = new Attachment();
            attachment.setName(node.getName());
            attachment.setContentType(contentNode.getProperty("jcr:mimeType").getString());
            Binary content = contentNode.getProperty("jcr:data").getBinary();
            InputStream stream = content.getStream();
            attachment.setContent(stream);
            return attachment;
        } finally {
            session.logout();
        }
    }

    private Session getSession() throws RepositoryException {
        return JCRFactory.getRepository().login(workspace);
    }

    private ContentNode createContentNode(Node node) throws RepositoryException {
        ContentNode contentNode = new ContentNode();
        return updateContentNode(contentNode, node);
    }

    private ContentNode updateContentNode(ContentNode contentNode, Node node) throws RepositoryException {
        contentNode.setName(node.getName());
        contentNode.setPath(node.getPath());
        if (node.getParent() != null) {
            contentNode.setParent(node.getParent().getPath());
        }
        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (!child.getPrimaryNodeType().getName().equals("nt:folder")) {
                ChildNode childNode = new ChildNode();
                childNode.setName(child.getName());
                childNode.setPath(child.getPath());
                childNode.setType(child.getPrimaryNodeType().getName());
                if (child.hasProperty(INTERNAL_CONTENT_CLASS_PROPERTY)) {
                    childNode.setContentType(child.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString());
                }
                contentNode.getChildren().add(childNode);
            }
        }
        contentNode.setType(node.getPrimaryNodeType().getName());
        if (node.hasProperty(INTERNAL_CONTENT_CLASS_PROPERTY)) {
            try {
                String contentType = node.getProperty(INTERNAL_CONTENT_CLASS_PROPERTY).getString();
                Map<String, String> properties = new HashMap<String, String>();
                PropertyIterator propertyIterator = node.getProperties();
                while (propertyIterator.hasNext()) {
                    Property property = propertyIterator.nextProperty();
                    if (!property.getName().startsWith(INTERNAL_PROPERTY_PREFIX) && !property.getName().startsWith(JCR_PROPERTY_PREFIX)) {
                        String value = property.getString();
                        if (value != null) {
                            properties.put(property.getName(), property.getString());
                        }
                    }
                }
                Object content = Class.forName(contentType).newInstance();
                org.apache.commons.beanutils.BeanUtils.populate(content, properties);
                contentNode.setContent(content);
                contentNode.setCreated(node.getProperty(INTERNAL_CREATED_PROPERTY).getDate().getTime());
                contentNode.setModified(node.getProperty(INTERNAL_MODIFIED_PROPERTY).getDate().getTime());
                contentNode.setUser(node.getProperty(INTERNAL_USER_PROPERTY).getString());
                contentNode.setContentType(node.getProperty(INTERNAL_CONTENT_TYPE_PROPERTY).getString());
                if (node.hasNode(INTERNAL_ATTACHMENTS_FOLDER)) {
                    Node attachmentsNode = node.getNode(INTERNAL_ATTACHMENTS_FOLDER);
                    NodeIterator attachmentsIterator = attachmentsNode.getNodes();
                    while (attachmentsIterator.hasNext()) {
                        Node attachment = attachmentsIterator.nextNode();
                        ChildNode childNode = new ChildNode();
                        childNode.setName(attachment.getName());
                        childNode.setPath("/attachment" + attachment.getPath().replace(INTERNAL_ATTACHMENTS_FOLDER + "/", ""));
                        childNode.setType(INTERNAL_ATTACHMENT_CONTENT_TYPE);
                        childNode.setContentType(attachment.getNode("jcr:content").getProperty("jcr:mimeType").getString());
                        contentNode.getChildren().add(childNode);
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new RepositoryException(ex);
            } catch (InvocationTargetException ex) {
                throw new RepositoryException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RepositoryException(ex);
            } catch (InstantiationException ex) {
                throw new RepositoryException(ex);
            }
        }
        return contentNode;
    }

}
