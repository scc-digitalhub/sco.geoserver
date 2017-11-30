/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class AACOAuthAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {
	OAuth2RestOperations oauth2RestTemplate;

    public AACOAuthAuthenticationFilter(SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
        
        this.oauth2RestTemplate = oauth2RestTemplate;
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
    			if (role.getRole().equals(AACRole.PROVIDER) && role.getContext() != null && role.getContext().equals(((AACOAuth2FilterConfig) filterConfig).getApiManagerDomain())) {
    				gsRoles.add(GeoServerRole.ADMIN_ROLE);
    			}
    		}
    	}
    	
        //return super.getRoles(request, principal);
    	System.out.println("roles returned from getRoles: "+gsRoles);
        return gsRoles;
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
