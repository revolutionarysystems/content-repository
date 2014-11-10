package uk.co.revsys.content.repository.mongo;

public class FongoDBCacheStoreConfig extends MongoDBCacheStoreConfig{

    public FongoDBCacheStoreConfig() {
        super.setCacheLoaderClassName(FongoDBCacheStore.class.getName());
    }

}
