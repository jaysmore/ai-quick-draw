import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.json.simple.parser.ParseException;

public class K_Means {

	private static final int NUM_PICTURES = 1000; // number of pictures used in the learning process
	private static final int FEATURE_LENGTH = 70; // feature length for the image (m)
	private static final int K = 7; // K - number of clusters for the algorithm
	private static int[] cluster_info = new int[NUM_PICTURES]; // image to the cluster assignment information

	private static String meansTxt = "src/means.txt";

	public static void main(String[] args) throws ParseException, IOException {

		Preprocessor p = new Preprocessor("src/full_simplified_pig.ndjson", NUM_PICTURES, FEATURE_LENGTH);
		List<Double[]> images = p.cleanData();

		// randomly generate K vectors of length FEATURE_LENGTH*2
		List<Double[]> means = new ArrayList<Double[]>(); // means for each cluster

		for (int i = 0; i < K; i++) {
			Double[] vector = new Double[FEATURE_LENGTH * 2];
			for (int j = 0; j < FEATURE_LENGTH * 2; j++) {
				Random r = new Random();
				vector[j] = (double) r.nextInt(256);
			}
			means.add(vector);
		}

		// find initial cluster assignment for all images
		findClusterForImages(images, means);

		// start updating means for clusters until clusters do not change
		int iteration = 0;
		boolean keepUpdatingMeans = true;

		List<Double[]> previous_iteration_means = means;

		while (keepUpdatingMeans) {
			iteration += 1;
			System.out.println("Iteration # " + String.valueOf(iteration));

			List<Double[]> recomputed_means = recomputeMeans(images);

			// need to make sure that all K means are not changing for stopping learning
			// algorithm
			int checker = 0;
			for (int cl_index = 0; cl_index < K; cl_index++) {

				if (Arrays.equals(recomputed_means.get(cl_index), previous_iteration_means.get(cl_index))) {
					checker += 1;
				}
			}
			if (checker == K) {
				// all K vectors are still the same after last iteration of updates to the means
				// therefore stop the learning algorithm
				keepUpdatingMeans = false;
				break;
			}
			findClusterForImages(images, recomputed_means);
			previous_iteration_means = recomputed_means;
		}

		writeMeansToOutputFiles(previous_iteration_means);
	}

	/*
	 * This method given input feature vectors for all images, and information about
	 * means for the clusters, assigns each image to the cluster and updates the
	 * array holding information about the assignment. Chooses the cluster with min
	 * Euclidean distance from image vector.
	 */
	private static void findClusterForImages(List<Double[]> images, List<Double[]> means) {

		for (int i = 0; i < images.size(); i++) {
			Double[] image = images.get(i);

			int min_cluster = 0;
			double min_distance = Double.MAX_VALUE;

			for (int k = 0; k < K; k++) {

				Double[] mean_vector = means.get(k);
				double distance = 0;

				for (int j = 0; j < image.length; j++) {
					distance += Math.pow((image[j] - mean_vector[j]), 2);
				}

				distance = Math.sqrt(distance);

				if (distance < min_distance) {
					min_cluster = k + 1;
					min_distance = distance;
				}
			}
			cluster_info[i] = min_cluster;
		}
	}

	/*
	 * This method given all images and a new assignment of images to the clusters,
	 * recomputes the means for clusters by finding the average.
	 */
	private static List<Double[]> recomputeMeans(List<Double[]> images) {

		List<Double[]> means = new ArrayList<Double[]>();

		// consider each cluster
		for (int i = 0; i < K; i++) {

			int cluster_size = 0;
			Double[] cluster_mean = new Double[FEATURE_LENGTH * 2];

			// initialize values to 0, to avoid null pointer
			for (int k = 0; k < cluster_mean.length; k++) {
				cluster_mean[k] = 0.0;
			}

			// for each image
			for (int j = 0; j < cluster_info.length; j++) {

				if (cluster_info[j] == i + 1) {

					cluster_size += 1;

					Double[] image = images.get(j);

					// sum values for all images in the cluster
					for (int index = 0; index < image.length; index++) {
						cluster_mean[index] += image[index];
					}
				}
			}
			System.out.println("Cluster size = " + cluster_size + " for cluster " + String.valueOf(i + 1));

			// average all images in the cluster
			for (int index = 0; index < cluster_mean.length; index++) {
				cluster_mean[index] = cluster_mean[index] / (double) cluster_size;
			}

			means.add(cluster_mean);
		}
		return means;
	}

	/*
	 * This method outputs K vectors of means for the clusters, one mean on a line,
	 * you can input that into the website to see the image for each cluster mean.
	 */
	private static void writeMeansToOutputFiles(List<Double[]> means) throws IOException {

		FileWriter file = new FileWriter(meansTxt);
		PrintWriter wr = new PrintWriter(file);

		for (int i = 0; i < cluster_info.length; i++) {
			wr.println(cluster_info[i]);
		}
		for (int i = 0; i < means.size(); i++) {

			System.out.println();
			String mean_arr = Arrays.toString(means.get(i));
			// wr.println(cluster_info[i]);
			// wr.println(mean_arr.substring(1, mean_arr.length() - 1));

		}
		wr.close();
	}

}
