
var types=['all','bike','bus','train','walk','planned'];
var selectedType='all';

function enableAuthToken (name){
  var chkbWso2 = (document.getElementById(name) ? document.getElementById(name).checked : false);
  var layer = (chkbWso2 ? wso2_layers[name] : layers[name]);
  map[name].getLayers().setAt(1, layer);
} 

function loadData(type){
  selectedType=type;
  var layername='playgo_'+type;
  var name='imagewms';
  map[name].getLayers().setAt(1, layers[type+'_hm']);
  for(var k=0;k<types.length;k++){
    document.getElementById('lbl_'+types[k]).style.display='none';
    document.getElementById(type+'_chkb').checked=false;
  }
  document.getElementById('lbl_'+type).style.display='block';
  loadInfo(type);
}

function loadInfo(type){
    var name='imagewms';
    if(document.getElementById(type+'_chkb').checked==true){
      map[name].getLayers().getArray()[2].setSource(sources[type]);
    }
    else
      map[name].getLayers().getArray()[2].setSource(null);
  }

function loadFilter(value){
    var name='imagewms';
    sources[selectedType].updateParams({'CQL_FILTER':"validity = '"+value+"'"});
    map[name].getLayers().getArray()[1].getSource().updateParams({'CQL_FILTER':"validity = '"+value+"'"});
}

var style = new ol.style.Style({
  text: new ol.style.Text({
    font: 'bold 11px "Open Sans", "Arial Unicode MS", "sans-serif"',
    placement: 'line',
    fill: new ol.style.Fill({
      color: 'white'
    })
  })
});

//sources
var sources={};
var layers={};

for(var k=0;k<types.length;k++){
    sources[types[k]] =  new ol.source.Vector({
        url: geoserverUrl+'/wms?service=WMS&version=1.0.0&request=GetMap&layers=topp:playgo_'+types[k]+'&format=kml&width=200&height=200&bbox=10.9306194, 40.35092374930597,18.16706709858563, 46.496204',
        //params: {'LAYERS': 'topp:playgo_'+types[k]},
        //ratio: 1,
        //serverType: 'geoserver',
        format: new ol.format.KML()
    });
    layers[types[k]] =     new ol.layer.Vector({
      source:sources[types[k]]
    });
    layers[types[k]+'_hmOld'] =     new ol.layer.Vector({
        source: new ol.source.Vector({
                //url: geoserverUrl+'/wms',
                url:    geoserverUrl+'/wms?service=WMS&version=1.0.0&request=GetMap&layers=topp:playgo_all&format=kml&styles=play_ti&srs=EPSG:4326&width=200&height=200&bbox=10.9306194, 40.35092374930597,18.16706709858563, 46.496204',
                //params: {'LAYERS': 'topp:playgo_'+types[k],'Styles':'topp:play_ti'},
                //ratio: 1,
                //serverType: 'geoserver',
                format: new ol.format.KML()
        })
    });
    layers[types[k]+'_hmOther'] = new ol.layer.Heatmap({
      radius:2,
      blur:10,
      opacity:0.5,
      weight:'the_geom',
      gradient:['#FFFFFF', '#4444FF', '#FF0000', '#FFFF00'],
      source: new ol.source.Vector({
              //url: geoserverUrl+'/wms',
              url:    geoserverUrl+'/wfs?service=WFS&version=1.0.0&request=GetFeature&typename=topp:playgo_'+types[k]+'&outputFormat=application/json',
              //params: {'LAYERS': 'topp:playgo_'+types[k],'Styles':'topp:play_ti'},
              //ratio: 1,
              //serverType: 'geoserver',
              format: new ol.format.GeoJSON()
      })
  });

  layers[types[k]+'_hm'] = new ol.layer.Heatmap({
    radius:2,
    blur:10,
    opacity:0.5,
    weight:'the_geom',
    gradient:['#FFFFFF', '#4444FF', '#FF0000', '#FFFF00'],
    source: new ol.source.Vector({
            //url: geoserverUrl+'/wms',
            url:    geoserverUrl+'/wms?service=WMS&version=1.0.0&request=GetMap&layers=topp:playgo_all&format=kml&width=200&height=200&bbox=10.9306194, 40.35092374930597,18.16706709858563, 46.496204',
            //params: {'LAYERS': 'topp:playgo_'+types[k],'Styles':'topp:play_ti'},
            //ratio: 1,
            //serverType: 'geoserver',
            format: new ol.format.KML()
    })
});
}
layers['all_hm'].getSource().on('addfeature', function(event) {
  console.log(event.feature.get('the_geom'));
  //var name = event.feature.get('name');
  //var magnitude = parseFloat(name.substr(2));
  //event.feature.set('weight', magnitude - 5);
});

layers['tilewcs'] =   new ol.layer.Tile({
  source: new ol.source.TileWMS({
          url: geoserverUrl+'/wcs?service=wcs&version=1.1.0&request=GetCapabilities',
          params: {
                  'LAYERS': 'medford:bikelanes,medford:bikelanes_original',
          },
          serverType: 'geoserver',
  })
});
var vectorSource =   new ol.source.Vector({
  format: new ol.format.GeoJSON(),
  url: function(extent) {
          return geoserverUrl+'/wfs?service=WFS&version=1.0.0&request=GetFeature&typename=topp:OGRGeoJSON&outputFormat=application/json';
  },
  strategy: ol.loadingstrategy.bbox
});
layers['play_style'] = new ol.layer.Vector({
  source: vectorSource,
  //style: function(feature) {
  //  style.getText().setText(feature.get('freeTracki'));
  //  return style;
  //}
});

// base layers
var tile = new ol.layer.Tile({
  source: new ol.source.OSM(),
  name: 'OpenStreetMap'
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
var transform=ol.proj.transform([10.887451171875,45.91389958711688], 'EPSG:4326', 'EPSG:3857');
var zoom = 10;

var layers_geo_wms =            [tile,layers['all_hm'],layers['all']];
var map={};
map['imagewms'] = new ol.Map({
  controls: controls,
  layers: layers_geo_wms,
  target: "map['imagewms']",
  view: new ol.View({
          center: transform,
          zoom:zoom,
  })
});
loadData('all');
document.getElementById('all').checked=true;
