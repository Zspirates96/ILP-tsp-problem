package uk.ac.ed.inf.aqmaps;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class DataReceiver {
	
	private static final HttpClient client = HttpClient.newHttpClient();
	
	//helper function to get file from webserver
	private static String get(String urlString) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
        		.uri(URI.create(urlString))
        		.build();
		var response = client.send(request, BodyHandlers.ofString());
		return response.body();
	}
	
	//get no_fly_zone in GeoJson format
	public static String no_fly_zones(String port) throws IOException, InterruptedException {
		var urlString = "http://localhost:" + port + "/buildings/no-fly-zones.geojson";
		return get(urlString);
	}
	
	//get air quality in Json format
	public static String air_quality(String date, String month, String year, String port) throws IOException, InterruptedException {
		var urlString = "http://localhost:" + port + "/maps/" + year +"/"+month+"/"+date+"/air-quality-data.json";
		return get(urlString);
	}
	
	//get details of sensors in Json format
	public static String sensor_detail(String location, String port) throws IOException, InterruptedException {
		var words = location.split("\\.");
		var urlString = "http://localhost:" + port + "/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json";
		return get(urlString);
	}

}
