var wso2Url =   'http://192.168.43.202:8280';
var wmsUrl =    wso2Url+'/geoserverwmstest/1.0.0/wms2';
var wfsUrl =    wso2Url+'/geoservertestwfs/1.0.0/wfs2';
var wcsUrl =    wso2Url+'/geoservertest_wcs/1.0.0/wcs2';
var auth_token_wso2='Bearer '+'4f42ff1d-faf3-3d3b-9a07-efd001e38c5d';

var wso2_layers = {};

wso2_layers['imagewms'] = new ol.layer.Tile({
    source: new ol.source.TileWMS({
      url:        wmsUrl,
      params:     {'LAYERS': 'topp:states', 'TILED': true,'format':'application/openlayers'},
      serverType: 'geoserver',
    })
  });

var vectorSource = new ol.source.Vector({
    format: new ol.format.GeoJSON(),
    url: function(extent) {
      return    wfsUrl+'?service=WFS&version=1.0.0&request=GetFeature&typename=topp:states&outputFormat=application/json';
    },
    strategy: ol.loadingstrategy.bbox
  });

wso2_layers['vectorwfs'] = new ol.layer.Vector({
    source: vectorSource,
    style: new ol.style.Style({
      stroke: new ol.style.Stroke({
        color: 'rgba(0, 0, 255, 1.0)',
        width: 2
      })
    })
  });

wso2_layers['tilewcs'] =   new ol.layer.Tile({
    extent: [-13884991, 2870341, -7455066, 6338219],
        source: new ol.source.TileWMS({
                url:    wcsUrl+'?service=wcs&version=1.1.0&request=GetCapabilities',
                params: {
                        'LAYERS': 'topp:states',
                },
                serverType: 'geoserver',
        })
});

wso2_layers['vector_kml_wms'] = new ol.layer.Vector({
    source: new ol.source.Vector({
        url:    wmsUrl+'?service=WMS&version=1.1.1&request=GetMap&layers=topp:states&format=kml&width=200&height=200&bbox=-124.73142200000001, 24.955967,-66.969849, 49.371735',
        //url:  wfsUrl+'?service=WFS&version=1.0.0&request=GetFeature&typename=topp:states&outputFormat=kml',
        format: new ol.format.KML({
          extractStyles: false
        })
    })
});
