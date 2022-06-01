package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class App 
{
	private static String[] colour = new String[256];
	
	//helper function to produce grid feature.
	private static String gridArea(int n, double lng, double lat, double lat_size, double lng_size) {
		
		String grid =	"{\n"
				+ 			"\"type\":\"Feature\",\n"
				+ 			"\"properties\": {\n"
				+ 				"\"fill-opacity\": 0.75,\n"
				+ 				"\"fill\":\"" + colour[n] + "\",\n"
				+ 				"\"rgb-string\":\"" + colour[n] + "\"\n"
				+ 			"},\n"
				+ 			"\"geometry\":{\n"
				+ 				"\"type\":\"Polygon\",\n"
				+ 				"\"coordinates\":[[\n"
				+ 					"[" + lng + "," + lat + "],\n"
				+ 					"[" + (lng + lng_size) + "," + lat + "],\n"
				+ 					"[" + (lng + lng_size) + "," + (lat - lat_size) + "],\n"
				+ 					"[" + lng + "," + (lat - lat_size) + "],\n"
				+ 					"[" + lng + "," + lat + "]\n"
				+ 		"]]}}";
				
		return grid;
	}
	
    public static void main( String[] args )
    {
    	//if no file is given, exit
        if (args.length == 0) {
        	System.out.println("No file.");
        	System.exit(1);
        }
        
        //Read file and if file given is invalid, exit
        Scanner input = null;
        try {
        	File file = new File(args[0]);
        	input = new Scanner(file);
        }	catch (IOException e) {
        	System.err.println("Invalid file.");
        	System.exit(1);
        }
        
        //read the file as string and make sure that "," exists between every integer to split the integer using String.split method.
        String temp0 = "";
		while (input.hasNext()) {
			String temp1 = input.next();
			if (temp1.charAt(temp1.length() - 1) != ',') {
				temp1 += ',';
			}
			temp0 += temp1;
        }
		//saving the predictions in an array.
		String[] prediction = temp0.split(",");
		
		//Save marker colours in an array, ignoring low battery and not visited
		String[] colour_string = new String[8];
		colour_string[0] = "#00ff00";
		colour_string[1] = "#40ff00";
		colour_string[2] = "#80ff00";
		colour_string[3] = "#c0ff00";
		colour_string[4] = "#ffc000";
		colour_string[5] = "#ff8000";
		colour_string[6] = "#ff4000";
		colour_string[7] = "#ff0000";
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 32; j++) {
				colour[32*i + j] = colour_string[i];
			}
		}
		
		//the confined area
		String map =	"{ \"type\": \"FeatureCollection\",\n"
				+ 			"\"features\": [\n"
				+ 				"{ \"type\": \"Feature\",\n"
				+ 				"\"properties\": { },"
				+ 				"\"geometry\": {\n"
				+ 					"\"type\": \"LineString\",\n"
				+ 					"\"coordinates\": [\n"
				+ 					"[-3.192473, 55.946233],\n"
				+ 					"[-3.184319, 55.946233],\n"
				+ 					"[-3.184319, 55.942617],\n"
				+ 					"[-3.192473, 55.942617],\n"
				+ 					"[-3.192473, 55.946233]]}},\n";
		
		//Making the grid starting from northwest part.
		Double[] startingPoint = new Double[2];
		startingPoint[0] = -3.192473;
		startingPoint[1] = 55.946233;
		
		//size of each small grids.
		double lat_size = 0.0003616;
		double lng_size = 0.0008154;
		
		//looping to make 100 small grids.
		for (int i = 0; i < prediction.length; i++) {
			if (i != 0 && i % 10 == 0) {
				startingPoint[0] = -3.192473;
				startingPoint[1] -= lat_size;
			}
			map += gridArea(Integer.parseInt(prediction[i]), startingPoint[0], startingPoint[1], lat_size, lng_size);
			map += ",\n";
			startingPoint[0] += lng_size;
		}
		map = map.substring(0, map.length() - 2);
		map += "]}";
		
		//Output heatmap.geojson
		try {
			FileWriter myWriter = new FileWriter("heatmap.geojson");
		    myWriter.write(map);
		    myWriter.close();
		}	catch (IOException e) {
		    System.out.println("Oops, something went wrong.");
		    e.printStackTrace();
		}
    }
}
