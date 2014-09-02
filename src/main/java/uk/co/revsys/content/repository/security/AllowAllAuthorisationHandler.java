package uk.co.revsys.content.repository.security;

public class AllowAllAuthorisationHandler implements AuthorisationHandler{

    @Override
    public boolean isAdministrator() {
        return true;
    }

    @Override
    public String getUserWorkspace() {
        return "default";
    }
    
}
