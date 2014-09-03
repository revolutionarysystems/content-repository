package uk.co.revsys.content.repository.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.apache.wink.common.model.multipart.InPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.revsys.content.repository.ContentRepositoryService;
import uk.co.revsys.content.repository.ContentRepositoryServiceFactory;
import uk.co.revsys.content.repository.ServiceInitializer;
import uk.co.revsys.content.repository.model.Attachment;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.security.AuthorisationHandler;

@Path("/")
public abstract class AbstractContentRepositoryRestService {

    private final Logger LOGGER = LoggerFactory.getLogger(ServiceInitializer.class);

    private final ContentRepositoryServiceFactory repositoryFactory;
    private final ObjectMapper objectMapper;
    private final AuthorisationHandler authorisationHandler;
    private final Class contentType;

    public AbstractContentRepositoryRestService(ContentRepositoryServiceFactory repositoryFactory, ObjectMapper objectMapper, AuthorisationHandler authorisationHandler, Class contentType) {
        this.repositoryFactory = repositoryFactory;
        this.objectMapper = objectMapper;
        this.authorisationHandler = authorisationHandler;
        this.contentType = contentType;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRootNode(){
        return getNode("");
    }
    
    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNode(@PathParam("path") String path) {
        try {
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            return Response.ok(objectMapper.writeValueAsString(repository.get(path))).build();
        } catch (JsonProcessingException ex) {
            LOGGER.error("Unable to get node " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get node " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/create/")
    public Response createNodeInRoot(String json){
        LOGGER.info("Create node in root");
        System.out.println("Create node in root");
        return createNode("", json);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)       
    @Path("/create/{path:.*}")
    public Response createNode(@PathParam("path") String path, String json) {
        LOGGER.info("Create node " + path + ": " + json);
        System.out.println("Create node " + path + ": " + json);
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Object content = objectMapper.readValue(json, contentType);
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            ContentNode node = repository.create(path, content);
            return Response.ok(objectMapper.writeValueAsString(node)).build();
        } catch (IOException ex) {
            LOGGER.error("Unable to create node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to create node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/update/{path:.*}")
    public Response updateNode(@PathParam("path") String path, String json) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Object content = objectMapper.readValue(json, contentType);
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            ContentNode node = repository.update(path, content);
            return Response.ok(objectMapper.writeValueAsString(node)).build();
        } catch (IOException ex) {
            LOGGER.error("Unable to update node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to update node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/delete/{path:.*}")
    public Response deleteNode(@PathParam("path") String path) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            repository.delete(path);
            return Response.noContent().build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to delete node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/query")
    public Response findNodes(@QueryParam("query") String query) {
        try {
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            List<SearchResult> results = repository.find(query);
            return Response.ok(objectMapper.writeValueAsString(results)).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to find nodes", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (JsonProcessingException ex) {
            LOGGER.error("Unable to find nodes", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/versions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersions(@PathParam("path") String path) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            return Response.ok(objectMapper.writeValueAsString(repository.getVersionHistory(path))).build();
        } catch (JsonProcessingException ex) {
            LOGGER.error("Unable to get versions for " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get versions for " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/attachment/{path:.*}")
    public Response saveAttachment(@PathParam("path") String path, BufferedInMultiPart bufferedInMultiPart) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            List<InPart> parts = bufferedInMultiPart.getParts();
            InPart part = parts.get(0);
            Attachment attachment = new Attachment();
            String contentDisposition = part.getHeaders().getFirst("Content-Disposition");
            String fileName = null;
            Pattern regex = Pattern.compile("(?<=filename=\").*?(?=\")");
            Matcher regexMatcher = regex.matcher(contentDisposition);
            if (regexMatcher.find()) {
                fileName = regexMatcher.group();
            }
            attachment.setName(fileName);
            attachment.setContentType(part.getContentType());
            attachment.setContent(part.getInputStream());
            repository.saveAttachment(path, attachment);
            return Response.ok().build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to save attachment for " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/attachment/{path:.*}")
    public Response getAttachment(@PathParam("path") String path) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            String name = path.substring(path.lastIndexOf("/") + 1);
            path = path.substring(0, path.lastIndexOf("/"));
            Attachment attachment = repository.getAttachment(path, name);
            return Response.ok(attachment.getContent()).type(attachment.getContentType()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get attachment for " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
    
    @DELETE
    @Path("/attachment/{path:.*}")
    public Response deleteAttachment(@PathParam("path") String path) {
        try {
            if (!authorisationHandler.isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String workspace = authorisationHandler.getUserWorkspace();
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            String name = path.substring(path.lastIndexOf("/") + 1);
            path = path.substring(0, path.lastIndexOf("/"));
            repository.deleteAttachment(path, name);
            return Response.noContent().build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get attachment for " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

}
