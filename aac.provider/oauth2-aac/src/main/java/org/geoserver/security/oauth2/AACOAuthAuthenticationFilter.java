/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class AACOAuthAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {
	OAuth2RestOperations oauth2RestTemplate;
	private static final String WS_OWNER = "ROLE_WS_OWNER";

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
    		System.out.println("Token: " + token);
    		
    		HttpHeaders headers = new HttpHeaders();
    		headers.set("Authorization", "Bearer "+token.getValue());
    		
    		ParameterizedTypeReference<List<AACRole>> listType = new ParameterizedTypeReference<List<AACRole>>() {};
    		roles = oauth2RestTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), listType).getBody();
    		
    		for (AACRole role : roles) {
    			System.out.println(role.getRole() +" " + role.getScope() + " " + role.getContext());
    		}
    	}
    	
    	if (roles != null) {
    		for (AACRole role : roles) {
    			//normal user
    			if (role.getRole().equals(AACRole.USER) && roles.size() == 1) {
    				break;
    			}
    			//user is the provider of sco.geoserver
    			if (role.getRole().equals(AACRole.PROVIDER) && role.getContext() != null && role.getContext().equals(((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain())) {
    				gsRoles.add(GeoServerRole.ADMIN_ROLE);
    			} else if (role.getRole().startsWith(((AACOAuth2FilterConfig) filterConfig).getRolePrefix())
    					&& role.getContext() != null && role.getContext().equals(((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain())
    					&& role.getScope().equalsIgnoreCase(AACRole.RoleScope.APPLICATION.toString())) {
    				//user is the owner of a workspace in sco.geoserver
    				String wsName = role.getRole().substring(((AACOAuth2FilterConfig) filterConfig).getRolePrefix().length());
    				System.out.println("workspace: "+wsName);
    				//check if workspace exists
    				if (checkWorkspace(wsName, token)) {
    					//
    				}
    				
    				GeoServerRole wsOwner = new GeoServerRole(WS_OWNER); //TODO check if it already exists
    				wsOwner.getProperties().setProperty("ws_name", wsName); //set name of the workspace owned by user as role property
    				wsOwner.setUserName(principal);
    				
//    				if (getSecurityManager().getActiveRoleService().canCreateStore()) {
//    					GeoServerRoleStore store = getSecurityManager().getActiveRoleService().createStore();
//    					if (store.getRoleByName(WS_OWNER) == null) {
//    						store.addRole(wsOwner);
//    						store.associateRoleToUser(wsOwner, principal);
//    						store.store();
//    					} else {
//    						store.updateRole(wsOwner);
//    						store.store();
//    					}
//    					
//    				}
    				gsRoles.add(wsOwner);
    			}
    		}
    	}
    	
    	//TODO policy (via REST): cache rules, check that role exists for WS
    	//rule format: <workspace>.<layer>.[r|w|a]
    	//json to add rule: {"rule": {"@resource": "<workspace>.<layer>.[r|w|a]","text": "role1,role2,..."}}
    	
    	DataAccessRuleDAO dao = getSecurityManager().getDataAccessRuleDAO();
    	//dao.
    	SortedSet<GeoServerRole> roleServiceRoles = getSecurityManager().getActiveRoleService().getRoles();
    	for (GeoServerRole role : roleServiceRoles) {
    		System.out.println("auth: "+role.getAuthority());
    	}
    	
    	System.out.println("roles returned from getRoles: "+gsRoles);
        return gsRoles;
    }
    
    private boolean checkWorkspace(String workspaceName, OAuth2AccessToken token) {
    	String apiUrl = "http://localhost:10000/geoserver/rest/workspaces";
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer "+token.getValue());
		boolean ret = false;
		//org.geoserver.rest.ResourceNotFoundException
		try {
			ParameterizedTypeReference<WorkspaceInfoImpl> workspaceJson = new ParameterizedTypeReference<WorkspaceInfoImpl>() {};
			WorkspaceInfoImpl workspace = oauth2RestTemplate.exchange(apiUrl+"/"+workspaceName, HttpMethod.GET, new HttpEntity<>(headers), workspaceJson).getBody();
			if(workspace != null)
				ret = true;
		} catch (HttpClientErrorException e) {
			if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
				try {
					headers.setContentType(MediaType.APPLICATION_JSON); //prevents 415 Unsupported Media Type
					
					Map<String,String> map = Collections.singletonMap("name", workspaceName);
					HttpEntity<Map<String,String>> request = new HttpEntity<Map<String,String>>(map, headers);
					
					//Map<String,String> answer = restTemplate.postForObject(apiUrl, request, Map.class);
					//System.out.println(answer);
					ParameterizedTypeReference<WorkspaceInfoImpl> wsList = new ParameterizedTypeReference<WorkspaceInfoImpl>() {};
					ResponseEntity<WorkspaceInfoImpl> wsInfo = oauth2RestTemplate.exchange(apiUrl, HttpMethod.POST, request, wsList);
					ret = true;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		System.out.println("here");
		return ret;
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
