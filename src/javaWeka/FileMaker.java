package javaWeka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import wordTreating.StrikeAMatch;

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
	
	// Tries to open one of the files containing a review, returns the path if found
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
		
		// Score of 5 is not included, score 11 doesn't exist...
		if(i == 5 || i == 11) {
			System.err.println("File not found: " + filePath);
			System.exit(1);
		}
		
		return filePath;
	}
 
	// Writes on our arff file, tries to delete if already exists (to avoid writing over an existing one)
	private static void writeOnWekaFile(String line) {
		
		File file = new File("wekaFiles/movieReviews.arff");
		PrintWriter pw;
		
		try {
			
			if(redoFile && file.exists()) {
				while(!file.delete()){}
					file.delete();
				redoFile = false;
			}
			
			// In case it doesn't exist
			if(!file.exists())
				file.createNewFile();
			
			pw = new PrintWriter(new FileWriter(file, true));
			
			pw.print(line);
			
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Returns a list containing the stop words written in the mentioned file
	private static List<String> getStopWords() {
		
		List<String> words = new ArrayList<String>();
		BufferedReader br = readDataFile("textFiles/englishStopwords.txt");
		
		try {
			for(String line = br.readLine(); line != null; line = br.readLine())
				words.add(line);
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return words;
	}
	
	// Extracts the reviews, putting them in a text file and classificating as positive or negative ,if doesn't exist. Then inside a map and returns it
	private static Map<String, String> extractReviews() {
		
		File file = new File("textFiles/reviews.txt");
		Map<String, String> reviewMap = new TreeMap<String, String>();
		
		if(!file.exists()) {
			
			String firstDirectory = new String("movie_review_dataset/part1/");
			String secondDirectory = null;
			BufferedReader dataFile = null;
			String classification = null;
			PrintWriter pw = null;
			
			try {
				
				file.createNewFile();
				pw = new PrintWriter(file);
				
				for (int i = 0; i < 4; i++) {

					if (i % 2 == 0) {
						secondDirectory = new String("neg/");
						classification = new String("NEGATIVE");
					}
					else if (i % 2 != 0) {
						secondDirectory = new String("pos/");
						classification = new String("POSITIVE");
					}

					if (i == 2)
						firstDirectory = new String("movie_review_dataset/part2/");

					for (int j = 0; j < 12500; j++) {

						String filePath = firstDirectory.concat(secondDirectory);
						filePath = filePath.concat(Integer.toString(j));
						filePath = filePath.concat("_");
						
						filePath = getFilePath(filePath);
						
						dataFile = readDataFile(filePath);

						String line = dataFile.readLine();

						line = line.toLowerCase();
						line = line.replaceAll("[^a-z\\s]", " ");
					
						// Getting rid of extra spaces (two times because some "survive" in the first)
						line = line.trim().replaceAll(" +", " ");
						line = line.trim().replaceAll("\\s\\s", " ");
					
						line = line.concat("@"); // My separator reference for the split
						line = line.concat(classification);
						
						if(!filePath.contains("12499_7.txt"))
							line = line.concat("\n");
							
						pw.print(line);
						
						System.out.println(filePath);
					}
				}
				
				pw.close();
				dataFile.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		readReviewFile(reviewMap);
		
		return reviewMap;
	}
	
	// Reads the file containing the reviews and stores the data inside a received map
	private static void readReviewFile(Map<String, String> reviewMap) {
		
		BufferedReader br  = readDataFile("textFiles/reviews.txt");
		
		try {
			
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				
				String[] content = line.split("@");
				reviewMap.put(content[0], content[1]);
				System.out.println("Review: \"" + content[0] + "\"\nType: \"" + content[1] + "\"\n");
			}
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Extracts a map of words from our reviews, first dividing in good and bad and returns the "best" map possible
	private static Map<String, String> extractWords(Map<String, String> reviewMap) {
		
		List<String> stopWords = getStopWords();
		Map<String, Integer> goodWords = new TreeMap<String, Integer>();
		Map<String, Integer> badWords = new TreeMap<String, Integer>();
		
		// TODO Similarity (changeable)
		double permitedSimilarity = 0.5;
		
		if(!(new File("textFiles/goodWords.txt").exists()) && !(new File("textFiles/baddWords.txt").exists())) {
			
			if(new File("textFiles/goodWords.txt").exists())
				while(new File("textFiles/goodWords.txt").exists())
					new File("textFiles/goodWords.txt").delete();
			if(new File("textFiles/badWords.txt").exists())
				while(new File("textFiles/badWords.txt").exists())
					new File("textFiles/badWords.txt").delete();

			int reviewNum = 1;
			
			Iterator<Entry<String, String>> iterator = reviewMap.entrySet().iterator();
			while(iterator.hasNext()) {
			
				Entry<String, String> entry = iterator.next();

				String line = entry.getKey();
			
				// Getting rid of extra spaces (two times in case some "survive" in the first)
				line = line.trim().replaceAll(" +", " ");
				line = line.trim().replaceAll("\\s\\s", " ");
			
				String words[] = line.split(" ");
			
				String classification;
				if(entry.getValue().contains("POS"))
					classification = new String("POSITIVE");
				else
					classification = new String("NEGATIVE");
			
				for(String word : words) {
		        
					// Desperation below...
					if(word.isEmpty() || word.length() == 0 || word.length() == 1
						|| word.length() == 2 || word.contains(" ") || word.contains("\\s"))
						continue;
					
					boolean itEnters = true;
					
					if(!word.contains("\\s") && !stopWords.contains(word)
						&& classification.contains("POS")) {
		            
						if(!goodWords.containsKey(word)) {
					
							if(!goodWords.isEmpty()) {
								
								Iterator<Entry<String, Integer>> goodIterator = goodWords.entrySet().iterator();
								
								while(goodIterator.hasNext()) {
								
									Entry<String, Integer> goodEntry = goodIterator.next();
									
									if(StrikeAMatch.compareStrings(word, goodEntry.getKey()) >= permitedSimilarity) {
										itEnters = false;
										goodWords.replace(goodEntry.getKey(), goodWords.get(goodEntry.getKey())+1);
									}
								}
							}
					
							if(itEnters)
								goodWords.put(word, 1);
						}
						else
							goodWords.replace(word, goodWords.get(word)+1);
		        	}
					else if(!word.contains("\\s") && !stopWords.contains(word)
							&& classification.contains("NEG")) {
		            
						if(!badWords.containsKey(word)) {
							
							if(!badWords.isEmpty()) {
								
								Iterator<Entry<String, Integer>> badIterator = badWords.entrySet().iterator();
								
								while(badIterator.hasNext()) {
								
									Entry<String, Integer> badEntry = badIterator.next();
									
									if(StrikeAMatch.compareStrings(word, badEntry.getKey()) >= permitedSimilarity) {
										itEnters = false;
										badWords.replace(badEntry.getKey(), badWords.get(badEntry.getKey())+1);
									}
								}
							}
					
							if(itEnters)
								badWords.put(word, 1);
						}
						else
							badWords.replace(word, badWords.get(word)+1);
					}
				}
				
				System.out.println("Review processed: " + reviewNum);
				reviewNum++;
			}
		
			writeWordFile(goodWords, "POSITIVE");
			writeWordFile(badWords, "NEGATIVE");
		}
		else {
			readWordFile(goodWords, "POSITIVE");
			readWordFile(badWords, "NEGATIVE");
		}
		
		return createFinalMap(goodWords, badWords, permitedSimilarity);
	}

	// Returns the map with the "best" words to use as attributes
	private static Map<String, String> createFinalMap(Map<String, Integer> goodWords, Map<String, Integer> badWords, double permitedSimilarity) {
		
		Map<String, String> map = new TreeMap<String, String>();
		
		// TODO Frequency (changeable)
		int frequencypermited = 5000;
		
		System.out.println("Selecting \"POSITIVE\" final words");
		Iterator<Entry<String, Integer>> iterator = goodWords.entrySet().iterator();
		while(iterator.hasNext()) {
		
			Entry<String, Integer> entry = iterator.next();
			
			if(!badWords.containsKey(entry.getKey())
				&& compareMaps(entry, badWords, permitedSimilarity)
				&& entry.getValue() >= frequencypermited) {
				
				map.put(entry.getKey(), "POSITIVE");
			}
		}
		
		if(!badWords.isEmpty()) {
			System.out.println("Selecting \"NEGATIVE\" final words");
			iterator = badWords.entrySet().iterator();
			while(iterator.hasNext()) {
		
				Entry<String, Integer> entry = iterator.next();
			
				if(!goodWords.containsKey(entry.getKey())
					&& compareMaps(entry, goodWords, permitedSimilarity)
					&& entry.getValue() >= frequencypermited) {
					
						map.put(entry.getKey(), "NEGATIVE");
				}
			}
		}
		
		return map;
	}

	// Check if a given word has a close resemblance to one from the opposite, later that one will be removed from it's map
	private static boolean compareMaps(Entry<String, Integer> entry, Map<String, Integer> mapWords, double permitedSimilarity) {
		
		Iterator<Entry<String, Integer>> iterator = mapWords.entrySet().iterator();
		boolean ret = true;
		List<String> wordsToRemove = new ArrayList<String>();
		
		// TODO Similarity final map (changeable)
		double newPermitedSimilarity = permitedSimilarity + 0.2;
		
		while(iterator.hasNext()) {
		
			Entry<String, Integer> entryT = iterator.next();
			
			if(StrikeAMatch.compareStrings(entry.getKey(), entryT.getKey()) >= newPermitedSimilarity) {
				
				if(entry.getValue() >= entryT.getValue()) {
					wordsToRemove.add(entryT.getKey());
					ret = false;
				}
			}
		}
		
		for(int i = 0; i < wordsToRemove.size(); i++)
			mapWords.remove(wordsToRemove.get(i));
		
		return ret;
	}

	// Writes the obtained words in the corresponding file
	private static void writeWordFile(Map<String, Integer> wordsMap, String type) {
		
		File file = null;
		if(type.contains("POS"))
			file = new File("textFiles/goodWords.txt");
		else
			file = new File("textFiles/badWords.txt");
		PrintWriter pw;
		
		try {
			
			file.createNewFile();
			
			pw = new PrintWriter(file);
			
			Iterator<Entry<String, Integer>> iterator = wordsMap.entrySet().iterator();
			
			while(iterator.hasNext()) {
			
				Entry<String, Integer> entry = iterator.next();
				
				if(iterator.hasNext())
					pw.print(entry.getKey()+ " " + Integer.toString(entry.getValue()) + "\n");
				else
					pw.print(entry.getKey()+ " " + Integer.toString(entry.getValue()));
			}
			
			pw.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Retrieves the words from the respective file and stores it inside the map
	private static void readWordFile(Map<String, Integer> mapWords, String type) {
		
		BufferedReader br = null;
		if(type.contains("POS"))
			br = readDataFile("textFiles/goodWords.txt");
		else
			br = readDataFile("textFiles/badWords.txt");
		
		try {
			
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				String[] content = line.split(" ");
				mapWords.put(content[0], Integer.decode(content[1]));
				System.out.println("File type: \"" + type + "\" Word read: \"" + content[0] + "\" Frequency: " + Integer.decode(content[1]));
			}
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Returns a list of the frequency a word has for each review (so the list is of size 50k :O)
	private static List<Integer> getWordFrequency(String selectedWord, Map<String, String> reviewMap) {
		
		List<Integer> list = new ArrayList<Integer>();
		
		// TODO Similarity final word frequency (changeable)
		double permitedSimilarity = 0.5;
		
		Iterator<Entry<String, String>> iterator = reviewMap.entrySet().iterator();
		while(iterator.hasNext()) {
			
			int count = 0;
			
			Entry<String, String> entry = iterator.next();
			
			String[] reviewWords = entry.getKey().split(" ");
			
			for(String word : reviewWords) {
				
				// Desperation below...
				if(word.isEmpty() || word.length() == 0 || word.length() == 1
					|| word.length() == 2 || word.contains(" ") || word.contains("\\s"))
					continue;
				
				if(StrikeAMatch.compareStrings(selectedWord, word) >= permitedSimilarity)
					count++;
			}
			
			list.add(count);
		}
		
		return list;
	}
	
	public static void main(String[] args) throws Exception {
		
		writeOnWekaFile("@relation \"Movie Reviews\"\n\n");
		int goodQuantity = 0, badQuantity = 0, wordNum = 1;
		
		if(redoFile)
			redoFile = false;
		
		Map<String, String> reviewMap = extractReviews();
		
		// This map contains the universe of words (Key == word && Value == Classification)
		Map<String, String> processedWords = extractWords(reviewMap);
		
		Map<String, List<Integer>> wordFrequencyMap = new HashMap<String, List<Integer>>();
		
		Iterator<Entry<String, String>> iterator = processedWords.entrySet().iterator();
		while(iterator.hasNext()) {
		
			Entry<String, String> entry = iterator.next();
			
			if(iterator.hasNext())
				System.out.println("\"" + entry.getKey()+ "\" - \"" + entry.getValue() + "\"");
			else
				System.out.println("\"" + entry.getKey() + "\" - \"" + entry.getValue() + "\"");
			
			if(entry.getValue().contains("POS"))
				goodQuantity++;
			else
				badQuantity++;
			
			String line = new String("@attribute word").concat(Integer.toString(wordNum));
			line = line.concat(" numeric\n");
			
			writeOnWekaFile(line);
			
			wordNum++;
			
			 List<Integer> wordFrequency = getWordFrequency(entry.getKey(), reviewMap);
			 
			 wordFrequencyMap.put(entry.getKey(), wordFrequency);
		}
		
		System.out.println("Total: " + processedWords.size());
		System.out.println("Good words: " + goodQuantity);
		System.out.println("Bad words: " + badQuantity);
		
		writeOnWekaFile("@attribute evaluation {POSITIVE, NEGATIVE}\n\n@data\n");
		
		int review = 0;
		
		iterator = reviewMap.entrySet().iterator();
		while(iterator.hasNext()) {
			
			Entry<String, String> entry = iterator.next();
			
			String line = new String();
			
			Iterator<Entry<String, List<Integer>>> freqIterator = wordFrequencyMap.entrySet().iterator();
			while(freqIterator.hasNext()) {
			
				Entry<String, List<Integer>> freqEntry = freqIterator.next();
				
				line = line.concat(Integer.toString(freqEntry.getValue().get(review)));
				line = line.concat(",");
			}
			
			line = line.concat(entry.getValue());
			
			if(iterator.hasNext())
				line = line.concat("\n");
			
			writeOnWekaFile(line);
			
			System.out.println("Wrote data line " + (review + 1));
			review++;
		}
		
		/*for(int i = 0; i < 4 ; i++) {
			
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
				
				/*int endLine = line.indexOf('.');
				if(endLine == -1)
					endLine = line.indexOf('!');
				if(endLine == -1)
					endLine = line.indexOf('?');
				if(endLine == -1)
					endLine = line.indexOf(';');
				if(endLine == -1)
					endLine = line.length();
				line = line.substring(0, endLine);
				
				line = line.replaceAll("[^a-zA-Z\\s\\']","");
				String words[] = line.split(" ");
				
				line = new String("\"").concat(line);
				line = line.concat("\",");
				
				if(secondDirectory.contains("neg"))
					line = line.concat("NEGATIVE");
				else if(secondDirectory.contains("pos"))
					line = line.concat("POSITIVE");
				
				System.out.println(filePath);
				//writeOnWekaFile(line);
			}
		}*/
 
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