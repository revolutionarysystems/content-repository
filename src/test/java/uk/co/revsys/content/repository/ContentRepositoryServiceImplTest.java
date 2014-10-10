package uk.co.revsys.content.repository;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.tika.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.BinaryNode;
import uk.co.revsys.content.repository.model.ContainerNode;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Status;
import uk.co.revsys.content.repository.model.Version;
import uk.co.revsys.user.manager.model.User;
import uk.co.revsys.user.manager.test.util.AbstractShiroTest;

public class ContentRepositoryServiceImplTest extends AbstractShiroTest {

    private ServiceInitializer serviceInitializer = new ServiceInitializer();
    private Date start;

    public ContentRepositoryServiceImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws RepositoryException {
        start = new Date();
        serviceInitializer.init("repository.json");
        System.out.println("Setup: " + (new Date().getTime() - start.getTime()));
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        serviceInitializer.shutdown();
        System.out.println("Total: " + (new Date().getTime() - start.getTime()));
    }

    @Test
    public void test() throws Exception {
        // Set up mocks
        IMocksControl mocksControl = EasyMock.createControl();
        Subject mockSubject = mocksControl.createMock(Subject.class);
        setSubject(mockSubject);
        User user = new User();
        user.setId("1234");
        user.setName("Test User");
        PrincipalCollection mockPrincipalCollection = mocksControl.createMock(PrincipalCollection.class);
        expect(mockSubject.getPrincipals()).andReturn(mockPrincipalCollection).anyTimes();
        expect(mockPrincipalCollection.oneByType(User.class)).andReturn(user).anyTimes();
        mocksControl.replay();
        // Create repository
        ContentRepositoryService repository = new ContentRepositoryServiceImpl("default");
        // Create Test Item 1
        Date startPartial = new Date();
        Map<String, String> properties1 = new HashMap<String, String>();
        properties1.put("name", "Test Item 1");
        properties1.put("property1", "value1");
        properties1.put("property2", "This is another test. Blah blah blah. foobar");
        System.out.println("Creating item");
        ContentNode contentNode = repository.create("abc", "Test_Item_1", Status.published, "rcr/test", properties1);
        System.out.println("Create Item: " + (new Date().getTime() - startPartial.getTime()));
        assertEquals("/abc/Test_Item_1", contentNode.getPath());
        assertEquals("Test_Item_1", contentNode.getName());
        assertEquals(Status.published, contentNode.getStatus());
        startPartial = new Date();
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        System.out.println("Get Item: " + (new Date().getTime() - startPartial.getTime()));
        assertEquals("/abc/Test_Item_1", contentNode.getPath());
        assertEquals("Test_Item_1", contentNode.getName());
        assertEquals(Status.published, contentNode.getStatus());
        assertEquals("/abc", contentNode.getParent());
        assertEquals("1234", contentNode.getCreatedBy().getId());
        assertEquals("Test User", contentNode.getCreatedBy().getName());
        assertEquals("1234", contentNode.getModifiedBy().getId());
        assertEquals("Test User", contentNode.getModifiedBy().getName());
        assertEquals("rcr/test", contentNode.getContentType());
        assertEquals("Test Item 1", contentNode.getProperties().get("name"));
        assertEquals("value1", contentNode.getProperties().get("property1"));
        // Get root node
        ContainerNode rootNode = (ContainerNode) repository.get("", false);
        assertEquals("/", rootNode.getPath());
        // Update Test Item 1
        startPartial = new Date();
        properties1.put("property1", "value2");
        repository.update("abc/Test_Item_1", Status.published, properties1);
        System.out.println("Update Item: " + (new Date().getTime() - startPartial.getTime()));
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        assertEquals("value2", contentNode.getProperties().get("property1"));
        ContainerNode containerNode = (ContainerNode) repository.get("abc", false);
        assertEquals(1, containerNode.getChildren().size());
        assertEquals("Test_Item_1", containerNode.getChildren().get(0).getName());
        // Create Test Item 2
        Map<String, String> properties2 = new HashMap<String, String>();
        properties2.put("name", "Test Item 2");
        contentNode = repository.create("abc", "Test_Item_2", Status.draft, "rcr/test", properties2);
        assertEquals(Status.draft, contentNode.getStatus());
        contentNode = (ContentNode) repository.get("abc/Test_Item_2", false);
        try {
            contentNode = (ContentNode) repository.get("abc/Test_Item_2", true);
            fail("Expected PathNotFoundException to be thrown");
        } catch (PathNotFoundException ex) {
            // pass
        }
        containerNode = (ContainerNode) repository.get("abc", false);
        assertEquals(2, containerNode.getChildren().size());
        containerNode = (ContainerNode) repository.get("abc", true);
        assertEquals(1, containerNode.getChildren().size());
        startPartial = new Date();
        List<SearchResult> results = repository.find("value2", false, 0, 2);
        System.out.println("Search: " + (new Date().getTime() - startPartial.getTime()));
        System.out.println(new ObjectMapper().writeValueAsString(results));
        assertEquals(1, results.size());
        assertEquals("Test_Item_1", results.get(0).getNode().getName());
        // Delete Test Item 2
        startPartial = new Date();
        repository.delete("abc/Test_Item_2");
        System.out.println("Delete Item: " + (new Date().getTime() - startPartial.getTime()));
        containerNode = (ContainerNode) repository.get("abc", false);
        assertEquals(1, containerNode.getChildren().size());
        try {
            repository.get("abc/Test_Item_2", false);
            fail("Expected path not found exception to be thrown");
        } catch (PathNotFoundException ex) {

        }
        // Get Versions
        List<Version> versions = repository.getVersionHistory("abc/Test_Item_1");
        assertEquals(2, versions.size());
        // Save binary
        startPartial = new Date();
        Binary binary = new Binary();
        binary.setName("test.txt");
        binary.setMimeType("text/plain");
        binary.setContent(new ByteArrayInputStream("This is a test".getBytes()));
        repository.saveBinary("abc", binary);
        System.out.println("Save binary: " + (new Date().getTime() - startPartial.getTime()));
        BinaryNode binaryNode = (BinaryNode) repository.get("abc/test.txt", false);
        assertEquals("/abc/test.txt", binaryNode.getPath());
        assertEquals("test.txt", binaryNode.getName());
        assertEquals("text/plain", binaryNode.getMimeType());
        assertEquals("rcr/binary", binaryNode.getContentType());
        assertEquals("1234", binaryNode.getCreatedBy().getId());
        // Retrieve binary
        startPartial = new Date();
        binary = repository.getBinary("abc/test.txt");
        System.out.println("Get binary: " + (new Date().getTime() - startPartial.getTime()));
        assertEquals("text/plain", binary.getMimeType());
        assertEquals("This is a test", IOUtils.toString(binary.getContent()));
        results = repository.find("test.txt", false, 0, 0);
        System.out.println(new ObjectMapper().writeValueAsString(results));
        assertEquals(1, results.size());
        assertEquals("test.txt", results.get(0).getNode().getName());
        // Delete binary
        repository.delete("abc/test.txt");
        try {
            repository.get("abc/Test_Item_2", false);
            fail("Expected path not found exception to be thrown");
        } catch (PathNotFoundException ex) {

        }
        // Add parts to content node
        repository.saveBinary("abc/Test_Item_1", binary);
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        assertEquals(1, contentNode.getChildren().size());
        repository.delete("abc/Test_Item_1/test.txt");
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        assertEquals(0, contentNode.getChildren().size());
        repository.create("abc/Test_Item_1", "Sub_Item_1", Status.published, "test/item", properties2);
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        assertEquals(1, contentNode.getChildren().size());
        repository.update("abc/Test_Item_1/Sub_Item_1", Status.published, properties2);
        repository.delete("abc/Test_Item_1/Sub_Item_1");
        contentNode = (ContentNode) repository.get("abc/Test_Item_1", false);
        assertEquals(0, contentNode.getChildren().size());
        // Using an alternative workspace
        repository = new ContentRepositoryServiceImpl("other");
        rootNode = (ContainerNode) repository.get("", false);
        assertEquals("/", rootNode.getPath());
    }

}
