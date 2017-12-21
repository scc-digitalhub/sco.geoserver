PREREQUISITES:
- Geoserver (https://github.com/geoserver/geoserver/tree/2.12.1)
- AAC


1. Clone sco.geoserver placing it under ...\geoserver\src\community\security

2. Modify the following files:

- ...\geoserver\src\community\pom.xml
  - Add this profile:
```
<profile>
  <id>oauth2-aac</id>
  <modules>
    <module>security/oauth2</module>
    <module>security/sco.geoserver/aac.provider/oauth2-aac</module>
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
      <module>sco.geoserver/aac.provider/oauth2-aac</module>
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

`mvn clean install -DskipTests -P oauth2-aac`

5. Build for Eclipse with extension enabled:

`cd geoserver/src`

`mvn eclipse:eclipse -P oauth2-aac`
	
Import in Eclipse as "General -> Existing projects into workspace", with /geoserver as root directory.

6. Run gs-web-app/src/test/java/org.geoserver.web/Start.java to start the web interface
NOTE: if AAC is running on port 8080 you need to change the port for Geoserver, e.g. adding the VM argument `-Djetty.port=<free_port>` in the run configurations in Eclipse

7. Create a provider on AAC:
	- Log into AAC as admin (http://localhost:8080/aac/login), go to "Admin" tab and create a new provider (use the email of an already registered user or register a new one beforehand); the default domain name is "sco.geoserver"
	- Log into WSO2 store (https://localhost:9443/store/) as the provider, enter the new domain and create a new application (e.g. "Geoserver")
	- Generate production keys for the new application and add the following callback URLs:
`http://localhost:10000/geoserver,http://localhost:10000/geoserver/`
	- Enter "carbon.super" domain and subscribe to AAC and AACRoles APIs with the new application
	- Log into AAC as the provider, click on the client app you just created, enter "Settings" tab and select the following options:
	  - Grant types: authorization code, password, client credentials, refresh token, native
	  - Enabled identity providers: internal (you will have to log in as admin and approve these settings from the "Admin" tab)
	- Enter "API Access" tab and select the following options:
	  - Basic profile service: profile.basicprofile.me
	  - Role management service: user.roles.me, user.roles.write, user.roles.read, user.roles.read.all, client.roles.read.all

8. Configure authentication filter on Geoserver:
	- log into Geoserver web interface as "admin" with password "geoserver"
	- navigate to Security -> Authentication
	- Under "Authentication Filters" section click "Add new"
	- Click "AAC OAuth2", fill in the missing fields (Name, Client ID, Client Secret), select "User group service - default" as role source
	NOTE: the API Manager domain must be the same domain name you created in AAC, if you did not use "sco.geoserver"
	- Under "Filter Chains" section, add AAC filter for chains web, rest, gwc and default (click on chain name and add filter, always leave "anonymous" as last")
	- Save and log out
