package javaWeka;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
 
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Instance;
import weka.core.Instances;

public class Learner {

	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;
 
		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}
 
		return inputReader;
	}
 
	public static void main(String[] args) throws Exception {
		BufferedReader datafile = readDataFile("weather_c45.txt");
 
		Instances data = new Instances(datafile);
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
 
		System.out.println("first: " + class1 + "\nsecond: " + class2 + "\nthird: " + class3);
			
	}
}