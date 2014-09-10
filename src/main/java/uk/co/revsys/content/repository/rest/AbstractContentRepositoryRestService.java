package uk.co.revsys.content.repository.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.security.AuthorisationHandler;

@Path("/")
public abstract class AbstractContentRepositoryRestService {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractContentRepositoryRestService.class);

    private final ContentRepositoryServiceFactory repositoryFactory;
    private final ObjectMapper objectMapper;
    private final AuthorisationHandler authorisationHandler;

    public AbstractContentRepositoryRestService(ContentRepositoryServiceFactory repositoryFactory, ObjectMapper objectMapper, AuthorisationHandler authorisationHandler) {
        this.repositoryFactory = repositoryFactory;
        this.objectMapper = objectMapper;
        this.authorisationHandler = authorisationHandler;
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

    @DELETE
    @Path("/{path:.*}")
    public Response deleteNode(@PathParam("path") String path) {
        try {
            if (!isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            ContentRepositoryService repository = getRepository();
            repository.delete(path);
            return Response.noContent().build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to delete node", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findNodes(@QueryParam("query") String query, @QueryParam("offset") int offset, @QueryParam("limit") int limit) {
        try {
            ContentRepositoryService repository = getRepository();
            List<SearchResult> results = repository.find(query, offset, limit);
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
            if (!isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            ContentRepositoryService repository = getRepository();
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
    @Path("/binary/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveBinary(@PathParam("path") String path, BufferedInMultiPart bufferedInMultiPart) {
        try {
            if (!isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            ContentRepositoryService repository = getRepository();
            List<InPart> parts = bufferedInMultiPart.getParts();
            InPart part = parts.get(0);
            Binary binary = new Binary();
            String contentDisposition = part.getHeaders().getFirst("Content-Disposition");
            String fileName = null;
            Pattern regex = Pattern.compile("(?<=filename=\").*?(?=\")");
            Matcher regexMatcher = regex.matcher(contentDisposition);
            if (regexMatcher.find()) {
                fileName = regexMatcher.group();
            }
            binary.setName(fileName);
            binary.setMimeType(part.getContentType());
            binary.setContent(part.getInputStream());
            repository.saveBinary(path, binary);
            return Response.ok().build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to save binary at " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/binary/{path:.*}")
    public Response getBinary(@PathParam("path") String path) {
        try {
            if (!isAdministrator()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            ContentRepositoryService repository = getRepository();
            Binary attachment = repository.getBinary(path);
            return Response.ok(attachment.getContent()).type(attachment.getMimeType()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get binary at " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    public boolean isAdministrator() {
        return authorisationHandler.isAdministrator();
    }

    public ContentRepositoryService getRepository() {
        String workspace = authorisationHandler.getUserWorkspace();
        ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
        return repository;
    }

    public ContentRepositoryServiceFactory getRepositoryFactory() {
        return repositoryFactory;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public AuthorisationHandler getAuthorisationHandler() {
        return authorisationHandler;
    }

}
