/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class AACOAuth2FilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig, OAuth2FilterConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = -3551428051398501603L;

    // DEFAULT VALUES - BEGIN -
    protected String cliendId;

    protected String clientSecret;
 
    protected String accessTokenUri = "http://localhost:8080/aac/oauth/token";

    protected String userAuthorizationUri = "http://localhost:8080/aac/eauth/authorize";

    protected String redirectUri = "http://localhost:10000/geoserver";

    protected String checkTokenEndpointUrl = "http://localhost:8080/aac/resources/token";

    protected String logoutUri = "http://localhost:8080/aac/logout?target=http://localhost:10000/geoserver";

    protected String scopes = "profile.basicprofile.me,user.roles.me";

    protected Boolean enableRedirectAuthenticationEntryPoint = false;
    
    protected Boolean forceAccessTokenUriHttps = false;
    
    protected Boolean forceUserAuthorizationUriHttps = false;
    
    protected String loginEndpoint = "/j_spring_oauth2_aac_login";
    
    protected String logoutEndpoint = "/j_spring_oauth2_aac_logout";
    
    protected String userRolesEndpoint = "http://localhost:8280/aacroles/1.0.0/userroles/me";
    
    protected String apiManagerDomain = "sco.geoserver";
    
    protected String rolePrefix = "geo_";

    protected String workspaceURI = "http://www.openplans.org/";
    // DEFAULT VALUES - END -

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    /**
     * @return the cliendId
     */
    public String getCliendId() {
        return cliendId;
    }

    /**
     * @param cliendId the cliendId to set
     */
    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the accessTokenUri
     */
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    /**
     * @param accessTokenUri the accessTokenUri to set
     */
    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    /**
     * @return the userAuthorizationUri
     */
    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    /**
     * @param userAuthorizationUri the userAuthorizationUri to set
     */
    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    /**
     * @return the redirectUri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * @param redirectUri the redirectUri to set
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * @return the checkTokenEndpointUrl
     */
    public String getCheckTokenEndpointUrl() {
        return checkTokenEndpointUrl;
    }

    /**
     * @param checkTokenEndpointUrl the checkTokenEndpointUrl to set
     */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    /**
     * @return the logoutUri
     */
    public String getLogoutUri() {
        return logoutUri;
    }

    /**
     * @param logoutUri the logoutUri to set
     */
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /**
     * @return the scopes
     */
    public String getScopes() {
        return scopes;
    }

    /**
     * @param scopes the scopes to set
     */
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getUserRolesEndpoint() {
		return userRolesEndpoint;
	}

	public void setUserRolesEndpoint(String userRolesEndpoint) {
		this.userRolesEndpoint = userRolesEndpoint;
	}

	public String getApiManagerDomain() {
		return apiManagerDomain;
	}

	public void setApiManagerDomain(String apiManagerDomain) {
		this.apiManagerDomain = apiManagerDomain;
	}

	public String getRolePrefix() {
		return rolePrefix;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
	}

        public String getWorkspaceURI() {
        return workspaceURI;
    }

    public void setWorkspaceURI(String workspaceURI) {
        this.workspaceURI = workspaceURI;
    }

	/**
     * @return the enableRedirectAuthenticationEntryPoint
     */
    public Boolean getEnableRedirectAuthenticationEntryPoint() {
        return enableRedirectAuthenticationEntryPoint;
    }

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to set
     */
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint) {
        this.enableRedirectAuthenticationEntryPoint = enableRedirectAuthenticationEntryPoint;
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {
                final StringBuilder loginUri = new StringBuilder(getUserAuthorizationUri());
                loginUri.append("?").append("response_type=code").append("&").append("client_id=")
                        .append(getCliendId()).append("&").append("scope=")
                        .append(getScopes().replace(",", "%20")).append("&").append("redirect_uri=")
                        .append(getRedirectUri());

                if (getEnableRedirectAuthenticationEntryPoint()
                        || request.getRequestURI().endsWith(getLoginEndpoint())) {
                    response.sendRedirect(loginUri.toString());
                }
            }
        };
    }

    @Override
    public Boolean getForceAccessTokenUriHttps() {
        return forceAccessTokenUriHttps;
    }

    @Override
    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps) {
        this.forceAccessTokenUriHttps = forceAccessTokenUriHttps;
    }

    @Override
    public Boolean getForceUserAuthorizationUriHttps() {
        return forceUserAuthorizationUriHttps;
    }

    @Override
    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps) {
        this.forceUserAuthorizationUriHttps = forceUserAuthorizationUriHttps;
    }

    @Override
    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    @Override
    public void setLoginEndpoint(String loginEndpoint) {
        this.loginEndpoint = loginEndpoint;
    }

    @Override
    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

}
