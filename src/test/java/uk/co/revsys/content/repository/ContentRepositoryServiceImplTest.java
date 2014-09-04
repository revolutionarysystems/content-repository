
package uk.co.revsys.content.repository;

import uk.co.revsys.content.repository.ServiceInitializer;
import uk.co.revsys.content.repository.ContentRepositoryServiceImpl;
import java.io.ByteArrayInputStream;
import java.util.List;
import javax.jcr.PathNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.revsys.content.repository.model.ContentNode;
import static org.junit.Assert.*;
import uk.co.revsys.content.repository.model.Attachment;
import uk.co.revsys.content.repository.model.SearchResult;
import uk.co.revsys.content.repository.model.Version;
import uk.co.revsys.user.manager.model.User;
import uk.co.revsys.user.manager.test.util.AbstractShiroTest;

public class ContentRepositoryServiceImplTest extends AbstractShiroTest{

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
        IMocksControl mocksControl = EasyMock.createControl();
        Subject mockSubject = mocksControl.createMock(Subject.class);
        setSubject(mockSubject);
        User user = new User();
        user.setId("1234");
        user.setName("Test User");
        PrincipalCollection mockPrincipalCollection = mocksControl.createMock(PrincipalCollection.class);
        expect(mockSubject.getPrincipals()).andReturn(mockPrincipalCollection).times(5);
        expect(mockPrincipalCollection.oneByType(User.class)).andReturn(user).times(5);
        mocksControl.replay();
        ContentRepositoryServiceImpl repository = new ContentRepositoryServiceImpl("default");
        TestObject object1 = new TestObject("Test Object 1");
        object1.setProperty1("value1");
        object1.getTags().add("tag1");
        ContentNode node = repository.create("abc", object1);
        assertEquals("Test_Object_1", node.getName());
        assertEquals("/abc/Test_Object_1", node.getPath());
        assertEquals("Test Object 1", ((TestObject)node.getContent()).getName());
        assertEquals("value1", ((TestObject)node.getContent()).getProperty1());
        node = repository.get("abc/Test_Object_1");
        assertEquals("Test_Object_1", node.getName());
        assertEquals("/abc/Test_Object_1", node.getPath());
        assertEquals("/abc", node.getParent());
        assertTrue(node.getChildren().isEmpty());
        assertEquals("Test Object 1", ((TestObject)node.getContent()).getName());
        assertEquals("value1", ((TestObject)node.getContent()).getProperty1());
        assertEquals("tag1", ((TestObject)node.getContent()).getTags().get(0));
        assertNotNull(node.getCreated());
        assertNotNull(node.getModified());
        assertEquals("Test User", node.getCreatedBy().getName());
        assertEquals("1234", node.getCreatedBy().getId());
        assertEquals("Test User", node.getModifiedBy().getName());
        assertEquals("1234", node.getModifiedBy().getId());
        object1.setProperty1("value2");
        repository.update("abc/Test_Object_1", object1);
        node = repository.get("abc/Test_Object_1");
        assertEquals("Test_Object_1", node.getName());
        assertEquals("/abc/Test_Object_1", node.getPath());
        assertEquals("Test Object 1", ((TestObject)node.getContent()).getName());
        assertEquals("value2", ((TestObject)node.getContent()).getProperty1());
        TestObject object2 = new TestObject("Test Object 2");
        object2.setProperty1("value3");
        repository.create("abc", object2);
        node = repository.get("abc");
        assertEquals("/abc", node.getPath());
        assertEquals("abc", node.getName());
        assertEquals(2, node.getChildren().size());
        List<SearchResult> results = repository.find("Test Object 1", 0, 0);
        assertEquals(2, results.size());
        assertTrue(results.get(0).getNode().getPath().startsWith("/abc/Test_Object"));
        results = repository.find("Test Object 1", 0, 1);
        assertEquals(1, results.size());
        results = repository.find("Test Object 1", 1, 0);
        assertEquals(1, results.size());
        List<Version> versions = repository.getVersionHistory("/abc/Test_Object_1");
        assertEquals(2, versions.size());
        repository.delete("abc/Test_Object_1");
        try{
            repository.get("abc/Test_Object_1");
            fail("Expected PathNotFoundException to be thrown");
        }catch(PathNotFoundException ex){
            //pass
        }
        repository.update("abc/Test_Object_2", object2);
        Attachment attachment = new Attachment();
        attachment.setName("test.txt");
        attachment.setContentType("text/plain");
        attachment.setContent(new ByteArrayInputStream("This is a test".getBytes()));
        repository.saveAttachment("abc/Test_Object_2", attachment);
        attachment = repository.getAttachment("abc/Test_Object_2", "test.txt");
        assertEquals("test.txt", attachment.getName());
        assertEquals("text/plain", attachment.getContentType());
        assertEquals("This is a test", IOUtils.toString(attachment.getContent()));
        attachment.setContent(new ByteArrayInputStream("This is not a test".getBytes()));
        repository.saveAttachment("abc/Test_Object_2", attachment);
        attachment = repository.getAttachment("abc/Test_Object_2", "test.txt");
        assertEquals("test.txt", attachment.getName());
        assertEquals("text/plain", attachment.getContentType());
        assertEquals("This is not a test", IOUtils.toString(attachment.getContent()));
        repository.deleteAttachment("abc/Test_Object_2", "test.txt");
        node = repository.get("");
        assertEquals("/", node.getPath());
        TestObject object3 = new TestObject("Test Object 3");
        node = repository.create("", object3);
        assertEquals("/Test_Object_3", node.getPath());
        assertEquals("Test_Object_3", node.getName());
        node = repository.get("Test_Object_3");
        assertEquals("/Test_Object_3", node.getPath());
        assertEquals("Test_Object_3", node.getName());
    }

}