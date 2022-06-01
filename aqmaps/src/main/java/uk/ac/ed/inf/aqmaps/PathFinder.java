package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PathFinder {
	
	//the starting point
	private final double xorigin, yorigin;
	//current location
	private double lng, lat;
	//number of sensors
	private int number;
	//list of sensor coordinates
	private ArrayList<SensorReader.SensorDetails> sensor_coordinate;
	//list of buildings
	private ArrayList<ArrayList<List<Double>>> buildings;
	//length of a move
	private final double fly_distance = 0.0003;
	//range required to read a sensor
	private final double range = 0.0002;
	//the remaining moves available to be made
	public int moves;
	//unvisited sensors
	public ArrayList<Integer> unvisited;
	//recording for reading a sensor, for the use of recording flight path in a text file
	public HashMap<Integer, Integer> read;
	private int seed;
	
	public PathFinder(String lg, String lt, ArrayList<SensorReader.SensorDetails> coordinates, 
			ArrayList<ArrayList<List<Double>>> b, String s) {
		this.lng = Double.parseDouble(lg);
		this.lat = Double.parseDouble(lt);
		this.sensor_coordinate = coordinates;
		this.buildings = b;
		this.number = coordinates.size();
		this.xorigin = this.lng;
		this.yorigin = this.lat;
		this.unvisited = new ArrayList<Integer>();
		this.read = new HashMap<Integer,Integer>();
		this.seed = Integer.parseInt(s);
	}
	
	//take a seat, drone is starting to fly.
	//return the flight path as a 2d list.
	public ArrayList<ArrayList<Double>> fly() {
		var solver = new NearestNeighbour(this.distance());
		int[] visit_order = solver.rnn(seed, 500);
		this.moves = 150;
		//an array representing the flight path of drone
		var ans = new ArrayList<ArrayList<Double>>();
		//longitude of flight path
		var x = new ArrayList<Double>();
		//latitude of flight path
		var y = new ArrayList<Double>();
		//direction of flight path
		var directions = new ArrayList<Double>();
		ans.add(x);
		ans.add(y);
		ans.add(directions);
		for (int i = 1; i < visit_order.length - 1; i++) {
			//if 150 moves have been made before all the sensors are visited,
			//add the remaining sensors to the list of unvisited sensors
			if (this.moves < 0) {
				for (int j = i; j < visit_order.length - 1; j++) {
					this.unvisited.add(visit_order[j] - 1);
				}
				return ans;
			}
			//visit sensor one by one according the the order calculated by NN+2-opt
			var sensor = this.sensor_coordinate.get(visit_order[i] - 1);
			fly_to(sensor.lng(), sensor.lat(), ans);
			var after = euclidean(this.lng, this.lat, sensor.lng(), sensor.lat());
			//if the sensor can't be reached somehow, add it to the list of unvisited sensors (won't happen, usually)
			if (after > this.range) {
				unvisited.add(visit_order[i] - 1);
			}
			//a record for the text output file
			else {
				read.put(150 - this.moves, visit_order[i] - 1);
			}
		}
		fly_to(this.xorigin, this.yorigin, ans);
		return ans;
	}
	
	//fly to a specific sensor from current location
	//recording the flight path in an array
	private void fly_to(double x, double y, ArrayList<ArrayList<Double>> ans) {
		double angle, newx=this.lng, newy=this.lat;
		int direction;
		ArrayList<Integer> too_closed;
		//fly to the destination directly
		while ("please_give_me_high_score"=="please_give_me_high_score") {
			var newxy = new ArrayList<Double[]>();
			angle = Math.toDegrees(Math.atan2(y - this.lat, x - this.lng));
			if (angle < 0) {
				angle += 360;
			}
			direction = (int) (Math.round(angle/10.0) * 10.0);
			int d = 0;
			//if it will cross no fly zones, change the direction
			while (d < 360) {
				newy = this.lat + (Math.sin(Math.toRadians(direction + d)) * this.fly_distance);
				newx = this.lng + (Math.cos(Math.toRadians(direction + d)) * this.fly_distance);
				too_closed = this.crossing_buildings(this.lng, this.lat, newx, newy);
				if (too_closed.size() == 0) {
					newxy.add(new Double[] {newx, newy, (double) d});
				}
				d += 10;
			}
			//if the drone somehow reaches a dead end.
			if (newxy.size() == 0) return;
			//find the angle that results in the least distance between destination and the upcoming location.
			double min = Double.MAX_VALUE;
			for (int i = 0; i < newxy.size(); i++) {
				var dist = cal_dist(x, y, newxy.get(i)[0], newxy.get(i)[1], 0);
				if (dist < min) {
					min = dist;
					newx = newxy.get(i)[0];
					newy = newxy.get(i)[1];
					angle = direction + newxy.get(i)[2];
				}
			}
			//update the current location
			this.lng = newx;
			this.lat = newy;
			angle = angle >=360 ? angle - 360 : angle;
			//record for the text output file
			ans.get(0).add(this.lng);
			ans.get(1).add(this.lat);
			ans.get(2).add(angle);
			this.moves -= 1;
			if (this.moves < 0) {
				return;
			}
			if (euclidean(this.lng, this.lat, x, y) <= this.range) {			
				return;
			}
		}
	}
	
	//return a table which element (i,j) = distance from sensor i to sensor j
	private double[][] distance() {
		var dist = new double[number+1][number+1];
		for (int i = 0; i < number + 1; i++) {
			for (int j = 0; j < number + 1;j++) {
				double x0;
				double y0;
				double x1;
				double y1;
				//first element is the origin
				if (i == 0) {
					x0 = this.xorigin;
					y0 = this.yorigin;
				}
				else {
					x0 = this.sensor_coordinate.get(i-1).lng();
					y0 = this.sensor_coordinate.get(i-1).lat();
				}
				if (j == 0) {
					x1 = this.xorigin;
					y1 = this.yorigin;
				}
				else {
					x1 = this.sensor_coordinate.get(j-1).lng();
					y1 = this.sensor_coordinate.get(j-1).lat();
				}
				dist[i][j] = cal_dist(x0, y0, x1, y1, 0);
			}
		}
		return dist;
	}
	
	//calculate the distance between point0 and point1, considering the no fly zones
	private double cal_dist(double x0, double y0, double x1, double y1, int iteration) {
		//maximum of 2 turning points are required to avoid a no fly zone
		//otherwise the path won't be the optimal path.
		if (iteration > 2) {
			return 999;
		}
		double ans = 999;
		//find if flying from point0 to point1 directly will cross no fly zones
		var crossed = this.crossing_buildings(x0, y0, x1, y1);
		//if no, take the euclidean distance
		if (crossed.size() == 0) {
			ans = euclidean(x0, y0, x1, y1);
		}
		//otherwise calculate another path, details mentioned in the following methods.
		else {
			var min_max = alternate_path(crossed);
			ans = lowest_cost(min_max, x0, y0, x1, y1, iteration);
		}
		return ans;
	}
	
	//find paths to go around no fly zones
	//treat the no fly zone as a rectangular area by taking the lowest and highest x/y coordinate
	private double[] alternate_path(ArrayList<Integer> crossed) {
		var min_max = new double[] {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		for (int i = 0; i < crossed.size(); i++) {
			var b = crossed.get(i);
			var the_one = this.buildings.get(b);
			for (int j = 0; j < the_one.size(); j++) {
				min_max[0] = Math.min(the_one.get(j).get(0), min_max[0]);
				min_max[1] = Math.max(the_one.get(j).get(0), min_max[1]);
				min_max[2] = Math.min(the_one.get(j).get(1), min_max[2]);
				min_max[3] = Math.max(the_one.get(j).get(1), min_max[3]);
			}
		}
		return min_max;
	}
	
	//find the path with lowest cost when traveling around a no fly zone and return the cost.
	//fly to one of the 4 points calculated in alternate_path()
	//and then fly to the destination from the chosen point
	//repeat until destination is reached
	//return the lowest cost among all the paths
	private double lowest_cost(double[] min_max, double x0, double y0, double x1, double y1, int iteration) {
		var minx = min_max[0];
		var maxx = min_max[1];
		var miny = min_max[2];
		var maxy = min_max[3];
		var path0 = cal_dist(x0, y0, minx, miny, iteration+1) + cal_dist(minx, miny, x1, y1, iteration+1);
		var path1 = cal_dist(x0, y0, minx, maxy, iteration+1) + cal_dist(minx, maxy, x1, y1, iteration+1);
		var path2 = cal_dist(x0, y0, maxx, miny, iteration+1) + cal_dist(maxx, miny, x1, y1, iteration+1);
		var path3 = cal_dist(x0, y0, maxx, maxy, iteration+1) + cal_dist(maxx, maxy, x1, y1, iteration+1);
		var path = Math.min(Math.min(path0, path1), Math.min(path2, path3));
		return path;
	}
	
	//test if the straight line from point0 and point1 crosses the no fly zones and return the building(s).
	private ArrayList<Integer> crossing_buildings(double x0, double y0, double x1, double y1) {
		var ans = new ArrayList<Integer>();
		var number_b = this.buildings.size();
		for (int i = 0; i < number_b; i++) {
			var current = this.buildings.get(i);
			var number_p = current.size();
			for (int j = 0; j < number_p-1; j++) {
				var point0 = current.get(j);
				var point1 = current.get(j+1);
				if (crossed_boundary(x0, y0, x1, y1, point0.get(0), point0.get(1), point1.get(0), point1.get(1))) {
					ans.add(i);
					break;
				}
			}
		}
		return ans;
	}
	
	//test if a straight line crosses the boundary of a building
	//by making multiples linear equation, test if the interception is within the line.
	private static Boolean crossed_boundary(double x0, double y0, double x1, double y1, double bx0, double by0, double bx1, double by1) {
		boolean y_inrange, x_inrange;
		double xintercept, yintercept;
		var boundary = line_equation(bx0, by0, bx1, by1);
		var line = line_equation(x0, y0, x1, y1);
		//1 vertical line
		if (boundary[0] == Double.POSITIVE_INFINITY || boundary[0] == Double.NEGATIVE_INFINITY) {
			xintercept = bx0;
			yintercept = (line[0]*xintercept) + line[1];
			y_inrange = (yintercept < Math.max(by0, by1)) && (yintercept > Math.min(by0, by1))
					&& (yintercept < Math.max(y0, y1)) && (yintercept > Math.min(y0, y1));
			return y_inrange;
		}
		//1 vertical line
		else if (line[0] == Double.POSITIVE_INFINITY || line[0] == Double.NEGATIVE_INFINITY) {
			xintercept = x0;
			yintercept = (boundary[0]*xintercept) + boundary[1];
			y_inrange = (yintercept < Math.max(by0, by1)) && (yintercept > Math.min(by0, by1))
					&& (yintercept < Math.max(y0, y1)) && (yintercept > Math.min(y0, y1));
			return y_inrange;
		}
		//2 vertical lines
		else if ((boundary[0] == Double.POSITIVE_INFINITY || boundary[0] == Double.NEGATIVE_INFINITY) &&
				(line[0] == Double.POSITIVE_INFINITY || line[0] == Double.NEGATIVE_INFINITY)) {
			return false;
		}
		//2 parallel lines
		else if (boundary[0] == line[0]) return false;
		else {
			xintercept = (boundary[1] - line[1])/(line[0] - boundary[0]);
			yintercept = (boundary[0]*xintercept) + boundary[1];
		}
		x_inrange = (xintercept < Math.max(bx0, bx1)) && (xintercept > Math.min(bx0, bx1))
				&& (xintercept < Math.max(x0, x1)) && (xintercept > Math.min(x0, x1));
		//1 horizontal line
		if (boundary[0] == 0 || line[0] == 0) return x_inrange;
		y_inrange = (yintercept < Math.max(by0, by1)) && (yintercept > Math.min(by0, by1))
				&& (yintercept < Math.max(y0, y1)) && (yintercept > Math.min(y0, y1));
		return x_inrange && y_inrange;
	}
	
	//return a linear equation based on 2 points.
	private static double[] line_equation(double x0, double y0, double x1, double y1) {
		var ans = new double[2];
		var gradient = (y1-y0)/(x1-x0);
		var constant = y0 - (gradient*x0);
		ans[0] = gradient;
		ans[1] = constant;
		return ans;
	}
	
	//return the euclidean distance between 2 points
	private static double euclidean(double x0, double y0, double x1, double y1) {
		double ans = Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2);
		return Math.sqrt(ans);
	}
}
