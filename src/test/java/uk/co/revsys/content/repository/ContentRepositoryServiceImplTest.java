package uk.co.revsys.content.repository;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.PathNotFoundException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.tika.io.IOUtils;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.revsys.content.repository.model.ContentNode;
import uk.co.revsys.user.manager.model.User;
import uk.co.revsys.user.manager.test.util.AbstractShiroTest;
import static org.junit.Assert.*;
import uk.co.revsys.content.repository.model.Binary;
import uk.co.revsys.content.repository.model.BinaryNode;
import uk.co.revsys.content.repository.model.ContainerNode;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Version;

public class ContentRepositoryServiceImplTest extends AbstractShiroTest {

    private ServiceInitializer serviceInitializer = new ServiceInitializer();

    public ContentRepositoryServiceImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        serviceInitializer.init();
    }

    @After
    public void tearDown() {
        serviceInitializer.shutdown();
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
        Map<String, String> properties1 = new HashMap<String, String>();
        properties1.put("name", "Test Item 1");
        properties1.put("property1", "value1");
        ContentNode contentNode = repository.create("abc", "Test_Item_1", "rcr/test", properties1);
        assertEquals("/abc/Test_Item_1", contentNode.getPath());
        assertEquals("Test_Item_1", contentNode.getName());
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals("/abc/Test_Item_1", contentNode.getPath());
        assertEquals("Test_Item_1", contentNode.getName());
        assertEquals("/abc", contentNode.getParent());
        assertEquals("1234", contentNode.getCreatedBy().getId());
        assertEquals("Test User", contentNode.getCreatedBy().getName());
        assertEquals("1234", contentNode.getModifiedBy().getId());
        assertEquals("Test User", contentNode.getModifiedBy().getName());
        assertEquals("rcr/test", contentNode.getContentType());
        assertEquals("Test Item 1", contentNode.getProperties().get("name"));
        assertEquals("value1", contentNode.getProperties().get("property1"));
        // Get root node
        ContainerNode rootNode = (ContainerNode) repository.get("");
        assertEquals("/", rootNode.getPath());
        // Update Test Item 1
        properties1.put("property1", "value2");
        repository.update("abc/Test_Item_1", properties1);
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals("value2", contentNode.getProperties().get("property1"));
        ContainerNode containerNode = (ContainerNode) repository.get("abc");
        assertEquals(1, containerNode.getChildren().size());
        assertEquals("Test_Item_1", containerNode.getChildren().get(0).getName());
        // Create Test Item 2
        Map<String, String> properties2 = new HashMap<String, String>();
        properties2.put("name", "Test Item 2");
        repository.create("abc", "Test_Item_2", "rcr/test", properties2);
        repository.get("abc/Test_Item_2");
        containerNode = (ContainerNode) repository.get("abc");
        assertEquals(2, containerNode.getChildren().size());
        List<SearchResult> results = repository.find("value2", 0, 0);
        assertEquals(1, results.size());
        assertEquals("Test_Item_1", results.get(0).getNode().getName());
        // Delete Test Item 2
        repository.delete("abc/Test_Item_2");
        containerNode = (ContainerNode) repository.get("abc");
        assertEquals(1, containerNode.getChildren().size());
        try{
            repository.get("abc/Test_Item_2");
            fail("Expected path not found exception to be thrown");
        }catch(PathNotFoundException ex){
            
        }
        // Get Versions
        List<Version> versions = repository.getVersionHistory("abc/Test_Item_1");
        assertEquals(2, versions.size());
        // Save binary
        Binary binary = new Binary();
        binary.setName("test.txt");
        binary.setMimeType("text/plain");
        binary.setContent(new ByteArrayInputStream("This is a test".getBytes()));
        repository.saveBinary("abc", binary);
        BinaryNode binaryNode = (BinaryNode) repository.get("abc/test.txt");
        assertEquals("/abc/test.txt", binaryNode.getPath());
        assertEquals("test.txt", binaryNode.getName());
        assertEquals("text/plain", binaryNode.getMimeType());
        assertEquals("rcr/binary", binaryNode.getContentType());
        assertEquals("1234", binaryNode.getCreatedBy().getId());
        // Retrieve binary
        binary = repository.getBinary("abc/test.txt");
        assertEquals("text/plain", binary.getMimeType());
        assertEquals("This is a test", IOUtils.toString(binary.getContent()));
        // Delete binary
        repository.delete("abc/test.txt");
        try{
            repository.get("abc/Test_Item_2");
            fail("Expected path not found exception to be thrown");
        }catch(PathNotFoundException ex){
            
        }
        // Add parts to content node
        repository.saveBinary("abc/Test_Item_1", binary);
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals(1, contentNode.getChildren().size());
        repository.delete("abc/Test_Item_1/test.txt");
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals(0, contentNode.getChildren().size());
        repository.create("abc/Test_Item_1", "Sub_Item_1", "test/item", properties2);
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals(1, contentNode.getChildren().size());
        repository.update("abc/Test_Item_1/Sub_Item_1", properties2);
        repository.delete("abc/Test_Item_1/Sub_Item_1");
        contentNode = (ContentNode) repository.get("abc/Test_Item_1");
        assertEquals(0, contentNode.getChildren().size());
    }

}
