package uk.ac.ed.inf.aqmaps;

import java.util.Random;

public class NearestNeighbour {
	
	private double[][] distance;
	private int n;
	
	public NearestNeighbour(double[][] d) {
		this.distance = d;
		this.n = this.distance.length;
	}
	
	//nearest neighbor algorithm with 2-opt to calculate the order to visit sensors, an approximation algorithm
	//won't give the optimal solution usually but fast and efficient
	public int[] nn() {
		var visited = new int[n];
		var ans = new int[n+1];
		for (int i = 0; i < n; i++) {
			visited[i] = 0;
		}
		visited[0] = 1;
		for (int i = 0; i < n+1; i++) {
			ans[i] = i;
		}
		ans[0] = 0;
		ans[n] = 0;
		for (int i = 1; i < n; i++) {
			var min = Double.MAX_VALUE;
			for (int j = 1; j < this.n; j++) {
				if (visited[j] == 1) continue;
				if (distance[ans[i-1]][j] < min) {
					min = distance[ans[i-1]][j];
					ans[i] = j;
				}
			}
			visited[ans[i]] = 1;
		}
		var better = twoOpt(ans);
		return better;
	}
	
	//randomized nearest neighbor algorithm with 2-opt to calculate the order to visit sensors, an approximation algorithm
	//better than nn if iteration > 50
	//return nn if nn is found to be better
	//if iteration == infinity, rnn is always better than nn
	public int[] rnn(int seed, int iteration) {
		var visited = new int[n];
		var ans = new int[n+1];
		for (int i = 0; i < n+1; i++) {
			ans[i] = i;
		}
		ans[0] = 0;
		ans[n] = 0;	
		//an array of size 3 that contains the distance of closest neighbor, 2nd closest neighbor and 3rd closest neighbor
		var c_d = new double[3];
		//an array of size 3 that contains the number of closest neighbor, 2nd closest neighbor and 3rd closest neighbor
		var c_i = new int[3];
		var rand = new Random(seed);	
		var order = ans;
		while (iteration>0) {
			for (int i = 0; i < n; i++) {
				visited[i] = 0;
			}
			visited[0] = 1;
			var original = cal(order);
			for (int i = 1; i < n-2; i++) {		
				c_d[0] = Double.MAX_VALUE;
				c_d[1] = Double.MAX_VALUE;
				c_d[2] = Double.MAX_VALUE;
				for (int j = 1; j < n; j++) {
					if (visited[j] == 1) continue;
					//closest neighbor
					if (distance[ans[i-1]][j] < c_d[0]) {
						c_d[2] = c_d[1];
						c_i[2] = c_i[1];
						c_d[1] = c_d[0];
						c_i[1] = c_i[0];
						c_d[0] = distance[ans[i-1]][j];
						c_i[0] = j;
					}
					//2nd closest neighbor
					else if (distance[ans[i-1]][j] < c_d[1]) {
						c_d[2] = c_d[1];
						c_i[2] = c_i[1];
						c_d[1] = distance[ans[i-1]][j];
						c_i[1] = j;
					}
					//3rd closest neighbor
					else if (distance[ans[i-1]][j] < c_d[2]) {
						c_d[2] = distance[ans[i-1]][j];
						c_i[2] = j;
					}
					var random = rand.nextInt(3);
					ans[i] = c_i[random];
				}
				visited[ans[i]] = 1;
			}
			int k = 0;
			var unvisited = new int[2];
			for (int i = 0; i < visited.length; i++) {
				if (visited[i] == 1) continue;
				unvisited[k] = i;
				k++;
				if (k==2) break;
			}
			//last 2 sensors
			var random = rand.nextInt(2);
			ans[n-2] = unvisited[random];
			random = random==0 ? 1 : 0;
			ans[n-1] = unvisited[random];
			var better = twoOpt(ans);
			var better_d = cal(better);
			if (better_d < original) order = better;
			else if (better_d == original) break;
			iteration--;
		}
		var no_rand = this.nn();
		if (cal(no_rand) < cal(order)) {
			return no_rand;
		}
		return order;
	}
	
	//2-opt algorithm to further optimize the solution given by NN
	private int[] twoOpt(int[] ans) {
		var n = ans.length;
		var original = cal(ans);
		var new_dist = original;
		var improved = new int[n];
		for (int i = 0; i < n; i++) {
			improved[i] = ans[i];
		}
		while (true) {
			original = new_dist;
			for (int i = 1; i < n-1; i++) {
				for (int j = 1; j < n-1; j++) {
					if (i == j) continue;
					var temp = improved[i];
					improved[i] = improved[j];
					improved[j] = temp;
					var d = cal(improved);
					if (d < new_dist) {
						new_dist = d;
					}
					else {
						improved[j] = improved[i];
						improved[i] = temp;
					}
				}
			}
			if (new_dist == original) break;
		}
		return improved;
	}
	
	//helper function to calculate the cost of current order
	private double cal(int[] ans) {
		var n = ans.length;
		double dist = 0;
		for (int i = 0; i < n-1; i++) {
			dist += distance[ans[i]][ans[i+1]];
		}
		return dist;
	}

}
