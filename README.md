# sco.geoserver
Extensions for GeoServer in order to integrate with the Open Services platform.


## Using Examples - OpenLayers

These examples show geospatial information from geoserver running on:

    geoserverUrl='http://localhost:8080/geoserver'

In examples/openlayers/config.js there are other url being used for integration with WSO2 AAC:

    Production Url:  wso2Url =   'http://192.168.43.202:8280'; 
    Api Url for accessing wms data: wmsUrl =    wso2Url+'/geoserverwmstest/1.0.0/wms2';
    Api Url for accessing wfs data: wfsUrl =    wso2Url+'/geoservertestwfs/1.0.0/wfs2';
    Api Url for accessing wcs data: wcsUrl =    wso2Url+'/geoservertest_wcs/1.0.0/wcs2';
    Api Url for accessing wps data: wpsUrl =    wso2Url+'/geoserver_wps/1.0.0/wps2';
    Api Url for executing post requests to wps: wpsUrlExecute =    wso2Url+'/geoserver_wps/1.0.0/execute';
    Authorization token: auth_token_wso2='Bearer '+'4f42ff1d-faf3-3d3b-9a07-efd001e38c5d';


