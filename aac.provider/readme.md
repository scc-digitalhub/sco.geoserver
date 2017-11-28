PREREQUISITES:
- Geoserver (https://github.com/geoserver/geoserver/tree/2.12.1)

1. Clone oauth2-aac

2. Move oauth2-aac folder under ...\geoserver\src\community\security

3. Modify the following files:

- ...\geoserver\src\community\pom.xml
	Add this profile:
	
	<profile>
      <id>oauth2-aac</id>
      <modules>
        <module>security/oauth2</module>
        <module>security/oauth2-aac</module>
      </modules>
    </profile>

- ...\geoserver\src\web\app\pom.xml
	Add this profile:
	
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

- ...\geoserver\src\community\security\pom.xml
	Add this profile:
	<profile>
      <id>oauth2-aac</id>
      <modules>
        <module>oauth2</module>
        <module>oauth2-aac</module>
      </modules>
    </profile>

4. Compile Geoserver with OAuth2 extension:
	cd geoserver/src
	mvn clean install -DskipTests -P oauth2-aac

5. Build for Eclipse with extension enabled:
	cd geoserver/src
	mvn eclipse:eclipse -P oauth2-aac
	
	Import in Eclipse as "General -> Existing projects into workspace", with /geoserver as root directory.

6. Run gs-web-app/src/test/java/org.geoserver.web/Start.java to start the web interface

7. Configure filter:
	- log into the web interface as "admin" with password "geoserver"
	- navigate to Security -> Authentication
	- Under "Authentication Filters" section click "Add new"
	- Click "AAC OAuth2", fill the missing fields (Name, Client ID, Client Secret), select "User group service - default" as role source
	- Under "Filter Chains" section, add AAC filter for chains web, rest, gwc and default (click on chain name and add filter, always leave "anonymous" as last")
	- Save and log out