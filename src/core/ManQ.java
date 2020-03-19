package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.util.Vector;

import core.problems.ManqProblem;
import core.problems.OptimalQueryProblem;
import util.FileSearcher;
import util.Preprocessor;

public class ManQ {

	public static String genOPT(String query, IndexSearcher indexSearcher, ArrayList<String> goldset) {
		String[] terms = Preprocessor.preprocessing(query.toLowerCase()).split(" ");
		OptimalQueryProblem.setnItems(terms.length);
		OptimalQueryProblem.setTermList(terms);
		OptimalQueryProblem.setGoldSet(goldset);
		OptimalQueryProblem.setIndexer(indexSearcher);
		OptimalQueryProblem.setnObjs(1);

		NondominatedPopulation result = new Executor().withProblemClass(OptimalQueryProblem.class).withAlgorithm("GA")
				.withProperty("populationSize", 100).withMaxEvaluations(10000).distributeOnAllCores().run();

		double maxScore = 0;
		String optQuery = "";
		for (int i = 0; i < result.size(); i++) {
			Solution solution = result.get(i);
			double[] objectives = solution.getObjectives();
			objectives = Vector.negate(objectives);
			String candidate = "";
			for (int a = 0; a < result.get(i).getVariable(0).toString().toCharArray().length; a++) {
				if (result.get(i).getVariable(0).toString().toCharArray()[a] == '1') {
					candidate = candidate + terms[a] + " ";
				}
			}
			double score = objectives[0];
			if (score > maxScore) {
				optQuery = candidate;
			}
			if (score == maxScore) {
				if (optQuery.split(" ").length > candidate.split(" ").length)
					optQuery = candidate;
			}
		}
		if (optQuery.equals(""))
			optQuery = query;
		return optQuery;
	}

	public static ArrayList<String> genManqQuery(String query, HashMap<String, HashMap<String, Object>> termStatistics,
			HashMap<String, HashSet<String>> analyzedData, IndexSearcher indexSearcher, ArrayList<String> goldset,
			String[] qqpList, String[] elseList) {
		String[] terms = Preprocessor.removeIrreBlank(query.toLowerCase()).split(" ");
		ManqProblem.setnItems(terms.length);
		ManqProblem.setTermList(terms);
		ManqProblem.setnObjs(qqpList.length + 7);
		ManqProblem.setAnalyzedData(analyzedData);
		ManqProblem.setQqpList(qqpList);
		ManqProblem.setElseList(elseList);
		ManqProblem.setTermStatistics(termStatistics);

		try {
			NondominatedPopulation result = new Executor().withProblemClass(ManqProblem.class).withAlgorithm("NSGAIII")
					.withProperty("populationSize", 8).withMaxEvaluations(10000).withProperty("pm.rate", 0.01)
					.withProperty("sbx.rate", 0.3).distributeOnAllCores().run();

			ArrayList<String> candidateResult = new ArrayList<String>();
			double maxScore = 0;
			double maxDist = 0;
			double maxDistScore = 0;
			String bestQuery = "";
			String distQuery = "";
			double[][] scoreList = new double[qqpList.length + 7][result.size()];
			double[] evalList = new double[result.size()];
			for (int i = 0; i < result.size(); i++) {
				Solution solution = result.get(i);
				double[] objectives = solution.getObjectives();
				objectives = Vector.negate(objectives);
				String candidate = "";
				for (int a = 0; a < result.get(i).getVariable(0).toString().toCharArray().length; a++) {
					if (result.get(i).getVariable(0).toString().toCharArray()[a] == '1') {
						candidate = candidate + terms[a] + " ";
					}
				}
				Analyzer analyzer = new StandardAnalyzer();
				QueryParser parser = new QueryParser("contents", analyzer);
				ArrayList<String> searchResult = FileSearcher.searchFile(indexSearcher, parser, 1000, candidate);
				ArrayList<Double> evalResult_base = FileSearcher.evaluator(searchResult, goldset, 1000);
				double score = evalResult_base.get(1) + evalResult_base.get(2);
				if (score == 0) {
					continue;
				} else {
					candidateResult.add(candidate);
				}
				evalList[i] = score;
				double dist = 0;
				for (int a = 0; a < objectives.length; a++) {
					double value = objectives[a];
					if (a < qqpList.length) {
						value = value / (candidate.split(" ").length);
					}
					dist = dist + value * value;
					scoreList[a][i] = value;
				}
				dist = Math.sqrt(dist);

				if (maxScore < score) {
					maxScore = score;
					bestQuery = candidate;
				}
				if (maxDist < dist) {
					maxDist = dist;
					maxDistScore = score;
					distQuery = candidate;
				}else if(maxDist == dist) {
					if(maxDistScore < score) {
						maxDist = dist;
						maxDistScore = score;
						distQuery = candidate;
					}
						
				}
			}
			ArrayList<String> queryResult = new ArrayList<String>();
			queryResult.add(bestQuery);
			queryResult.add(distQuery);
			String rankQuery = candidateResult.get(getRankQuery(scoreList, scoreList.length, candidateResult.size(), evalList));
			queryResult.add(rankQuery);
			return queryResult;
		} catch (Exception e) {
			System.out.println("ERROR");
			ArrayList<String> queryResult = new ArrayList<String>();
			queryResult.add(query); queryResult.add(query); queryResult.add(query);
			return queryResult;
		}

	}


	private static int getRankQuery(double[][] scoreList, int objLength, int resultLength, double[] evalList) {
		int[][] rankList = new int[objLength][resultLength];
		for (int a = 0; a < scoreList.length; a++) {
			double[] scores = scoreList[a];
			int[] ranks = new int[scoreList.length];
			for (int i = 0; i < ranks.length; i++) {
				ranks[i] = 1;
			}

			for (int i = 0; i < scores.length; i++) {
				for (int j = 0; j < scores.length; j++) {
					if (scores[i] < scores[j])
						ranks[i] = ranks[i] + 1;
				}
			}
			rankList[a] = ranks;
		}

		int minRank = 99999;
		int minRankIndex = -1;
		double maxEval = -1;
		for (int a = 0; a < resultLength; a++) {
			int sumRank = 0;
			for (int b = 0; b < objLength; b++) {
				sumRank = sumRank + rankList[b][a];
			}
			if (minRank > sumRank) {
				minRank = sumRank;
				minRankIndex = a;
				maxEval = evalList[a];
			}else if(minRank == sumRank) {
				double x = evalList[a];
				if(maxEval < x) {
					minRank = sumRank;
					minRankIndex = a;
					maxEval = evalList[a];					
				}					
			}
		}
		return minRankIndex;
	}

}
