/* Copyright 2009-2016 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package core.problems;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.Vector;

import util.FileSearcher;
import util.Preprocessor;

public class OptimalQueryProblem extends AbstractProblem {

	private static int nObjs;
	static int nItems;
	static String[] termList;
	static IndexSearcher indexer = null;
	static ArrayList<String> goldSet = null;

	public OptimalQueryProblem() {
		super(1, nObjs, nObjs);

	}

	@Override
	public void evaluate(Solution solution) {
		boolean[] d = EncodingUtils.getBinary(solution.getVariable(0));
		double[] f = new double[nObjs];

		String query = "";
		for (int i = 0; i < nItems; i++) {
			if (d[i]) {
				query = query + termList[i] + " ";
			}
		}
		query = Preprocessor.removeIrreBlank(query);
		if (query.length() < 2) {
			f[0] = 0.0;
		} else {
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser queryParser = new QueryParser("contents", analyzer);

			int topN = 1000;
			ArrayList<String> searchResult = FileSearcher.searchFile(indexer, queryParser, topN, query);
			ArrayList<Double> evalResult = FileSearcher.evaluator(searchResult, goldSet, topN);			
			f[0] = evalResult.get(1) +  evalResult.get(2);
		}
		solution.setObjectives(Vector.negate(f));
	}

	@Override
	public Solution newSolution() {
		Solution solution = new Solution(1, nObjs, nObjs);
		solution.setVariable(0, EncodingUtils.newBinary(nItems));
		return solution;
	}

	public static int getnObjs() {
		return nObjs;
	}

	public static void setnObjs(int nObjs) {
		OptimalQueryProblem.nObjs = nObjs;
	}

	public static int getnItems() {
		return nItems;
	}

	public static void setnItems(int nItems) {
		OptimalQueryProblem.nItems = nItems;
	}

	public static String[] getTermList() {
		return termList;
	}

	public static void setTermList(String[] termList) {
		OptimalQueryProblem.termList = termList;
	}

	public static IndexSearcher getIndexer() {
		return indexer;
	}

	public static void setIndexer(IndexSearcher indexer) {
		OptimalQueryProblem.indexer = indexer;
	}

	public static ArrayList<String> getGoldSet() {
		return goldSet;
	}

	public static void setGoldSet(ArrayList<String> goldSet) {
		OptimalQueryProblem.goldSet = goldSet;
	}
	
	
}
