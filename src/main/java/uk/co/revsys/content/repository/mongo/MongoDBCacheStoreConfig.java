package uk.co.revsys.content.repository.mongo;

import org.infinispan.loaders.LockSupportCacheStoreConfig;
import org.infinispan.loaders.mongodb.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class MongoDBCacheStoreConfig extends LockSupportCacheStoreConfig {
   private static final Log log = LogFactory.getLog(MongoDBCacheStoreConfig.class, Log.class);
   private String host;
   private int port;
   private int timeout;
   private String username;
   private String password;
   private String database;
   private String collection;
   private int acknowledgment;

   public MongoDBCacheStoreConfig() {
      super.setCacheLoaderClassName(MongoDBCacheStore.class.getName());
   }

   public String getHost() {
      return host;
   }

   public int getPort() {
      return port;
   }

   public String getUsername() {
      return username;
   }

   public String getPassword() {
      return password;
   }

   public String getDatabase() {
      return database;
   }

   public String getCollectionName() {
      return collection;
   }

   public int getTimeout() {
      return timeout;
   }

   public int getAcknowledgment() {
      return acknowledgment;
   }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setAcknowledgment(int acknowledgment) {
        this.acknowledgment = acknowledgment;
    }
}
