/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.FileInputStream;
import java.io.IOException;
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
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
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
	OAuth2RestOperations oauth2RestTemplate;
	private static final String WS_OWNER = "OWNER_";
	private static final String SCO_PROVIDER = "PROVIDER_";

    public AACOAuthAuthenticationFilter(SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
        
        this.oauth2RestTemplate = oauth2RestTemplate;
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
    
    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
    	Collection<GeoServerRole> gsRoles = new ArrayList<GeoServerRole>();
    	
    	List<AACRole> roles = null;
    	OAuth2AccessToken token = oauth2RestTemplate.getOAuth2ClientContext().getAccessToken();
    	String path = ((AACOAuth2FilterConfig) filterConfig).getUserRolesEndpoint();
    	
    	//get AAC roles
    	if (token != null || path != null) {
    		if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Access token: " + token);
            }
    		
    		HttpHeaders headers = new HttpHeaders();
    		headers.set("Authorization", "Bearer "+token.getValue());
    		
    		ParameterizedTypeReference<List<AACRole>> listType = new ParameterizedTypeReference<List<AACRole>>() {};
    		roles = oauth2RestTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), listType).getBody();
    	}
    	
    	if (roles != null) {
    		if (LOGGER.isLoggable(Level.FINE)) {
    			String log = "";
    			for (AACRole role : roles) {
    				log += String.format("[role: %s, scope: %s, context: %s] ", role.getRole(), role.getScope(), role.getContext());
        		}
                LOGGER.fine("AAC roles for user: " + log);
            }
    		
    		for (AACRole role : roles) {
    			//normal user
    			if (role.getRole().equals(AACRole.USER) && roles.size() == 1) {
    				break;
    			}
    			//user is the provider of sco.geoserver
    			if (role.getRole().equals(AACRole.PROVIDER) && role.getContext() != null && role.getContext().equals(((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain())) {
    				SortedSet<GeoServerRole> providerRoles = getScoProviderRoles(principal);
    				for (GeoServerRole providerRole : providerRoles) {
    					gsRoles.add(providerRole);
					}
    				//gsRoles.add(GeoServerRole.ADMIN_ROLE);
    					
    			} else if (role.getRole().startsWith(((AACOAuth2FilterConfig) filterConfig).getRolePrefix())
    					&& role.getContext() != null && role.getContext().equals(((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain())
    					&& role.getScope().equalsIgnoreCase(AACRole.RoleScope.APPLICATION.toString())) {
    				//user is the owner of a workspace in sco.geoserver
    				String wsName = role.getRole().substring(((AACOAuth2FilterConfig) filterConfig).getRolePrefix().length());
    				
    				if(getWorkspace(wsName) != null) {
    					GeoServerRole wsOwner = getWorkspaceOwner(wsName, principal);
    					if(wsOwner != null) {
    						setPolicy(wsOwner, wsName);
    						gsRoles.add(wsOwner);
    					}
    				}
    			}
    		}
    	}
    	
    	if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Roles returned from getRoles: "+gsRoles);
        }
        return gsRoles;
    }
    
    /**
     * 
     * @param role
     * @param workspaceName
     */
    private void setPolicy(GeoServerRole role, String workspaceName) {
    	try {
			DataAccessRuleDAO dao = getSecurityManager().getApplicationContext().getBean(DataAccessRuleDAO.class);
			SortedSet<DataAccessRule> rules = dao.getRulesAssociatedWithRole(role.getAuthority());
			if(rules.isEmpty()) {
				DataAccessRule rule = new DataAccessRule(workspaceName, DataAccessRule.ANY, AccessMode.ADMIN, role.getAuthority());
				dao.addRule(rule);
				dao.storeRules();
			}
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
    private GeoServerRole getWorkspaceOwner(String workspaceName, String principal) {
    	GeoServerRole owner = null;
    	try {
    		GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
    		owner = roleService.getRoleByName(WS_OWNER + workspaceName);
    		if (owner == null) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				owner = new GeoServerRole(WS_OWNER + workspaceName);
    				owner.getProperties().setProperty("ws_name", workspaceName);
    				
    				store.addRole(owner);
    				store.associateRoleToUser(owner, principal);
    				store.store();
    			}
    		}
    		//TODO if role exists but is associated to a different username?
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
    	try {
    		GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
    		SortedSet<GeoServerRole> roles = roleService.getRolesForUser(principal);
    		
    		if (roles.isEmpty()) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				GeoServerRole provider = new GeoServerRole(SCO_PROVIDER + ((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain());
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
    		
    		if (roleService.getRoleByName(SCO_PROVIDER + ((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain()) == null) {
    			if (roleService.canCreateStore()) {
    				GeoServerRoleStore store = roleService.createStore();
    				GeoServerRole provider = new GeoServerRole(SCO_PROVIDER + ((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain());
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
		
		//use catalog bean to get or create workspace
		try {
			Catalog catalog = (Catalog) getSecurityManager().getApplicationContext().getBean("rawCatalog");
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
				
				try {
					Properties prop = new Properties();
					String path = Thread.currentThread().getContextClassLoader().getResource("namespace.properties").getPath();
					prop.load(new FileInputStream(path));
					String uri = prop.getProperty("URI");
					namespace.setURI(uri + workspaceName);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (namespace.getURI() != null) {
					catalog.add(workspace);
					catalog.add(namespace);
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return workspace;
		
		/*
    	String apiUrl = "http://localhost:10000/geoserver/rest/workspaces";
		HttpHeaders headers = new HttpHeaders();
		try { //prepare authorization header
			GeoServerUser admin = getSecurityManager().loadUserGroupService("default").getUserByUsername("admin");
			String credentials = admin.getUsername() + ":";

			if(getSecurityManager().checkForDefaultAdminPassword()) {
				credentials += admin.DEFAULT_ADMIN_PASSWD;
			}
			else {
				//TODO handle change of default password
//				GeoServerPBEPasswordEncoder encoder = (GeoServerPBEPasswordEncoder)(getSecurityManager().loadPasswordEncoder("pbePasswordEncoder"));
//				if (encoder != null) {
//					credentials += encoder.decode(admin.getPassword()); //javax.crypto.BadPaddingException
//				}
			}
			
			System.out.println(credentials);
			
			String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
			headers.set("Authorization", "Basic " + encodedCredentials);
			
			try { //try to get workspace
				//TODO change WorkspaceInfoImpl to WorkspaceInfo and check that it works
				ParameterizedTypeReference<WorkspaceInfoImpl> workspaceJson = new ParameterizedTypeReference<WorkspaceInfoImpl>() {};
				WorkspaceInfoImpl workspace = oauth2RestTemplate.exchange(apiUrl+"/"+workspaceName, HttpMethod.GET, new HttpEntity<>(headers), workspaceJson).getBody();
				if(workspace != null)
					ret = true;
			} catch (HttpClientErrorException e) {
				if(e.getStatusCode() == HttpStatus.NOT_FOUND) { //workspace does not exist
					try {
						headers.setContentType(MediaType.APPLICATION_XML);
						
						String xmlTemplate = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workspace><name>%s</name></workspace>", workspaceName);
						
						HttpEntity<String> entity = new HttpEntity<String>(xmlTemplate, headers);
						ResponseEntity<String> wsInfo = oauth2RestTemplate.postForEntity(apiUrl, entity, String.class);
						
						if(wsInfo.getStatusCode() == HttpStatus.CREATED)
							ret = true;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return ret;
		*/
    }

	private static class AACRole {
		//{"id": 21,"scope": "system","role": "ROLE_ADMIN","context": null,"authority": "ROLE_ADMIN"}
		//{"id": 21,"scope": "system","role": "ROLE_USER","context": null,"authority": "ROLE__USER"}
		//{"id": 21,"scope": "tenant","role": "ROLE_PROVIDER","context": "sco.geoserver","authority": "ROLE_PROVIDER"}
		private int id;
		private String scope;
		private String role;
		private String context;
		private String authority;
		private static final String PROVIDER = "ROLE_PROVIDER";
		private static final String USER = "ROLE_USER";
		public enum RoleScope {
			SYSTEM, APPLICATION, TENANT, USER
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
	}
	

}
