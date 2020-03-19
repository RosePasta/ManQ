package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DataLoader {

	public static ArrayList<String> getPoorQueries() {
		ArrayList<String> bugList = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("./data/sampled_data/poorqueries.txt"));
			String str;
			while ((str = br.readLine()) != null) {
				bugList.add(str);

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bugList;
	}

	public static HashMap<String, String> getBugreport(ArrayList<String> targetBugs) {
		HashMap<String, String> bugList = new HashMap<String, String>();
		try {
			for (String bugID : targetBugs) {
				BufferedReader br = new BufferedReader(
						new FileReader("./data/sampled_data/bugreports/" + bugID + ".txt"));
				String str;
				String summary = "";
				String desc = "";
				while ((str = br.readLine()) != null) {
					if (summary.equals("")) {
						summary = str;
					} else {
						desc = desc + str.replace("\n", "") + " ";
					}
				}
				bugList.put(bugID, summary + ".\n" + desc);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bugList;
	}

	public static HashMap<String, String> getBaseline(ArrayList<String> targetBugs) {
		HashMap<String, String> bugList = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("./data/sampled_data/baseline.txt"));
			String str;
			while ((str = br.readLine()) != null) {
				String bugID = str.split("\t")[0];
				if (targetBugs.contains(bugID))
					;
				bugList.put(bugID, str.split("\t", 2)[1].toLowerCase());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bugList;
	}

	public static HashMap<String, String> getTechniqueQuery(ArrayList<String> targetBugs, String technique) {
		HashMap<String, String> bugList = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("./data/sampled_data/" + technique + ".txt"));
			String str;
			while ((str = br.readLine()) != null) {
				String bugID = str.split("\t")[0];
				if (targetBugs.contains(bugID))
					;
				bugList.put(bugID, str.split("\t", 2)[1].toLowerCase());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bugList;
	}

	public static HashMap<String, ArrayList<String>> getGoldset(ArrayList<String> targetBugs) {
		HashMap<String, ArrayList<String>> goldSetMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> indexMap = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("./data/sampled_data/index/key_file.ckeys"));
			String str;
			while ((str = br.readLine()) != null) {
				String index = str.split("\\:")[0];
				String fileName = str.split("ssystems\\\\tomcat70\\\\")[1].replace("\\", "/");
				indexMap.put(fileName, index);
			}
			for (String bugID : targetBugs) {
				br = new BufferedReader(new FileReader("./data/sampled_data/goldset/" + bugID + ".txt"));
				ArrayList<String> goldIDList = new ArrayList<String>();
				while ((str = br.readLine()) != null) {
					String index = indexMap.get(str);
					if (index != null) {
						goldIDList.add(index);
					}
				}
				goldSetMap.put(bugID, goldIDList);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return goldSetMap;
	}

	public static HashMap<String, HashMap<String, Object>> getQQPData() {
		HashMap<String, HashMap<String, Object>> results = new HashMap<String, HashMap<String, Object>>();

		try {
			BufferedReader br = new BufferedReader(new FileReader("./data/sampled_data/qqp_data.txt"));
			String str;
			while ((str = br.readLine()) != null) {
				String[] data = str.split("\t");

				String term = data[0];
				double var = Double.parseDouble(data[1]);
				double scq = Double.parseDouble(data[2]);
				double prob1 = Double.parseDouble(data[3]);
				double prob2 = Double.parseDouble(data[4]);
				double prob3 = Double.parseDouble(data[5]);
				double idf = Double.parseDouble(data[6]);
				double ictf = Double.parseDouble(data[7]);
				double ent = Double.parseDouble(data[8]);
				String fileListText = data[9].replace("[", "").replace("]", "").replace(" ", "");

				HashMap<String, Object> qqp = new HashMap<String, Object>();
				qqp.put("VAR", var);
				qqp.put("SCQ", scq);
				qqp.put("PROB_T", prob1);
				qqp.put("PROB_D", prob3);
				qqp.put("IDF", idf);
				qqp.put("ICTF", ictf);
				qqp.put("ENT", ent);
				qqp.put("DOCS", fileListText);
				results.put(term, qqp);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

}
