package uk.co.revsys.content.repository.security;

import org.apache.shiro.SecurityUtils;
import uk.co.revsys.user.manager.model.User;

public class AuthorisationHandlerImpl implements AuthorisationHandler {

    private final String administratorRole;

    public AuthorisationHandlerImpl(String administratorRole) {
        this.administratorRole = administratorRole;
    }

    @Override
    public boolean isAdministrator() {
        return SecurityUtils.getSubject().hasRole(administratorRole);
    }

    @Override
    public String getUserWorkspace(){
        User user = SecurityUtils.getSubject().getPrincipals().oneByType(User.class);
        return user.getAccount();
    }

}
