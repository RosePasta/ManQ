package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;

public class Extractor {

	public static HashSet<String> getFileName() {
		HashSet<String> fileNameSet = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(
					new FileReader("./data/sampled_data/index/key_file.ckeys"));
			String str;
			while ((str = br.readLine()) != null) {
				String fileName = str.split("ssystems\\\\tomcat70\\\\")[1].replace("\\", "/");
				fileName = fileName.split("/")[fileName.split("/").length - 1].replace(".java", "");
				if(fileName.length() <= 5)
					continue;
				int numUp = 0;
				boolean numeric = false;
				for(int idx = 0 ; idx < fileName.length(); idx++) {
					if(fileName.charAt(idx)>='A' && fileName.charAt(idx)<='Z')
						numUp++;
					if(fileName.charAt(idx)>='0' && fileName.charAt(idx)<='9')
						numeric = true;
				}
				if(numeric) {
					continue;
				}
				
//				if(numUp > 1) {
				if(numUp > 0) {
					fileNameSet.add(fileName.toLowerCase());
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileNameSet;
	}

	public static HashMap<String, HashSet<String>> getData(String bugReport, StanfordCoreNLP pipeline) {
		HashMap<String, HashSet<String>> analyzedData = new HashMap<String, HashSet<String>>();

		HashSet<String> sentenceList = new HashSet<String>();
		HashSet<String> oebTokens = new HashSet<String>();
		HashSet<String> posSet = new HashSet<String> ();

		String[] oebKeys = { "fail","error","obser", "expec",  "not", "'t", "should", "shall", "however","but" };

		
		ArrayList<String> oebKeyList = new ArrayList(Arrays.asList(oebKeys));
		Annotation annotation = new Annotation(bugReport);
		pipeline.annotate(annotation);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			String sent = sentence.toString().toLowerCase();
			if(sent.length() <= 5)
				continue;
			
			String ppSent = Preprocessor.preprocessing(sent);
			sentenceList.add(ppSent);
			List<String> ppSentTokens = Arrays.asList(ppSent.split(" "));
			
			String[] tokenList = sent.split(" ");
			for(int i = 0 ; i<tokenList.length; i++) {
				if (tokenList[i].length() <= 3) {
					continue;
				}
				if(oebKeyList.contains(tokenList[i])) {					
					for(int j = 0 ; j<ppSentTokens.size(); j++)
						oebTokens.add(ppSentTokens.get(j));
					break;
				}
			}
			
			for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
		        String word = token.get(CoreAnnotations.TextAnnotation.class).toLowerCase();
		        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
		        if(ppSentTokens.contains(word)) {
			        if(pos.contains("NN"))
			        	posSet.add(word+":1.0");
			        else if(pos.contains("VB") || pos.contains("JJ"))
			        	posSet.add(word+":0.8");
			        else if(pos.contains("RB"))
			        	posSet.add(word+":0.4");
		        }
		    }			
		}
		analyzedData.put("SENTENCE", sentenceList);
		analyzedData.put("OEB", oebTokens);
		analyzedData.put("POS", posSet);
		
		String sum = Preprocessor.preprocessing(bugReport.split("\n")[0]).split(" ",2)[1];
		String[] sumTokens = sum.split(" ");
		int len = sumTokens.length;
		HashSet<String> keyTokens = new HashSet<String>();
		if(len > 0) {
			keyTokens.add(sumTokens[0]);
			if(len > 1)
				keyTokens.add(sumTokens[1]);
			if(len - 2 >= 0)
				keyTokens.add(sumTokens[len-2]);
			if(len - 1 >= 0)
				keyTokens.add(sumTokens[len-1]);
		}

		analyzedData.put("KEYWORD", keyTokens);
		return analyzedData;
	}

}
