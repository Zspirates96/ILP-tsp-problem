package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.reflect.TypeToken;

public class SensorReader {
	
	//class constructor for retrieving sensor data
	public class Sensor {
		String location;
		double battery;
		String reading;		
	}
	//class constructor for retrieving sensor details
	public class SensorDetails {
		Coordinates coordinates;
		public class Coordinates {
			double lng;
			double lat;
		}
		//helper method to get longitude
		public double lng() {
			return this.coordinates.lng;
		}
		//helper method to get latitude
		public double lat() {
			return this.coordinates.lat;
		}
	}
	
	//convert the sensors in Json format to a list of Sensor class
	public static ArrayList<Sensor> read_aq_data(String date, String month, String year, String port) throws IOException, InterruptedException {//convert the string file to a list of sensor
		var aq_data = DataReceiver.air_quality(date, month, year, port);
		Type listType = new TypeToken<ArrayList<Sensor>>() {}.getType();
		ArrayList<Sensor> sensorList = new Gson().fromJson(aq_data, listType);
		return sensorList;
	}
	
	//get a list of SensorDetails class containing the details of sensor.
	public static ArrayList<SensorDetails> read_coordinate(ArrayList<Sensor> sensor_list, String port) throws IOException, InterruptedException {//convert the string file to a list of SensorDetails
		var n = sensor_list.size();
		var sensor_coordinate = new ArrayList<SensorDetails>();
		for (int i = 0; i < n; i++) {
        	var details = DataReceiver.sensor_detail(sensor_list.get(i).location, port);
        	sensor_coordinate.add(read_sensor_details(details));
        }
		return sensor_coordinate;
	}
	
	//convert details of sensors to SensorDetails class
	private static SensorDetails read_sensor_details(String sensor_word) {
		var details = new Gson().fromJson(sensor_word, SensorDetails.class);
		return details;
	}

}
