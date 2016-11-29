package javaWeka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileMaker {

	// Is activated by default to delete the arff file when starting the execution
	private static boolean redoFile = true;
	
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
	
	// Method to try to open a file, returns the path if found
	private static String getFilePath(String path) {
		
		File file;
		int i;
		String filePath = null;
		
		for(i = 1; i < 11; i++) {
			
			filePath = new String(path);
			filePath = filePath.concat(Integer.toString(i)); // TODO Apparently, this number represents the score given in the review
			filePath = filePath.concat(".txt");
			
			file = new File(filePath);
			
			if(file.exists())
				break;
		}
		
		if(i == 5) {
			System.err.println("File not found: " + filePath);
			System.exit(1);
		}
		
		return filePath;
	}
 
	// Method to write on our arff file
	private static void writeOnFile(String line) {
		
		File file = new File("movieReviews.arff");
		PrintWriter pw;
		
		try {
			
			if(redoFile && file.exists()) {
				//while(!file.delete()){}
				file.delete();
				redoFile = false;
			}
			
			// In case it doesn't exist
			if(!file.exists())
				file.createNewFile();
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			// In case file is empty (to avoid extra line jump)
			if(reader.readLine() == null)
				pw = new PrintWriter(file);
			else {
				pw = new PrintWriter(new FileWriter(file, true));
				pw.print("\r\n");
			}
			
			pw.print(line);
			reader.close();
			pw.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		String firstDirectory = new String("movie_review_dataset/part1/");
		String secondDirectory = new String("neg/");
		BufferedReader dataFile = null;
		
		writeOnFile("@relation \"Movie Reviews\"\n\n@attribute phrase string\n@attribute evaluation {POSITIVE, NEGATIVE}\n\n@data");
		
		if(redoFile)
			redoFile = false;
		
		for(int i = 0; i < 4 ; i++) {
			
			if(i%2 == 0)
				secondDirectory = new String("neg/");
			else if(i%2 != 0)
				secondDirectory = new String("pos/");
			
			if(i == 2)
				firstDirectory = new String("movie_review_dataset/part2/");
			
			for(int j = 0; j < 12500; j++) {
				
				String filePath = firstDirectory.concat(secondDirectory);
				filePath = filePath.concat(Integer.toString(j));
				filePath = filePath.concat("_");
				
				filePath = getFilePath(filePath);
				
				dataFile = readDataFile(filePath);
				
				String line = dataFile.readLine();
				
				int endLine = line.indexOf('.');
				if(endLine == -1)
					endLine = line.indexOf('!');
				if(endLine == -1)
					endLine = line.indexOf('?');
				if(endLine == -1)
					endLine = line.indexOf(';');
				if(endLine == -1)
					endLine = line.length();
				
				line = line.substring(0, endLine);
				line = line.replaceAll("[^\\w\\s]","");
				line = line.replaceAll("[0-9]","");
				line = new String("\"").concat(line);
				line = line.concat("\",");
				
				if(secondDirectory.contains("neg"))
					line = line.concat("NEGATIVE");
				else if(secondDirectory.contains("pos"))
					line = line.concat("POSITIVE");
				
				System.out.println(filePath);
				writeOnFile(line);
			}
		}
		
		dataFile.close();
 
		/*Instances data = new Instances(dataFile);
		data.setClassIndex(data.numAttributes() - 1);
 
		//do not use first and second
		Instance first = data.instance(0);
		Instance second = data.instance(1);
		Instance third = data.instance(2);
		data.delete(0);
		data.delete(1);
		data.delete(2);
 
		Classifier smo = new SMO();		
		smo.buildClassifier(data);
 
		double class1 = smo.classifyInstance(first);
		double class2 = smo.classifyInstance(second);
		double class3 = smo.classifyInstance(third);
 
		System.out.println("first: " + class1 + "\nsecond: " + class2 + "\nthird: " + class3);*/
			
	}
}