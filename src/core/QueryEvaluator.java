package core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.CosineSimilarity;

public class QueryEvaluator {

	public static ArrayList<Double> getQQP(String query,
			HashMap<String, HashMap<String, Object>> termStatistics, String[] qqpList) {
		HashSet<String> termSet = new HashSet<String>(Arrays.asList(query.split(" ")));
		Iterator<String> iter = termSet.iterator();
		ArrayList<Double> sumValue = new ArrayList<Double>();
		for(int i = 0 ; i<qqpList.length; i++)
			sumValue.add(0.0);
		int qsIndex = -1;
		int pmiIndex = -1;
		HashSet<String> qs = new HashSet<String>();
		while(iter.hasNext()) {
			String term = iter.next();
			if(!termStatistics.containsKey(term)) {
				continue;
			}
			HashMap<String, Object> statistics = termStatistics.get(term);
			HashSet<String> docSet = new HashSet<String>();
			for(int i = 0 ; i<qqpList.length; i++) {
				String qqpKey = qqpList[i];
				if(statistics.containsKey(qqpKey)) {
					double value = (double) statistics.get(qqpKey);
					sumValue.set(i,sumValue.get(i) + value);
				}else {
					switch(qqpKey) {
					case "PMI":
						pmiIndex = i;
						break;					
					case "SCS":
						double value = (double) statistics.get("PROB_T");
						value = value * Math.log(1.0/value);
						if(Double.isFinite(value))
							sumValue.set(i,sumValue.get(i) + value);
						break;
					case "QS":
						docSet = new HashSet<String>(Arrays.asList(((String)statistics.get("DOCS")).split(",")));
						qs.addAll(docSet);
						qsIndex = i;
						break;
						
					}
				}
			}
			double term_prob = (double) statistics.get("PROB_D");
			Iterator<String> iter2 = termSet.iterator();
			while(iter2.hasNext()) {
				String term2 = iter2.next();
				if(!termStatistics.containsKey(term2)) {
					continue;
				}if(term.equals(term2)) {
					continue;
				}
				double term2_prob = (double) termStatistics.get(term2).get("PROB_D");
				HashSet<String> docSet2 = new HashSet<String>(Arrays.asList(((String)termStatistics.get(term2).get("DOCS")).split(",")));
				HashSet<String> retainDoc = new HashSet<String>(docSet2);
				retainDoc.retainAll(docSet);
				HashSet<String> unionDoc = new HashSet<String>(docSet2);
				unionDoc.addAll(docSet);
				
				double mutual_prob = (retainDoc.size()*1.0) / (unionDoc.size()*1.0);
				mutual_prob = mutual_prob / (term_prob * term2_prob);
				if(Double.isFinite(mutual_prob)) {
					sumValue.set(pmiIndex,sumValue.get(pmiIndex) + mutual_prob);
				}				
			}
			
		}
		if(qsIndex > -1)
			sumValue.set(qsIndex, (1.0*qs.size()));
		return sumValue;
	}

	public static ArrayList<Double> getElse(String query, HashMap<String, HashSet<String>> analyzedData, String[] elseList) {

		ArrayList<Double> result = new ArrayList<Double>();
		HashSet<String> oebs = analyzedData.get("OEB");
		HashSet<String> fName = analyzedData.get("FNAME");
		HashSet<String> keywords = analyzedData.get("KEYWORD");
		HashSet<String> posMap = analyzedData.get("POS");
		HashSet<String> sentences = analyzedData.get("SENTENCE");
		
		double oebNum = 0;
		double fNum = 0;
		double keyNum = 0;
		double posScore = 0;
		String[] termList = query.split(" ");
		for(int i = 0 ; i<termList.length; i++) {
			String term = termList[i].toLowerCase();
			if(oebs.contains(term)) {				
				oebNum++;
			}
			if(fName.contains(term)) {
				fNum = 1;
			}
			if(keywords.contains(term)) {
				keyNum++;
			}
		}
		if(oebs.size() > 0)
			result.add(oebNum);
		else
			result.add(0.0);
		
		result.add(fNum);
		
		if(keywords.size() > 0)
			result.add(keyNum);
		else
			result.add(0.0);
		
		
		Iterator<String> positer = posMap.iterator();
		while(positer.hasNext()) {
			String pos = positer.next();
			String term = pos.split("\\:")[0];
			double value = Double.parseDouble(pos.split("\\:")[1]);
			if((" "+query+" ").contains(term))
				posScore = posScore + value;
		}
		result.add(posScore);
		
		HashMap<String, Double> sentenceProb = getSentenceProb(sentences, termList);
		Iterator<String> sentenceIter =sentences.iterator();
		HashMap<String, Double> sentenceOccurProb = new HashMap<String, Double>();
		while(sentenceIter.hasNext()) {
			String sentence = " "+sentenceIter.next()+" ";
			for(int i = 0 ; i <termList.length-1; i++) {
				String term1 = " "+termList[i]+" ";
				for(int j = i+1; j<termList.length; j++) {
					String term2 = " "+termList[j]+" ";
					if(sentence.contains(term1) && sentence.contains(term2)) {
						String key1 = term1.replace(" ", "")+"-"+term2.replace(" ", "");
						String key2 = term2.replace(" ", "")+"-"+term1.replace(" ", "");
						if(sentenceOccurProb.containsKey(key1) && sentenceOccurProb.containsKey(key2)) {
							sentenceOccurProb.replace(key1, sentenceOccurProb.get(key1)+1.0);
							sentenceOccurProb.replace(key2, sentenceOccurProb.get(key2)+1.0);
						}else {
							sentenceOccurProb.put(key1, 1.0);
							sentenceOccurProb.put(key2, 1.0);
						}
					}
				}
			}
		}
		double pmi = 0;
		Iterator<String> occurIter = sentenceOccurProb.keySet().iterator();
		while(occurIter.hasNext()) {
			String key = occurIter.next();
			String term1 = key.split("-")[0];
			String term2 = key.split("-")[1];
			double prob1 = sentenceProb.get(term1);
			double prob2 = sentenceProb.get(term2);
			double occurProb = sentenceOccurProb.get(key) / (1.0*sentences.size());
			double value = Math.log(occurProb / (prob1*prob2));
			if(Double.isFinite(value)) {
				pmi = pmi + value;
			}				
		}
		
		result.add(pmi/(1.0*sentenceOccurProb.size()));
		return result;
	}

	private static HashMap<String, Double> getSentenceProb(HashSet<String> sentences, String[] termList) {
		HashMap<String, Double> probMap = new HashMap<String, Double>();
		HashSet<String> termSet = new HashSet<String>(Arrays.asList(termList));
		Iterator<String> sentenceIter =sentences.iterator();
		while(sentenceIter.hasNext()) {
			String sentence = " "+sentenceIter.next()+" ";
			Iterator<String> termIter = termSet.iterator();
			while(termIter.hasNext()) {
				String term = " "+termIter.next()+" ";
				if(sentence.contains(term)) {
					term = term.replace(" ", "");
					if(probMap.containsKey(term)) {
						probMap.replace(term, probMap.get(term)+1.0);
					}else {
						probMap.put(term,1.0);
					}
				}
			}
		}
		Iterator<String> probIter = probMap.keySet().iterator();
		while(probIter.hasNext()) {
			String probTerm = probIter.next();
			double value = probMap.get(probTerm);
			value = value / (1.0*sentences.size());
			probMap.replace(probTerm, value);
		}
		return probMap;
	}

	public static double cosineSimilarity(String query, String initialQuery) {
		CosineSimilarity documentsSimilarity  = new CosineSimilarity();

		Map<CharSequence, Integer> vectorA =  Arrays.stream(query.split(" ")).collect(Collectors.toMap(
                character -> character, character -> 1, Integer::sum));
		
		Map<CharSequence, Integer> vectorB =  Arrays.stream(initialQuery.split(" ")).collect(Collectors.toMap(
                character -> character, character -> 1, Integer::sum));
		

		Double docABCosSimilarity = documentsSimilarity.cosineSimilarity(vectorA, vectorB);


	    return docABCosSimilarity;
	}
	

}
