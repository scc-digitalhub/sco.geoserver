function enableAuthToken (name){
  var chkbWso2 = (document.getElementById(name) ? document.getElementById(name).checked : false);
  var layer = (chkbWso2 ? wso2_layers[name] : layers[name]);
  map[name].getLayers().setAt(1, layer);
}     

var layers={};
// A WMS Geoserver layer
layers['imagewms'] =     new ol.layer.Image({
  //extent: [-13884991, 2870341, -7455066, 6338219],
  source: new ol.source.ImageWMS({
          url: geoserverUrl+'/wms',
          params: {'LAYERS': 'topp:states'},
          ratio: 1,
          serverType: 'geoserver'
  })
});
layers['tilewcs'] =   new ol.layer.Tile({
  //extent: [-13884991, 2870341, -7455066, 6338219],
  source: new ol.source.TileWMS({
          url: geoserverUrl+'/wcs?service=wcs&version=1.1.0&request=GetCapabilities',
          params: {
                  'LAYERS': 'topp:states',
          },
          serverType: 'geoserver',
  })
});
var vectorSource =   new ol.source.Vector({
  format: new ol.format.GeoJSON(),
  url: function(extent) {
          return geoserverUrl+'/wfs?service=WFS&version=1.0.0&request=GetFeature&typename=topp:states&outputFormat=application/json';
  },
  strategy: ol.loadingstrategy.bbox
});
var vectorSource_csw =   new ol.source.Vector({
  format: new ol.format.GeoJSON(),
  url: function(extent) {
          return geoserverUrl+'/csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd';
  },
  strategy: ol.loadingstrategy.bbox
});
layers['vectorwfs'] = new ol.layer.Vector({
  source: vectorSource,
  style: new ol.style.Style({
          stroke: new ol.style.Stroke({
                  color: 'rgba(0, 0, 255, 1.0)',
                  width: 2
          })
  })
});
// base layers
var tile = new ol.layer.Tile({
  source: new ol.source.OSM(),
  name: 'OpenStreetMap'
});
var layerStamenWater = new ol.layer.Tile({
  source: new ol.source.Stamen({
          layer: 'watercolor'
  }),
  name: 'Watercolor'
});
var layerStamenTerrain = new ol.layer.Tile({
  source: new ol.source.Stamen({
          layer: 'terrain'
  }),
  name: 'Terrain'
});
var layerStm = new ol.layer.Group({
  layers: [layerStamenWater, layerStamenTerrain],
  name: 'Stamen Group'
});
var raster = new ol.layer.Tile({
  source: new ol.source.Stamen({
          layer: 'toner'
  })
});

layers['vector_kml_wms'] = new ol.layer.Vector({
  source: new ol.source.Vector({
          url:    geoserverUrl+'/wms?service=WMS&version=1.0.0&request=GetMap&layers=topp:states&format=kml&width=200&height=200&bbox=-124.73142200000001, 24.955967,-66.969849, 49.371735',
          //url:  geoserverUrl+'/wfs?service=WFS&version=1.0.0&request=GetFeature&typename=topp:states&outputFormat=kml',
          format: new ol.format.KML({
                  extractStyles: false
          })
  })
});


var mousePositionControl = new ol.control.MousePosition({
  coordinateFormat: ol.coordinate.createStringXY(4),
  undefinedHTML: '&nbsp;'
});
var scaleLineControl = new ol.control.ScaleLine();
var zoomslider = new ol.control.ZoomSlider();
var controls = ol.control.defaults().extend([
          mousePositionControl,
          scaleLineControl,
          zoomslider
]);
var transform=ol.proj.transform([-104.15655, 35.74222], 'EPSG:4326', 'EPSG:3857');

var layers_geo_wms =            [layerStamenWater,layers['imagewms']];
var layers_geo_wfs =            [tile,layers['vectorwfs']];
var layers_geo_wcs =            [tile,layers['tilewcs']];
var layers_geo_wms_kml =        [tile,layers['vector_kml_wms']];

var map={};
map['imagewms'] = new ol.Map({
  controls: controls,
  layers: layers_geo_wms,
  target: "map['imagewms']",
  view: new ol.View({
          center: transform,
          zoom:4,
  })
});

map['tilewcs'] = new ol.Map({
  controls: controls,
  layers: layers_geo_wcs,
  target: "map['tilewcs']",
  view: new ol.View({
          center: transform,
          zoom:4,
  })
});

map['vectorwfs'] = new ol.Map({
  controls: controls,
  layers: layers_geo_wfs,
  target: "map['vectorwfs']",
  view: new ol.View({
          center: transform,
          zoom:4,
  })
});

map['vector_kml_wms'] = new ol.Map({
  controls: controls,
  layers: layers_geo_wms_kml,
  target: "map['vector_kml_wms']",
  view: new ol.View({
          center: transform,
          zoom:4,
  })
});