package uk.co.revsys.content.repository;

public class MongoContentRepositoryServiceImplTest extends ContentRepositoryServiceImplTest{

    private ServiceInitializer serviceInitializer = new ServiceInitializer();
    
    @Override
    public void setUp() {
        serviceInitializer.init("mongo-repository.json");
    }
    
    @Override
    public void tearDown() {
        serviceInitializer.shutdown();
    }

}
