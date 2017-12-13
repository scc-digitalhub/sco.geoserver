package sco.scenario;

public class App {

    public static void main(String[] args) {

        String[] types = {"walk", "bus", "train", "bike", "planned"};
        for (int i = 0; i < types.length; i++) {
            Importer.convertToGeoJSON(types[i]);
            Importer.convertGeoJSONToShapeFile(types[i]);
        }
    }
}
