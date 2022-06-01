package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public class MapReader {
	
	//Get a list of Polygon, each representing a building in no fly zone
	private static List<Polygon> nf_buildings(String nf_zones) {
		var features = FeatureCollection.fromJson(nf_zones).features();
		var buildings = new ArrayList<Polygon>();
		var n = features.size();
		for (int i = 0; i < n; i++) {
			var building = (Polygon) features.get(i).geometry();
			buildings.add(building);
		}
		return buildings;
	}
	
	//get the coordinate of building from a Polygon
	private static ArrayList<List<Double>> get_coordinate(Polygon building) {
		var points = building.coordinates();
		var coordinates = new ArrayList<List<Double>>();
		var n = points.get(0).size();
		for (int i = 0; i < n; i++) {
			coordinates.add(points.get(0).get(i).coordinates());
		}
		return coordinates;
	}
	
	//return a 2d list of double which row i contains a list of coordinate for building i
	public static ArrayList<ArrayList<List<Double>>> nf_buildings_coordinates(String port) throws IOException, InterruptedException {
		var nf_zones = DataReceiver.no_fly_zones(port);
		var buildings = nf_buildings(nf_zones);
		var n = buildings.size();
		var buildings_coordinates = new ArrayList<ArrayList<List<Double>>>();
		for (int i = 0; i < n; i++) {
			buildings_coordinates.add(get_coordinate(buildings.get(i)));
		}
		return buildings_coordinates;
	}
}
