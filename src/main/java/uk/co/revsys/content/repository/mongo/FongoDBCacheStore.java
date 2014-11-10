package uk.co.revsys.content.repository.mongo;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderMetadata;

@CacheLoaderMetadata(configurationClass = FongoDBCacheStoreConfig.class)
public class FongoDBCacheStore extends MongoDBCacheStore{

    public FongoDBCacheStore() {
        System.out.println("fongo");
    }

    @Override
    protected MongoClient getMongo(ServerAddress serverAddress, MongoClientOptions options) {
        System.out.println("getFongo");
        return new Fongo("Fongo Server").getMongo();
    }

    @Override
    public Class<? extends CacheLoaderConfig> getConfigurationClass() {
        System.out.println("getCC");
        return FongoDBCacheStoreConfig.class;
    }

}
