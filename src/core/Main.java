package core;

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

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import util.DataLoader;
import util.Extractor;
import util.FileSearcher;
import util.Preprocessor;

public class Main {

	static String[] qqpList = { "VAR", "SCQ", "PMI", "IDF", "ICTF", "ENT", "QS", "SCS" };
	static String[] elseList = { "OEB", "FILE", "KEY", "POS", "SENT" };
	static int topn = 100;

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
		HashMap<String, String> strictMap = DataLoader.getTechniqueQuery(targetBugs, "strict");
		HashMap<String, String> blzMap = DataLoader.getTechniqueQuery(targetBugs, "blizzard");
		HashMap<String, ArrayList<String>> goldMap = DataLoader.getGoldset(targetBugs);
		HashSet<String> fileNameSet = Extractor.getFileName();

		for (String bugID : targetBugs) {
			String bugReport = bugMap.get(bugID);
			String baseline = baseMap.get(bugID);
			HashMap<String, HashSet<String>> analyzedData = Extractor.getData(bugReport, pipeline);
			analyzedData.put("FNAME", fileNameSet);

			ArrayList<String> searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, baseline);
			ArrayList<Double> evalResult_base = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_base = evalResult_base.get(0);

			String strict = baseline;
			if (strictMap.containsKey(bugID))
				strict = strictMap.get(bugID);
			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, strict);
			evalResult_base = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_strict = evalResult_base.get(0);

			String blz = baseline;
			if (blzMap.containsKey(bugID))
				blz = blzMap.get(bugID);
			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, blz);
			evalResult_base = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_blz = evalResult_base.get(0);

			String optQuery = ManQ.genOPT(baseline, indexSearcher, goldMap.get(bugID));
			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, optQuery);
			ArrayList<Double> evalResult_opt = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_opt = evalResult_opt.get(0);

			ArrayList<String> manqQueryList = ManQ.genManqQuery(baseline, termStatistics, analyzedData, indexSearcher,
					goldMap.get(bugID), qqpList, elseList);
			String manqBestQuery = manqQueryList.get(0).toLowerCase();
			String manqDistQuery = manqQueryList.get(1).toLowerCase();
			String manqRankQuery = manqQueryList.get(2).toLowerCase();
			String manqQuery = manqRankQuery;
			String[] text = manqDistQuery.split(" ");
			for (int i = 0; i < text.length; i++) {
				if (!manqQuery.contains(text[i]))
					manqQuery = manqQuery + " " + text[i];
			}

			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, manqRankQuery);
			ArrayList<Double> evalResult_manq_rank = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_manq_rank = evalResult_manq_rank.get(0);

			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, manqDistQuery);
			ArrayList<Double> evalResult_manq_dist = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_manq_dist = evalResult_manq_dist.get(0);

			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, manqBestQuery);
			ArrayList<Double> evalResult_manq_best = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_manq_best = evalResult_manq_best.get(0);

			searchResult = FileSearcher.searchFile(indexSearcher, parser, topn, manqQuery);
			ArrayList<Double> evalResult_manq = FileSearcher.evaluator(searchResult, goldMap.get(bugID), topn);
			double top_manq = evalResult_manq.get(0);

			int check = 0;
			double diff = top_manq - top_base;
			if (top_base == 0 && top_manq != 0) {
				check = 1;
				diff = -topn;
			} else if (top_base != 0 && top_manq == 0) {
				check = 0;
				diff = topn;
			} else if (top_base > top_manq) {
				check = 1;
			} else if (top_base == top_manq) {
				check = 2;
			}

			int bestcheck = 0;
			if (top_base == 0 && top_manq_best != 0) {
				bestcheck = 1;
			} else if (top_base > top_manq_best) {
				bestcheck = 1;
			} else if (top_base == top_manq_best) {
				bestcheck = 2;
			}

			System.out.println(bugID + "\t" + top_base + "\t" + top_strict + "\t"+ top_blz + "\t" + top_opt + "\t" + top_manq_rank
					+ "\t" + top_manq_dist + "\t" + top_manq_best + "\t" + top_manq + "\t" + bestcheck + "\t" + check
					+ "\t" + diff+"\t"+baseline.split(" ").length+"\t"+manqQuery.split(" ").length+"\t"+manqBestQuery.split(" ").length);
		}

	}

	private static void PrintBaseObjectives(String query, HashMap<String, HashMap<String, Object>> termStatistics,
			String[] qqpList, String[] elseList, HashMap<String, HashSet<String>> analyzedData,
			IndexSearcher indexSearcher, ArrayList<String> goldset) {
		ArrayList<Double> qqpValues = QueryEvaluator.getQQP(query, termStatistics, qqpList);
		ArrayList<Double> elseValues = QueryEvaluator.getElse(query, analyzedData, elseList);
		int nObjs = qqpList.length + 7;
		double[] f = new double[nObjs];
		for (int i = 0; i < qqpValues.size(); i++)
			f[i] = qqpValues.get(i);

		for (int i = 0; i < elseValues.size(); i++)
			f[qqpList.length + i] = elseValues.get(i);

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("contents", analyzer);
		ArrayList<String> searchResult = FileSearcher.searchFile(indexSearcher, parser, 1000, query);
		ArrayList<Double> evalResult_base = FileSearcher.evaluator(searchResult, goldset, 1000);
		System.out.print("-1\t" + evalResult_base.get(0) + "\t");

		f[nObjs - 2] = QueryEvaluator.cosineSimilarity(query, query);
		f[nObjs - 1] = 1.0 - (query.length() / query.length());

		for (int a = 0; a < nObjs; a++) {
			System.out.print(String.format("%.3f", f[a]) + "\t");
		}
		System.out.println(query);
	}

}
