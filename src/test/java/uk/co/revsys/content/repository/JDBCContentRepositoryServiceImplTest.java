package uk.co.revsys.content.repository;

public class JDBCContentRepositoryServiceImplTest extends ContentRepositoryServiceImplTest{

    private ServiceInitializer serviceInitializer = new ServiceInitializer();
    
    @Override
    public void setUp() {
        serviceInitializer.init("jdbc-repository.json");
    }
    
    @Override
    public void tearDown() {
        serviceInitializer.shutdown();
    }

}
