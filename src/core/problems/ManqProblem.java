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
import java.util.HashSet;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.Vector;

import core.QueryEvaluator;
import util.Preprocessor;

public class ManqProblem extends AbstractProblem {

	private static int nObjs;
	static int nItems;
	static String[] termList;
	static HashMap<String, HashMap<String, Object>> termStatistics;
	static HashMap<String, HashSet<String>> analyzedData;
	static String[] qqpList;
	static String[] elseList;
	

	public ManqProblem() {
		super(1, nObjs, nObjs);

	}

	@Override
	public void evaluate(Solution solution) {
		boolean[] d = EncodingUtils.getBinary(solution.getVariable(0));
		double[] f = new double[nObjs];

		String query = "";
		double queryLen = 0.0;
		String initialQuery = "";
		double initQueryLen = 0.0;
		for (int i = 0; i < nItems; i++) {
			if (d[i]) {
				queryLen++;
				query = query + termList[i] + " ";
			}
			initQueryLen++;
			initialQuery = initialQuery+termList[i]+" ";
		}
		query = Preprocessor.removeIrreBlank(query);
		initialQuery = Preprocessor.removeIrreBlank(initialQuery);
		if (query.length() < 2) {
			for(int i = 0 ; i < nObjs; i++)
				f[i] = 0.0;
		} else {
			ArrayList<Double> qqpValues = QueryEvaluator.getQQP(query, termStatistics, qqpList);
			ArrayList<Double> elseValues = QueryEvaluator.getElse(query, analyzedData, elseList);
			for(int i = 0 ; i<qqpValues.size(); i++)
				if(Double.isFinite(qqpValues.get(i)))
					f[i] = qqpValues.get(i);
				else
					f[i] = 0.0;
			
			for(int i = 0 ; i<elseValues.size(); i++)
				if(Double.isFinite(elseValues.get(i)))
					f[qqpList.length+i] = elseValues.get(i);
				else
					f[i] = 0.0;
				
			
			f[nObjs-2] = QueryEvaluator.cosineSimilarity(query, initialQuery);
			f[nObjs-1] = 1.0 - (queryLen/initQueryLen);
			
			
			
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
		ManqProblem.nObjs = nObjs;
	}

	public static int getnItems() {
		return nItems;
	}

	public static void setnItems(int nItems) {
		ManqProblem.nItems = nItems;
	}

	public static String[] getTermList() {
		return termList;
	}

	public static void setTermList(String[] termList) {
		ManqProblem.termList = termList;
	}

	public static HashMap<String, HashMap<String, Object>> getTermStatistics() {
		return termStatistics;
	}

	public static void setTermStatistics(HashMap<String, HashMap<String, Object>> termStatistics) {
		ManqProblem.termStatistics = termStatistics;
	}

	public static HashMap<String, HashSet<String>> getAnalyzedData() {
		return analyzedData;
	}

	public static void setAnalyzedData(HashMap<String, HashSet<String>> analyzedData) {
		ManqProblem.analyzedData = analyzedData;
	}

	public static String[] getQqpList() {
		return qqpList;
	}

	public static void setQqpList(String[] qqpList) {
		ManqProblem.qqpList = qqpList;
	}

	public static String[] getElseList() {
		return elseList;
	}

	public static void setElseList(String[] elseList) {
		ManqProblem.elseList = elseList;
	}

	
}
