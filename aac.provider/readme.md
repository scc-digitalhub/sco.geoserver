## PREREQUISITES:
- Geoserver (https://github.com/geoserver/geoserver/tree/2.12.1)
	-`git clone https://github.com/geoserver/geoserver.git`

	-`cd geoserver`

	-`git checkout 2.12.1`


- AAC

1. Clone sco.geoserver and copy the folder aac.provider/oauth2-aac placing it under ...\geoserver\src\community\security

2. Modify the following files:

	- ...\geoserver\src\community\pom.xml
      - Add this profile:

```
<profile>
  <id>oauth2-aac</id>
  <modules>
    <module>security/oauth2</module>
    <module>security/oauth2-aac</module>
  </modules>
</profile>
```

	- ...\geoserver\src\web\app\pom.xml
      - Add this profile:

```
<profile>
  <id>oauth2-aac</id>
  <dependencies>
    <dependency>
      <groupId>org.geoserver.community</groupId>
      <artifactId>gs-sec-oauth2</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.geoserver.community</groupId>
      <artifactId>gs-sec-oauth2-aac</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
	<groupId>org.springframework.security.oauth</groupId>
	<artifactId>spring-security-oauth2</artifactId>
	<version>2.0.11.RELEASE</version>
  </dependency>
  </dependencies>
</profile>
```
	- ...\geoserver\src\community\security\pom.xml
      - Add this profile:

```
<profile>
  <id>oauth2-aac</id>
  <modules>
    <module>oauth2</module>
    <module>oauth2-aac</module>
  </modules>
</profile>
```

3. Verify that `<version>...</version>` in ...\geoserver\src\community\security\sco.geoserver\aac.provider\oauth2-aac\pom.xml corresponds to the version of Geoserver you are using:

```
	<parent>
		<groupId>org.geoserver.community</groupId>
		<artifactId>gs-security</artifactId>
		<version>YOUR_GEOSERVER_VERSION</version>
	</parent>

	<groupId>org.geoserver.community</groupId>
	<artifactId>gs-sec-oauth2-aac</artifactId>
	<packaging>jar</packaging>
	<version>YOUR_GEOSERVER_VERSION</version>
	<name>GeoServer OAuth2 Connect Security Module - AAC</name>
```

4. Compile Geoserver with OAuth2 extension:

`cd geoserver/src`

`mvn -DskipTests clean install -P wps,csw,oauth2-aac,authkey`

5. Build for Eclipse with extension enabled:

`cd geoserver/src`

`mvn eclipse:eclipse -P wps,csw,oauth2-aac,authkey`
	
Import in Eclipse as "General -> Existing projects into workspace", with /geoserver as root directory.

6. Run gs-web-app/src/test/java/org.geoserver.web/Start.java to start the web interface 
   
(from command-line use the commands: `cd geoserver/src/web/app   mvn jetty:run -Djetty.port=1000`  to start geoserver at port 1000)

NOTE: if AAC is running on port 8080 you need to change the port for Geoserver, e.g. adding the VM argument `-Djetty.port=<free_port>` in the run configurations in Eclipse

7. Create a provider (tenant) on AAC:
	- Log in to AAC as admin (http://localhost:8080/aac/login), go to "Admin" tab and create a new provider (use the email of an already registered user or register a new one beforehand); the default domain name is "sco.geoserver"
	- Log in to WSO2 store (https://localhost:9443/store/) as the provider, enter the new domain and create a new application (e.g. "Geoserver")
	- Generate production keys for the new application and add the following callback URLs (with the port you are using for Geoserver):
`http://localhost:10000/geoserver,http://localhost:10000/geoserver/`
	- Enter "carbon.super" domain and subscribe to AAC and AACRoles APIs with the new application
	- Log in to AAC as the provider, click on the client app you just created, enter "Settings" tab and select the following options:
	  - Grant types: authorization code, password, client credentials, refresh token, native
	  - Enabled identity providers: internal (you will have to log in as admin and approve these settings from the "Admin" tab)
	- Enter "API Access" tab and select the following options:
	  - Basic profile service: profile.basicprofile.me
	  - Role management service: user.roles.me, user.roles.write, user.roles.read, user.roles.read.all, client.roles.read.all
    
	NOTE: In order to create simple non-provider users, just sign up to AAC with a different email and password, then you can use those credentials to log in to Geoserver.

8. Configure authentication filter on Geoserver:
	- Log in to Geoserver web interface as "admin" with password "geoserver"
	- Navigate to Security -> Authentication
	- Under "Authentication Filters" section click "Add new"
	- Click "AAC OAuth2", fill in the missing fields (Name, Client ID, Client Secret), select "User group service - default" as role source
	
	NOTE: the API Manager domain must be the same domain name you created in AAC, if you did not use "sco.geoserver", and the role prefix is the prefix that will be used to create the AAC role for workspace owners
	- Under "Filter Chains" section, add AAC filter for chains web, rest, gwc and default (click on chain name and add filter, always leave "anonymous" as last)
	- Save and log out

************************************************************************

## LOGGING IN TO GEOSERVER AS OWNER OF A WORKSPACE IN sco.geoserver DOMAIN

Prerequisites:
- the user must be registered on AAC
- the user must be assigned an AAC role with the syntax `<prefix><workspace_name>`, e.g. "geo_myWS" (by now this role is not assigned programmatically but must be created by the provider via AACRoles API)


Procedure on Geoserver (retrieval/creation of workspace and its associated policy):
- Copy the file `namespace.properties` under `/geoserver/src/main/src/main/resources/`, or create a new one and write there the URI you want to use for your namespace (e.g. `URI=http://www.openplans.org/`)
- When the user logs in to Geoserver via AAC and has role `<prefix><workspace_name>`, the authentication filter:
  - checks if the workspace and the associated namespace exist in the catalog, otherwise they are created
  - checks that the role `OWNER_<workspace_name>` exists within the active role service, otherwise it is created
  - checks if the AAC user exists within the active user/group service , otherwise it is created
  - grants admin privileges on the workspace to the Geoserver role `OWNER_<workspace_name>`

Operations available for the authenticated user:
- CATALOG MODE = HIDE (default)
  - view and edit only workspace owned
  - view, add, edit and remove stores in workspace owned
  - view, add, edit and remove layers and layer groups in workspace owned
  - view, add, edit and remove styles in workspace owned
- CATALOG MODE = MIXED
  - view and edit only workspace owned
  - view, add, edit and remove stores in workspace owned
  - view, add, edit and remove layers and layer groups in workspace owned
  - view, add, edit and remove styles in workspace owned
- CATALOG MODE = CHALLENGE
  - view and edit all workspaces
  - view, add, edit and remove stores in any workspace
  - view, add, edit and remove layers and layer groups in any workspace
  - view global styles (read-only) and styles in workspace owned (not other workspaces')
  - add, edit or remove styles in workspace owned

    NOTE: Non-admin users cannot directly create new stores within their workspaces, they have to create the store, then go and save the workspace so that it is refreshed, then go back and publish the layers.

************************************************************************

## CONFIGURING `authkey` MODULE FOR OGC SERVICES (http://docs.geoserver.org/stable/en/user/community/authkey/index.html)

1. Compile Geoserver with authkey extension enabled and build for Eclipse (leave also AAC enabled!):

`cd geoserver/src`

`mvn clean install -DskipTests -P oauth2-aac,authkey`

`mvn eclipse:eclipse -P oauth2-aac,authkey`

2. Configure authentication filter on Geoserver:
	- Log in to Geoserver web interface as "admin" with password "geoserver"
	- Navigate to Security -> Authentication
	- Under "Authentication Filters" section click "Add new", select "AuthKey" and configure as follows:
	  - Name: a name of your choice
	  - Name of URL parameter: `authkey`
	  - Mapper: `Web Service`
	  - Web Service URL: `http://localhost:8080/aac/apikeycheck/{key}`
	  - Regular expression: `.?\"username\"\s*:\s*\"([^\"]+)\".`
	  - User/group service: `default`
	- Under "Filter Chains" section, add the new filter for chains gwc and default, placing it at the top of the chain
	- Save and log out

You can test the filter with Postman:
- You need two different AAC users and a layer with different access rules for certain roles (rules are set under Security -> Data)
  - e.g.: user bob@gmail.com has role GEOSERVER_ROLE1, user tom@gmail.com has role GEOSERVER_ROLE2 and the following access rule is set: `myworkspace.mylayer.r` to role GEOSERVER_ROLE1
- Generate the key for those users on AAC: log in to AAC as each user, select an application (if there is no application, you need to generate production keys for it on WSO2 store), navigate to "API Keys" and click "New API Key"
- To check that only users with role GEOSERVER_ROLE1 have read access to `mylayer`:
  - navigate to "Layer Preview"
  - open `mylayer` in OpenLayers format and copy the URL
  - prepare a GET request on Postman with the copied URL
  - add `authkey` to the list of parameters with the key value you generated on AAC and send the request (you can set `format` parameter to `image/png` for a better display)
  - if bob@gmail.com can see the layer while tom@gmail.com gets an HTTP 404, the plugin works correctly
