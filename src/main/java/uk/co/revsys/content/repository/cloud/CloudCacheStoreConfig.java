package uk.co.revsys.content.repository.cloud;

public class CloudCacheStoreConfig extends org.infinispan.loaders.cloud.CloudCacheStoreConfig{

    public CloudCacheStoreConfig() {
        setCacheLoaderClassName(CloudCacheStore.class.getName());
    }

}
