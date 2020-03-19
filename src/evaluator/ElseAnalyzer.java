package evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import core.ManQ;
import core.QueryEvaluator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import util.DataLoader;
import util.Extractor;
import util.FileSearcher;
import util.Preprocessor;

public class ElseAnalyzer {

	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		properties.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("./data/sampled_data/index/").toPath()));
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("contents", analyzer);
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		HashMap<String, HashMap<String, Object>> termStatistics = DataLoader.getQQPData();

		ArrayList<String> targetBugs = DataLoader.getPoorQueries();
		HashMap<String, String> bugMap = DataLoader.getBugreport(targetBugs);
		HashMap<String, String> baseMap = DataLoader.getBaseline(targetBugs);
		HashMap<String, ArrayList<String>> goldMap = DataLoader.getGoldset(targetBugs);
		HashSet<String> fileNameSet = Extractor.getFileName();

		for (String bugID : targetBugs) {
			String bugReport = bugMap.get(bugID);
			String baseline = baseMap.get(bugID);
			String[] terms = baseline.split(" ");
			for (int a = 0; a < terms.length; a++) {
				String term = terms[a];
			}

			ArrayList<String> searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, baseline);
			ArrayList<Double> evalResult_base = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_base = evalResult_base.get(0);

			String optQuery = ManQ.genOPT(baseline, indexSearcher, goldMap.get(bugID));
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, optQuery);
			ArrayList<Double> evalResult_opt = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_opt = evalResult_opt.get(0);

			double cnt = 0;
			if (top_base > top_opt)
				cnt++;
			if (top_base < top_opt)
				cnt--;

			evalResult_opt = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10);
			double rr = evalResult_opt.get(1);
			double ap = evalResult_opt.get(2);

			double top1 = 0;
			double top5 = 0;
			double top10 = 0;
			if (top_opt <= 1)
				top1++;
			if (top_opt <= 5)
				top5++;
			if (top_opt <= 10)
				top10++;

			System.out.println(bugID + "\t" + top_base + "\t" + top_opt + "\t" + top1 + "\t" + top5 + "\t" + top10
					+ "\t" + rr + "\t" + ap + "\t" + cnt + "\t" + optQuery);

			HashMap<String, HashSet<String>> analyzedData = Extractor.getData(bugReport, pipeline);

			String oebQuery = Preprocessor.preprocessing(analyzedData.get("OEB").toString());
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, oebQuery);
			ArrayList<Double> evalResult_oeb = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_oeb = evalResult_oeb.get(0);

			String keyQuery = Preprocessor.preprocessing(analyzedData.get("KEYWORD").toString());
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, keyQuery);
			ArrayList<Double> evalResult_key = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_key = evalResult_key.get(0);

			String posQuery = "";
			Iterator<String> iter = analyzedData.get("POS").iterator();
			while (iter.hasNext()) {
				String pos = iter.next();
				String term = pos.split("\\:")[0];
				String val = pos.split("\\:")[1];
				if (val.equals("1.0"))
					posQuery = posQuery + term + " ";
				if (val.equals("0.8") && Math.random() > 0.5)
					posQuery = posQuery + term + " ";
				if (val.equals("0.6") && Math.random() > 0.7)
					posQuery = posQuery + term + " ";
			}

			posQuery = Preprocessor.preprocessing(posQuery);
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, posQuery);
			ArrayList<Double> evalResult_pos = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_pos = evalResult_pos.get(0);

			String fileQuery = "";
			iter = fileNameSet.iterator();
			while (iter.hasNext()) {
				String filename = iter.next().toLowerCase();
				if (baseline.toLowerCase().contains(filename))
					fileQuery = fileQuery + filename + " ";
			}

			fileQuery = Preprocessor.preprocessing(fileQuery);
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, fileQuery);
			ArrayList<Double> evalResult_file = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_file = evalResult_file.get(0);

			String allQuery = Preprocessor.preprocessing(fileQuery + " " + posQuery + " " + keyQuery + " " + oebQuery);
			searchResult = FileSearcher.searchFile(indexSearcher, parser, 10000, allQuery);
			ArrayList<Double> evalResult_all = FileSearcher.evaluator(searchResult, goldMap.get(bugID), 10000);
			double top_all = evalResult_all.get(0);

			System.out.println(bugID + "\t" + top_base + "\t" + top_oeb + "\t" + top_key + "\t" + top_pos + "\t"
					+ top_file + "\t" + top_all);
		}

	}
}