package util;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class FileSearcher {
	private static String removeNoise(String string) {
		return string.split("\\\\")[string.split("\\\\").length - 1].replace(".java", "");
	}

	public static ArrayList<String> searchFile(IndexSearcher searcher, QueryParser parser, int top, String searchQuery) {
		ArrayList<String> searchResult = new ArrayList<String>();
		int queryLen = searchQuery.split(" ").length;
		if (queryLen > 1024)
			BooleanQuery.setMaxClauseCount(queryLen);

		try {
			Query myquery = parser.parse(searchQuery);
			TopDocs results = searcher.search(myquery, top);
			ScoreDoc[] hits = results.scoreDocs;
			String findBuggyFile = "";
			ArrayList<String> rankedList = new ArrayList<String>();
			for (int i = 0; i < hits.length; ++i) {
				ScoreDoc item = hits[i];
				Document doc;
				doc = searcher.doc(item.doc);
				searchResult.add(removeNoise(doc.get("path")));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return searchResult;
	}

	public static ArrayList<Double> evaluator(ArrayList<String> searchResult, ArrayList<String> goldSet, int topK) {
		ArrayList<Double> evalResult = new ArrayList<Double>();
		double ap = 0;
		double rr = 0;
		double top = 0;
		double goldIndex = 0;
		for(int i = 0 ; i <searchResult.size(); i++) {
			double rank = i+1;
			String locFile = searchResult.get(i);
			if(!goldSet.contains(locFile)) {
				continue;
			}
			if(i == topK) {
				break;
			}
			goldIndex ++;
			if(top == 0) {
				top = rank;
				rr = 1.0 / top;
			}
			ap = goldIndex / rank;			
		}
		ap = ap / goldIndex;
		evalResult.add(top);
		evalResult.add(rr);
		evalResult.add(ap);
		return evalResult;
	}

}
