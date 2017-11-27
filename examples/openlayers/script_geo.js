function enableAuthToken (name){
  var chkbWso2 = (document.getElementById(name) ? document.getElementById(name).checked : false);
  var layer = (chkbWso2 ? wso2_layers[name] : layers[name]);
  map[name].getLayers().setAt(1, layer);
} 

function describeProcessWps (name){

    document.getElementById('tbl_'+name).innerHTML='';
    var xmlHttp = new XMLHttpRequest();
    var chkbWso2 = (document.getElementById(name) ? document.getElementById(name).checked : false);
    var url=(chkbWso2 ? wpsUrl : geoserverUrl+'/wps');
    xmlHttp.open("GET", url+'?service=WPS&version=1.0.0&request=DescribeProcess&identifier=gs:Aggregate');
    if(chkbWso2){
      xmlHttp.setRequestHeader('Authorization', auth_token_wso2);
    }
    xmlHttp.onload = function (oEvent) {
      var result = xmlHttp.response;
      parser = new DOMParser();
      xmlDoc = parser.parseFromString(result, "text/xml");
      
          document.getElementById('type_'+name).innerHTML=xmlDoc.getElementsByTagName("ows:Identifier")[0].childNodes[0].nodeValue;
          var inputParam = xmlDoc.getElementsByTagName("Input");
          var tblContent='<table border="1">';
          tblContent+='<th>Input Type</th><th>Description</th>';
          for(var i=0;i<inputParam.length;i++){
            tblContent+='<tr><td>';
            tblContent+=inputParam[i].childNodes[0].childNodes[0].nodeValue;
            tblContent+='</td><td>';
            tblContent+=inputParam[i].childNodes[2].childNodes[0].nodeValue;
            tblContent+='</td></tr>';
          }
          tblContent+='</table>';
          document.getElementById('tbl_'+name).innerHTML=tblContent;
  };
    xmlHttp.send(null);
} 
describeProcessWps('desc_proc_wps');

function executeWps (name){

      document.getElementById('tbl_'+name).innerHTML='';
      var xmlHttp = new XMLHttpRequest();
      var chkbWso2 = (document.getElementById(name) ? document.getElementById(name).checked : false);
      var url=(chkbWso2 ? wpsUrlExecute : geoserverUrl+'/wps');
      xmlHttp.open("POST", url);
      if(chkbWso2){
        xmlHttp.setRequestHeader('Authorization', auth_token_wso2);
      }
      var data='<?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">'
      +'<ows:Identifier>gs:Aggregate</ows:Identifier>'
      +' <wps:DataInputs>'
      +' <wps:Input>'
      +'   <ows:Identifier>features</ows:Identifier>'
      +' <wps:Reference mimeType="text/xml" xlink:href="http://geoserver/wfs" method="POST">'
      +'   <wps:Body>'
      +'    <wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:sf="http://www.openplans.org/spearfish">'
      +'      <wfs:Query typeName="topp:states"/>'
      +'    </wfs:GetFeature>'
      +'   </wps:Body>'
      +' </wps:Reference>'
      +' </wps:Input>'
      +' <wps:Input>'
      +'   <ows:Identifier>aggregationAttribute</ows:Identifier>'
      +'    <wps:Data>'
      +'      <wps:LiteralData>PERSONS</wps:LiteralData>'
      +'    </wps:Data>'
      +'  </wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>function</ows:Identifier>'
      +'   <wps:Data>'
      +'    <wps:LiteralData>Count</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>function</ows:Identifier>'
      +'  <wps:Data>'
      +'    <wps:LiteralData>Average</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>function</ows:Identifier>'
      +'  <wps:Data>'
      +'    <wps:LiteralData>Sum</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>function</ows:Identifier>'
      +'  <wps:Data>'
      +'    <wps:LiteralData>Min</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>function</ows:Identifier>'
      +'  <wps:Data>'
      +'    <wps:LiteralData>Max</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'<wps:Input>'
      +'  <ows:Identifier>singlePass</ows:Identifier>'
      +'  <wps:Data>'
      +'    <wps:LiteralData>false</wps:LiteralData>'
      +'  </wps:Data>'
      +'</wps:Input>'
      +'</wps:DataInputs>'
      +'<wps:ResponseForm>'
      +'<wps:RawDataOutput mimeType="application/json">'
      +'   <ows:Identifier>result</ows:Identifier>'
      +' </wps:RawDataOutput>'
      +' </wps:ResponseForm>'
      +'</wps:Execute>';
      xmlHttp.onload = function (oEvent) {
        var result = JSON.parse(xmlHttp.response);
        var inputParam = result['AggregationFunctions'];
        var values = result['AggregationResults'];
        var tblContent='<table border="1" width="100%">';
        tblContent+='<th>Aggregate</th><th>Value</th>';
        for(var i=0;i<inputParam.length;i++){
          tblContent+='<tr><td>';
          tblContent+=inputParam[i];
          tblContent+='</td><td>';
          tblContent+=values[0][i];
          tblContent+='</td></tr>';
        }
        tblContent+='</table>';
        document.getElementById('tbl_'+name).innerHTML=tblContent;
    };
      xmlHttp.send(data);
} 
executeWps('wps_execute');

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