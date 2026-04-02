package edu.jxust.agritrace.support;

import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public abstract class AuthenticatedIntegrationTestSupport {

    protected static final AuthUserSession PLATFORM_ADMIN_SESSION = new AuthUserSession(
            1L,
            "platform",
            "Platform Admin",
            "PLATFORM_ADMIN",
            null
    );

    protected static final AuthUserSession ENTERPRISE_ADMIN_SESSION = new AuthUserSession(
            2L,
            "enterprise_admin",
            "Enterprise Admin",
            "ENTERPRISE_ADMIN",
            1L
    );

    protected static final AuthUserSession OPERATOR_SESSION = new AuthUserSession(
            3L,
            "operator",
            "Field Operator",
            "OPERATOR",
            1L
    );

    protected static final AuthUserSession REGULATOR_SESSION = new AuthUserSession(
            4L,
            "regulator",
            "Regulator",
            "REGULATOR",
            null
    );

    @BeforeEach
    void setUpSecurityContext() {
        authenticateAs(PLATFORM_ADMIN_SESSION);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected void authenticateAs(AuthUserSession session) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                session,
                "test-token",
                List.of(new SimpleGrantedAuthority("ROLE_" + session.roleCode()))
        ));
        SecurityContextHolder.setContext(context);
    }
}
