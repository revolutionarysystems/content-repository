package uk.co.revsys.content.repository.security;

public interface AuthorisationHandler {

    public boolean isAdministrator();
    
    public String getUserWorkspace();
    
}
