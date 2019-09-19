import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * This class processes input images from Quick Draw, 
 * creates vectors in the form (x1,y1,x2,y2,...,xk, yk) for each image,
 * upsamples or downsamples input vectors for each image in order to create 
 * a list of images with the same feature length.
 */

public class Preprocessor {

	private static String filePathName;
	private static int numPictures;     // number of pictures to be processed 
	private static int featureLength;   

	// Constructor for the class
	public Preprocessor(String filePathName, int numPictures, int featureLength){
		Preprocessor.filePathName = filePathName;
		Preprocessor.numPictures = numPictures;
		Preprocessor.featureLength = featureLength;
	}

	/*
	 * This method returns a list of feature vectors for images that were processed.
	 */
	public List<Double[]> cleanData() throws FileNotFoundException, ParseException{

		Scanner scanner = new Scanner(new File(filePathName));
		List<Double[]> processed_images = new ArrayList<Double[]>();

		// consider only part of the images from the input file
		for (int i = 0; i < numPictures; i++){

			if (scanner.hasNextLine()){

				Object obj = new JSONParser().parse(scanner.nextLine());
				JSONObject image = (JSONObject) obj;

				// array containing several strokes 
				JSONArray image_data = (JSONArray) image.get("drawing");

				// concatenate coordinates for all strokes into two arrays for x and y separately
				int number_strokes = image_data.size();
				double[] x_coordinates = {};
				double[] y_coordinates = {};

				for (int j=0; j < number_strokes; j++){
					JSONArray stroke = (JSONArray) image_data.get(j); // get the stroke

					JSONArray stroke_x = (JSONArray) stroke.get(0); // get array with x coordinates of the stroke
					JSONArray stroke_y = (JSONArray) stroke.get(1); // get array with y coordinates of the stroke

					// concatenate all x coordinates and y coordinates for the strokes
					x_coordinates = concatenate(x_coordinates, stroke_x);
					y_coordinates = concatenate(y_coordinates, stroke_y);
				}
				
				Double[] final_feature_vector;
				
				// check if we need to upsample or downsample features or no change is needed
				if (x_coordinates.length > featureLength){
					
					final_feature_vector = downsample(x_coordinates,y_coordinates);
					
				} else if (x_coordinates.length < featureLength){
					
					final_feature_vector = upsample(x_coordinates,y_coordinates);
					
				} else {
					//output values as a single array in the form (X1,Y1,X2,Y2,...,Xm,Ym)
					final_feature_vector = mergeXYVectors(x_coordinates,y_coordinates);
				}
				// store the final feature vector for the image into the list 
				processed_images.add(final_feature_vector);
			}

		}
		scanner.close();
		// return all processed image vectors
		return processed_images;
	}
	
	/*
	 * This method returns merged X and Y vectors in the form (X1,Y1,X2,Y2,...,Xk,Yk).
	 */
	private static Double[] mergeXYVectors(double[] x_coordinates, double[] y_coordinates){
		
		Double[] feature_vector = new Double[2*featureLength];
		int index = 0;
		for (int pos=0; pos < featureLength; pos++){
			feature_vector[index] = x_coordinates[pos];
			index+=1;
			feature_vector[index] = y_coordinates[pos];
			index+=1;
		}
		return feature_vector;
	}

	/*
	 * This method helps to concatenate arrays representing different strokes in the 
	 * input image into a single array. 
	 */
	private static double[] concatenate(double[] coordinates, JSONArray stroke){
		int length = coordinates.length + stroke.size();
		double[] result = new double[length];

		int pos = 0;
		for (double element : coordinates) {
			result[pos] = element;
			pos++;
		}
		for (int i=0;i<stroke.size();i++) {
			result[pos] = Double.valueOf(stroke.get(i).toString()); 
			pos++;
		}
		return result;
	}

	/*
	 * Method accepts two arrays with x and y coordinates for the image, and 
	 * upsamples # of points to M, creating additional new points using linear interpolation.
	 */
	private static Double[] upsample(double[] x_coordinates, double[] y_coordinates){

		int q;
		int t = x_coordinates.length;
		int m;
		boolean downsample = false;
		Double[] final_upsampled_arr;
		
		// check whether q is integer or not 
		if ((featureLength-1)%(t-1) == 0){
			m = featureLength;
			q = (featureLength-1)/(t-1);
		} else {
			m = Math.round((featureLength-1)/(t-1))*(t-1)+1;
			q = (m-1)/(t-1);   
			downsample = true; // need to downsample later to featureLength
		}

		double[] x_new = new double[m];
		double[] y_new = new double[m];
		
		for (int i=1; i<m+1; i++){
		
			int index1 = (int) Math.floor(i/q)-1;
			int index2 = (int) Math.floor(i/q)+1-1;
			
			double x_arg1 = 0, x_arg2 = 0, y_arg1 = 0, y_arg2 = 0;
			
			if (index1>=0 && index1<x_coordinates.length){
				x_arg1 = (1-((i%q)/(double)q))*x_coordinates[index1];
				y_arg1 = (1-((i%q)/(double)q))*y_coordinates[index1];
			}
			if (index2>=0 && index2<x_coordinates.length){
				x_arg2 = ((i%q)/(double)q)*x_coordinates[index2];
				y_arg2 = ((i%q)/(double)q)*y_coordinates[index2];
			}
			x_new[i-1] = x_arg1 + x_arg2;
			y_new[i-1] = y_arg1 + y_arg2;
		}
		
		if (downsample){
			final_upsampled_arr = downsample(x_new,y_new); 
		} else {
			final_upsampled_arr = mergeXYVectors(x_new,y_new);
		}
		return final_upsampled_arr;
	}

	/*
	 * Method accepts two arrays with x and y coordinates for the image, and 
	 * downsamples # of points to M, removing some pairs of (x,y) coordinates.
	 */
	private static Double[] downsample(double[] x_coordinates, double[] y_coordinates){

		// stores indices i that will be chosen for the final feature array
		int[] indices = new int[featureLength];

		for (int i=0; i < featureLength; i++){
			indices[i] = (int) Math.floor(i*x_coordinates.length/featureLength);
		}

		// output final chosen values as a single array in the form (X1,Y1,X2,Y2,...,Xm,Ym)
		Double[] final_downsampled_arr = new Double[2*featureLength];

		int index = 0;
		for (int i=0; i < featureLength; i++){
			final_downsampled_arr[index] = x_coordinates[indices[i]];
			index += 1;
			final_downsampled_arr[index] = y_coordinates[indices[i]];
			index += 1;
		}
	
		return final_downsampled_arr;
	}
}
