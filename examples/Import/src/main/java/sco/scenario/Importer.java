package sco.scenario;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Importer {

    private static final String path = "/home/albana/Desktop/www/";
    private static final String urlGeoserver = "http://localhost:8080/geoserver/";
    private static final String urlGeoserverImportRest = "rest/imports";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static OkHttpClient client;
    static {
        client = new OkHttpClient();
    }


    public static void convertToGeoJSON(String type) {

        System.out.println("Name: " + type);
        JSONParser parser = new JSONParser();
        JSONArray object = null;
        String path = Importer.path;
        String sourceFile = "ti1.json";
        String geoJSONFile = type + ".geojson";

        try {
            try {
                System.out.println(path);
                object = (JSONArray) parser.parse(new FileReader(path + sourceFile));
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
            JSONArray fileObject = object;
            JSONArray features = new JSONArray();
            JSONObject obj = new JSONObject();
            JSONObject featuresType = new JSONObject();
            obj.put("type", "FeatureCollection");
            Integer i = 0;

            for (Object job : fileObject) {
                JSONObject jobj = (JSONObject) job;
                JSONArray jarr = (JSONArray) jobj.get("geolocationEvents");
                String nameTracking = (String) jobj.get("freeTrackingTransport");
                if (nameTracking == null) {
                    nameTracking = "planned";
                }
                String validity = (String) jobj.get("changedValidity");
                System.out.println("Name: " + nameTracking);
                if (!nameTracking.equals(type))
                    continue;

                JSONObject objProperties = new JSONObject();
                for (Object record : jarr) {
                    JSONObject record2 = (JSONObject) record;
                    objProperties.put("freeTrackingTransport", nameTracking);
                    objProperties.put("validity", validity);
                    JSONArray coordinates = new JSONArray();
                    coordinates.add(record2.get("longitude"));
                    coordinates.add(record2.get("latitude"));
                    JSONObject objGeom = new JSONObject();
                    objGeom.put("coordinates", coordinates);
                    objGeom.put("type", "Point");
                    objGeom.put("geometry_name", "the_geom");
                    JSONObject objCurr = new JSONObject();
                    objCurr.put("type", "Feature");
                    objCurr.put("geometry", objGeom);
                    objCurr.put("properties", objProperties);

                    features.add(objCurr);
                }
                obj.put("features", features);
                i++;
                // if (i == 5)
                // break;// FIX THIS: temporal solution to easy tests due to large amount of data
            }

            try (FileWriter file = new FileWriter(path + geoJSONFile)) {
                file.write(obj.toJSONString());
                System.out.println("\nJSON Object: " + obj);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void convertGeoJSONToShapeFile(String type) {

        String shapeFile = "playgo_" + type;
        String shapeFilePath = Importer.path + "playgo_" + type;
        String geoJSONFile = Importer.path + type + ".geojson";
        String s = null;

        try {
            String[] commands = {"ogr2ogr", "-nln", shapeFile, "-f", "ESRI Shapefile",
                    shapeFilePath, geoJSONFile};
            Process p = Runtime.getRuntime().exec(commands);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void importToGeoServer(String type) {

        String json = "{" + "   \"import\": {" + "      \"targetWorkspace\": {"
                + "         \"workspace\": {" + "            \"name\": \"topp\"" + "         }"
                + "      }," + "      \"data\": {" + "        \"type\": \"directory\","
                + "        \"location\": \"/home/albana/Desktop/www/playgo_" + type + "\""
                + "      }" + "   }" + "}";

        try {
            RequestBody body = RequestBody.create(JSON, json);
            Request request =
                    new Request.Builder().header("Authorization", "Basic YWRtaW46Z2Vvc2VydmVy")
                            .url(urlGeoserver + urlGeoserverImportRest).post(body).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("problem on post: ");
                }
                // String outString = response.body().string();
                response.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
