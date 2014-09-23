package uk.co.revsys.content.repository.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.revsys.content.repository.ContentRepositoryService;
import uk.co.revsys.content.repository.ContentRepositoryServiceFactory;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.SearchResult;

@Path("/public")
public class PublicContentRepositoryRestService {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractContentRepositoryRestService.class);

    private final ContentRepositoryServiceFactory repositoryFactory;
    private final ObjectMapper objectMapper;

    public PublicContentRepositoryRestService(ContentRepositoryServiceFactory repositoryFactory, ObjectMapper objectMapper) {
        this.repositoryFactory = repositoryFactory;
        this.objectMapper = objectMapper;
    }

    @GET
    @Path("/{workspace}/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNode(@PathParam("workspace") String workspace, @PathParam("path") String path) {
        try {
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
    
    @GET
    @Path("/{workspace}/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findNodes(@PathParam("workspace") String workspace, @QueryParam("query") String query, @QueryParam("offset") int offset, @QueryParam("limit") int limit) {
        try {
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
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
    @Path("/{workspace}/binary/{path:.*}")
    public Response getBinary(@PathParam("workspace") String workspace, @PathParam("path") String path) {
        try {
            ContentRepositoryService repository = repositoryFactory.getInstance(workspace);
            Binary attachment = repository.getBinary(path);
            return Response.ok(attachment.getContent()).type(attachment.getMimeType()).build();
        } catch (RepositoryException ex) {
            LOGGER.error("Unable to get binary at " + path, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }
}
