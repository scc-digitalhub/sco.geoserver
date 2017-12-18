
## Import data to Geoserver

A simple project to convert data to the format accepted by GeoServer.
It takes in consideration the fact that the set of information is initially in JSON format.
The steps to upload and visualize the data into Geoserver are:

1. Convert from JSON to GeoJSON
2. Convert from GoeJSON to ShapeFile format
3. Import ShapeFiles into GeoServer

Prerequisites:


    1. install the gdal-bin package:  apt install gdal-bin
    2. start Geoserver with WPS extention enabled mvn clean install -DskipTests -P wps

In order to use it will be necessary to change the following parameters:

    private static final String path = "path_to_the_source_file/";
    private static final String urlGeoserver = "http://localhost:8080/geoserver/";