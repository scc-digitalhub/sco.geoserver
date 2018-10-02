/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class AACOAuthAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {
	private static final String WS_OWNER = "OWNER_";
	private static final String SCO_PROVIDER = "PROVIDER_";

    public AACOAuthAuthenticationFilter(SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
    }
    
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String cacheKey = authenticateFromCache(this, (HttpServletRequest) request,
                (HttpServletResponse) response);

        // Search for an access_token on the request (simulating SSO)
        String accessToken = request.getParameter("access_token");

        OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();

        if (accessToken != null && token != null && !token.getValue().equals(accessToken)) {
            restTemplate.getOAuth2ClientContext().setAccessToken(null);
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        /*
         * This cookie works only locally, when accessing the GeoServer GUI and on the same domain.
         * For remote access you need to logout from the GeoServer GUI.
         */
        final String gnCookie = getGeoNodeCookieValue(httpRequest);

        final Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        final Collection<? extends GrantedAuthority> authorities = (authentication != null
                ? authentication.getAuthorities() : null);

        if (accessToken == null && gnCookie == null && 
                (authentication != null && (authentication instanceof PreAuthenticatedAuthenticationToken) &&  
                !(authorities.size() == 1 && authorities.contains(GeoServerRole.ANONYMOUS_ROLE)))) {
            final AccessTokenRequest accessTokenRequest = restTemplate.getOAuth2ClientContext()
                    .getAccessTokenRequest();
            if (accessTokenRequest != null && accessTokenRequest.getStateKey() != null) {
                restTemplate.getOAuth2ClientContext()
                        .removePreservedState(accessTokenRequest.getStateKey());
            }
        }

        if (accessToken != null || authentication == null || (authentication != null
                && authorities.size() == 1 && authorities.contains(GeoServerRole.ANONYMOUS_ROLE))) {

            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication = authentication;
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager().getAuthenticationCache().put(getName(), cacheKey,
                            postAuthentication);
                }
            }
        }

        chain.doFilter(request, response);
	}
	
	private String getGeoNodeCookieValue(HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Inspecting the http request looking for the GeoNode Session ID.");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Found " + cookies.length + " cookies!");
            }
            for (Cookie c : cookies) {
                if (GEONODE_COOKIE_NAME.equals(c.getName())) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Found GeoNode cookie: " + c.getValue());
                    }
                    return c.getValue();
                }
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Found no cookies!");
            }
        }

        return null;
    }
    
	/**
	 * Get mapped Geoserver roles for AAC user.
	 * @param request
	 * @param principal
	 * @return Collection<GeoServerRole>
	 */
    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
    	Collection<GeoServerRole> gsRoles = new ArrayList<GeoServerRole>();
    	
    	List<AACRole> roles = null;
    	OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();
    	String path = ((AACOAuth2FilterConfig) filterConfig).getUserRolesEndpoint();
    	String definedContext = ((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain();
    	//get AAC roles
    	if (token != null || path != null) {
    		if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Access token: " + token);
            }
    		
    		HttpHeaders headers = new HttpHeaders();
    		headers.set("Authorization", "Bearer "+token.getValue());
    		
    		ParameterizedTypeReference<List<AACRole>> listType = new ParameterizedTypeReference<List<AACRole>>() {};
    		roles = restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), listType).getBody();
    	}
    	
    	if (roles != null) {
    		if (LOGGER.isLoggable(Level.FINE)) {
    			String log = "";
    			for (AACRole role : roles) {
    				log += String.format("[role: %s, scope: %s, context: %s] ", role.getRole(), role.getSpace(), role.getContext());
        		}
    			LOGGER.fine("AAC roles for user " + principal + ": " + log);
            }
    		    		
    		for (AACRole role : roles) {
    			if(role.getContext() != null && role.getContext().equals(definedContext)) {
	    			String wsName = role.getSpace();
	    			boolean isProvider = role.isProviderOf(definedContext);
	    			String type = (isProvider ? SCO_PROVIDER : WS_OWNER);
					if(getWorkspace(wsName) != null) {
						GeoServerRole wsOwner = getWorkspaceRole(wsName, principal,type);
						if(wsOwner != null) {
							setPolicy(wsOwner, wsName, isProvider);
							gsRoles.add(wsOwner);
						}
					}
    			}
    		}
    		createUser(principal);
    	}
    	
    	if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Roles returned from getRoles: "+gsRoles);
        }
        return gsRoles;
    }
    
    /**
     * Set policy for a given workspace, providing admin access (which includes also read and write) to the given role.
     * @param role role that is granted admin privileges over the workspace
     * @param workspaceName name of the workspace
     */
    private void setPolicy(GeoServerRole role, String workspaceName, boolean isProvider) {
    	try {
			DataAccessRuleDAO dao = getSecurityManager().getApplicationContext().getBean(DataAccessRuleDAO.class);
			SortedSet<DataAccessRule> rules = dao.getRulesAssociatedWithRole(role.getAuthority());
			DataAccessRule ruleOne = new DataAccessRule(workspaceName, DataAccessRule.ANY, AccessMode.READ, role.getAuthority());
			dao.addRule(ruleOne);
			if(isProvider) {
				DataAccessRule ruleAdmin = new DataAccessRule(workspaceName, DataAccessRule.ANY, AccessMode.ADMIN, role.getAuthority());
				dao.addRule(ruleAdmin);
			}
			
			dao.storeRules();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Check if a role for the owner of a workspace already exists, otherwise create it.
     * @param workspaceName workspace name
     * @param principal user associated to the role
     * @return GeoServerRole if role exists or has been created
     */
    private GeoServerRole getWorkspaceRole(String workspaceName, String principal, String type) {
    	GeoServerRole owner = null;
    	try {
    		GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
    		owner = roleService.getRoleByName(type + workspaceName);
    		if (owner == null) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				owner = new GeoServerRole(type + workspaceName);
    				owner.getProperties().setProperty("ws_name", workspaceName);
    				store.addRole(owner);
    				store.associateRoleToUser(owner, principal);
    				store.store();
    			}
    		}else {
    			GeoServerRoleStore store = roleService.createStore();
				store.associateRoleToUser(owner, principal);
				store.store();
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return owner;
    }
    
    /**
     * Get or create roles for the provider of the API Manager domain.
     * @param principal user associated to the role
     * @return GeoServerRole if role exists or has been created
     */
    private SortedSet<GeoServerRole> getScoProviderRoles(String principal) {
    	SortedSet<GeoServerRole> providerRoles = null;
    	String context = ((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain();
    	String scoDomain = context;
    	if(context.contains("/")) {
    		String [] temp = context.split("/");
    		scoDomain = temp[temp.length -1];
    	}
    	try {
    		GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
    		SortedSet<GeoServerRole> roles = roleService.getRolesForUser(principal);
    		
    		if (roles.isEmpty()) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				GeoServerRole provider = new GeoServerRole(SCO_PROVIDER + scoDomain);
    				store.addRole(provider);
    				store.associateRoleToUser(provider, principal);
    				
    				store.associateRoleToUser(store.getAdminRole(), principal);
    				store.store();
    			}
    		}
    		
    		if (!roles.contains(roleService.getAdminRole())) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				store.associateRoleToUser(store.getAdminRole(), principal);
    				store.store();
    			}
    		}
    		
    		if (roleService.getRoleByName(SCO_PROVIDER + scoDomain) == null) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				GeoServerRole provider = new GeoServerRole(SCO_PROVIDER + scoDomain);
    				store.addRole(provider);
    				store.associateRoleToUser(provider, principal);
    				store.store();
    			}
    		}

			RoleCalculator calc = new RoleCalculator(roleService);
			providerRoles = calc.calculateRoles(principal);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return providerRoles;
    }
    
    /**
     * Check if a workspace already exists, otherwise create it.
     * @param workspaceName workspace name
     * @return WorkspaceInfo if workspace exists or has been created
     */
    private WorkspaceInfo getWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;

        // use catalog bean to get or create workspace
        try {
            Catalog catalog =
                    (Catalog) getSecurityManager().getApplicationContext().getBean("rawCatalog");
            workspace = catalog.getWorkspaceByName(workspaceName);
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceName);

            if (workspace == null) {
                if (namespace != null) {
                    catalog.detach(namespace);
                }
                workspace = catalog.getFactory().createWorkspace();
                workspace.setName(workspaceName);
                namespace = catalog.getFactory().createNamespace();
                namespace.setPrefix(workspaceName);

                String uri = ((AACOAuth2FilterConfig) filterConfig).getWorkspaceURI();
                namespace.setURI(uri + workspaceName);

                if (namespace.getURI() != null) {
                    catalog.add(workspace);
                    catalog.add(namespace);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workspace;
    }

	/**
     * Create GeoServer User if it doesn't already exist
     * 
     * @param username
     * @return
     **/

    private void createUser(String username) {
        try {
            SortedSet <String> groupSer = getSecurityManager().listUserGroupServices();
            String gsname = groupSer.first();
        	GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(gsname);
            GeoServerUser u = service.getUserByUsername(username);           

            if (u == null) {
                GeoServerUserGroupStore ugstore = service.createStore();
                ugstore.load();
                String randomPassw = generatePassword();
                GeoServerUser newUser = ugstore.createUserObject(username, randomPassw, true);
                ugstore.addUser(newUser);
                ugstore.store();
            }
        } catch (IOException ex) {
            try {
                throw ex;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (PasswordPolicyException ex) {
            ;
            try {
                throw ex;
            } catch (PasswordPolicyException e) {
                e.printStackTrace();
            }
        }
    }

    private String generatePassword() {
    	SecureRandom random = new SecureRandom();
        return  new BigInteger(130, random).toString(32);
    }
    
    private static class AACRole {
		private int id;
		private String scope;
		private String role;
		private String context;
		private String authority;
		private String space;
		private static final String PROVIDER = "ROLE_PROVIDER";
		private static final String USER = "ROLE_USER";
		public enum RoleScope {
			SYSTEM, APPLICATION, TENANT, USER
		}
		
		/**
		 * Check whether this AACRole is provider of the given domain.
		 * @param domain API Manager domain
		 * @return
		 */
		public boolean isProviderOf(String context) {
			boolean res = false;
			if (this.getRole().equals(PROVIDER) && this.getContext() != null && this.getContext().equals(context))
				res = true;
			return res;
		}
		
		/**
		 * Check whether this AACRole is owner of a workspace in the given domain.
		 * @return
		 */
		public boolean isWorkspaceOwner(String context) {
			boolean res = false;
			if (this.getContext() != null && this.getContext().equals(context))
				res = true;
			return res;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getScope() {
			return scope;
		}
		public void setScope(String scope) {
			this.scope = scope;
		}
		public String getRole() {
			return role;
		}
		public void setRole(String role) {
			this.role = role;
		}
		public String getContext() {
			return context;
		}
		public void setContext(String context) {
			this.context = context;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getSpace() {
			return space;
		}
		public void setSpace(String space) {
			this.space = space;
		}
	}
}
