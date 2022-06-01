package uk.ac.ed.inf.aqmaps;

import java.io.IOException;


public class App 
{	
	//seed is useless
	private static String date, month, year, latitude, longitude, seed, port;
	
    public static void main(String[] args) throws IOException, InterruptedException
    {
        if (args.length == 7) {
        	date = args[0];
        	month = args[1];
        	year = args[2];
        	latitude = args[3];
        	longitude = args[4];
        	seed = args[5];
        	port = args[6];
        }
        //if argument given is invalid, exit the program
        else {
        	System.out.println("Invalid argument.");
        	System.exit(1);
        }
        //list of sensor battery, sensor reading, sensor location
        var sensor_list = SensorReader.read_aq_data(date, month, year, port);
        //list of sensor coordinate   
        var sensors_coordinate = SensorReader.read_coordinate(sensor_list, port);     
        //list of coordinates of buildings
        var buildings = MapReader.nf_buildings_coordinates(port);
        //Class of the drone
        var drone = new PathFinder(longitude, latitude, sensors_coordinate, buildings, seed); 
        //the path of drone after visiting the sensors
        var flight_path = drone.fly();
        //generate the GeoJson map
        var map = Output.generate_map(sensor_list, sensors_coordinate, drone.unvisited);
        //draw the path of drone
        var new_map = Output.draw_path(map, flight_path, longitude, latitude);
        //output the GeoJson map with flight path
        Output.write_map(new_map, date, month, year);
        //output a text file recording the flight path
        Output.txt_file(flight_path, date, month, year, longitude, latitude, drone.read, sensor_list);
        
        System.out.print("Drone's moves is in range:");
        System.out.println(drone.moves<=150);
    }
}
