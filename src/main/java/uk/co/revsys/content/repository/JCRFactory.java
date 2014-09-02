package uk.co.revsys.content.repository;

import javax.jcr.Repository;

public class JCRFactory {

    private static Repository repository;

    public static Repository getRepository() {
        return repository;
    }

    public static void setRepository(Repository repository) {
        JCRFactory.repository = repository;
    }
    
    
}
