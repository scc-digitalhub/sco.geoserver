/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.AACOAuth2FilterConfig;
import org.geoserver.security.oauth2.AACOAuthAuthenticationFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}.
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class AACOAuth2AuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<AACOAuth2FilterConfig, AACOAuth2AuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = 75616833259749745L;

    public AACOAuth2AuthProviderPanelInfo() {
        setComponentClass(AACOAuth2AuthProviderPanel.class);
        setServiceClass(AACOAuthAuthenticationFilter.class);
        setServiceConfigClass(AACOAuth2FilterConfig.class);
    }
}
