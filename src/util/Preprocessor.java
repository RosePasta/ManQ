package util;

import java.io.StringReader;
import java.util.ArrayList;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class Preprocessor {

	static public String preprocessing(String content) {
		Stopword stops = new Stopword();		
		String preprocessedContent = "";
		content = split(content.toLowerCase());		
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<CoreLabel>(new StringReader(content),
				new CoreLabelTokenFactory(), "");
		for (CoreLabel label; ptbt.hasNext();) {
			String token = ptbt.next().toString();
			if (!stops.isEnglishStopword(token)) {
				if(token.length() > 2) {
					preprocessedContent = preprocessedContent + token + " ";
				}
			}
		}
		return removeIrreBlank(split(preprocessedContent));
	}
	public static String split(String natureLanguage) {
		ArrayList<String> wordList = new ArrayList<String>();
		StringBuffer wordBuffer = new StringBuffer();
		char ac[] = natureLanguage.toCharArray();
		for (int i = 0; i < natureLanguage.toCharArray().length; i++) {
			char c = ac[i];
			if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '\'') {
				wordBuffer.append(c);
			} else {
				String word = wordBuffer.toString();
				if (!word.equals(""))
					wordList.add(word);
				wordBuffer = new StringBuffer();
			}
		}

		if (wordBuffer.length() != 0) {
			String word = wordBuffer.toString();
			if (!word.equals(""))
				wordList.add(word);
			wordBuffer = new StringBuffer();
		}
		String[] tokens = (String[]) wordList.toArray(new String[wordList.size()]);
		String content = "";
		for(int i = 0 ; i<tokens.length; i++) {
			content = content + tokens[i]+" ";
		}
		return content;
	}
	

	public static String removeIrreBlank(String query) {

		while (query.contains("  ")) {
			query = query.replace("  ", " ");
		}
		if (query.endsWith(" ")) {
			query = query.substring(0, query.length() - 1);
		}
		if (query.startsWith(" ")) {
			query = query.substring(1);
		}
		
		return query;
	}
}
