/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.contextualsuggestion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import nl.uva.lm.CollectionSLM;
import nl.uva.lm.Divergence;
import nl.uva.lm.LanguageModel;
import static nl.uva.lm.LanguageModel.sortByValues;
import nl.uva.lm.SmoothedLM;
import nl.uva.lm.StandardLM;
import nl.uva.lucenefacility.IndexInfo;
import static nl.uva.settings.Config.configFile;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Mostafa Dehghani
 */
public class Ranker_2014 {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Ranker_2014.class.getName());
    private String indexPathString;
    private Path ipath;
    private IndexReader ireader;
    private IndexInfo iInfo;
    public HashMap<String, HashMap<String, HashSet<String>>> test_user_city_suggestions = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> train_user_suggestions_rate = new HashMap<>();

    public Ranker_2014() throws IOException {
        indexPathString = configFile.getProperty("INDEX_PATH");
        ipath = FileSystems.getDefault().getPath(indexPathString);
        ireader = DirectoryReader.open(FSDirectory.open(ipath));
        iInfo = new IndexInfo(ireader);
    }

    public void loadTrain() throws FileNotFoundException, IOException {
        String inputProfiles = "train2014_2.txt";
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        while ((line = br.readLine()) != null) {
            String[] lineArray = line.split("\\s+");
            String uId = lineArray[0];
            HashMap<String, Double> sug_rat = new HashMap<>();
            for (String s : lineArray[1].split(",")) {
                String[] lineArray2 = s.split(":");
                if (iInfo.getIndexId(lineArray2[0]) == null) {
                    System.out.println(lineArray2[0]);
                    continue;
                }
                sug_rat.put(lineArray2[0], Double.parseDouble(lineArray2[1]));
            }
            train_user_suggestions_rate.put(uId, sug_rat);
        }
    }

    public void main() throws FileNotFoundException, IOException {
        File f = new File("response_2014.json");
        f.delete();
        File f2 = new File("response-treceval_2014.res");
        f2.delete();
        String field = "TEXT";
        indexPathString = configFile.getProperty("INDEX_PATH");
        ipath = FileSystems.getDefault().getPath(indexPathString);
        ireader = DirectoryReader.open(FSDirectory.open(ipath));
        iInfo = new IndexInfo(ireader);
        Integer cnt = 1;
        this.loadTests();
        this.loadTrain();
        CollectionSLM CLM = new CollectionSLM(ireader, field);
        for (Entry<String, HashMap<String, HashSet<String>>> e : this.test_user_city_suggestions.entrySet()) {
            for (Entry<String, HashSet<String>> e1 : e.getValue().entrySet()) {
                String reqId = e.getKey() + "," + e1.getKey();
                String userId = e.getKey();
                HashSet<Prefrence> pres = new HashSet<>();
                System.out.println(e1.getKey());
                for (Entry<String, Double> e3 : train_user_suggestions_rate.get(e.getKey()).entrySet()) {
                    Prefrence p = new Prefrence(e3.getKey(), e3.getValue());
                    pres.add(p);
                }
                User user = new User(reqId, userId, pres, e1.getValue());

                HashMap<String, Double> scores = new HashMap<>();
                for (String candidate : user.suggestionCandidates) {
                    Integer indexId = iInfo.getIndexId(candidate);
                    LanguageModel candidateSLM = new StandardLM(ireader, indexId, field);

                    
                    SmoothedLM candidateSLM_smoothed = new SmoothedLM(candidateSLM, CLM);
                    SmoothedLM PM_PLMsmoothed = new SmoothedLM(user.userPositiveMixturePLM, CLM);
                    SmoothedLM NM_PLMsmoothed = new SmoothedLM(user.userNegativeMixturePLM, CLM);
//                    Divergence div1 = new Divergence(candidateSLM_smoothed, PM_PLMsmoothed);
//                    Double score1 = div1.getJsdSimScore();
                    Divergence div2 = new Divergence(candidateSLM_smoothed, NM_PLMsmoothed);
                    Double score2 = div2.getJsdSimScore();

//                    ParsimoniousLM PLM = new ParsimoniousLM(PM_PLMsmoothed, NM_PLMsmoothed);
//                    SmoothedLM PLM_smoothed = new SmoothedLM(PLM, CLM);
//                    Divergence div1 = new Divergence(candidateSLM_smoothed, PLM_smoothed);
//                    Double score1 = div1.getJsdSimScore();
                    
//                    ParsimoniousLM PLM = new ParsimoniousLM(user.userPositiveMixturePLM, user.userNegativeMixturePLM);
//                    Divergence div1 = new Divergence(candidateSLM, PLM);
//                    Double score1 = div1.getJsdSimScore();
                    
                    
//                    Divergence div1 = new Divergence(candidateSLM, user.userPositiveMixturePLM);
//                    Double score1 = div1.getJsdSimScore();
//                    Divergence div2 = new Divergence(candidateSLM, user.userNegativeMixturePLM);
//                    Double score2 = div2.getJsdSimScore();
                    Double FinalScore =  -1 * score2;
                    scores.put(candidate, FinalScore);
                }
                List<Map.Entry<String, Double>> sortedCandidates = sortByValues(scores, false);
                System.out.println("Request: " + cnt++ + " - " + user.reqId);
//            System.out.println(sortedCandidates.toString());
//                this.OutputGenerator(user.reqId, sortedCandidates);
                this.OutputGenerator_trecEval(user.reqId, sortedCandidates);

            }
        }

    }

    private void OutputGenerator(String rId, List<Map.Entry<String, Double>> sortedCandidates) throws IOException {
        JSONObject obj = new JSONObject();
        JSONObject body = new JSONObject();
        JSONArray suggestions = new JSONArray();
        for (Map.Entry<String, Double> e : sortedCandidates) {
            suggestions.add(e.getKey());
        }
        body.put("suggestions", suggestions);
        obj.put("body", body);
        obj.put("groupid", "UvA");
        obj.put("id", new Integer(rId));
        obj.put("runid", "runid");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("response.json", true)));
        out.println(obj);
        out.close();
//        System.out.print(obj);
    }

    private void OutputGenerator_trecEval(String rId, List<Map.Entry<String, Double>> sortedCandidates) throws IOException {
        //query-number    Q0  document-id rank    score   Exp
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("response-treceval_2014_n_smoothed.res", true)));
        Integer rank = 1;
        for (Map.Entry<String, Double> e : sortedCandidates) {
            String line = rId + " Q0 " + e.getKey() + " " + rank + " " + e.getValue() + " Exp";
            out.println(line);
            rank++;
        }
        out.close();
    }

    public static void main(String[] args) throws IOException {
        Ranker_2014 r = new Ranker_2014();
        r.main();
//        r.refineTests();
        System.out.println("...");
    }

    public void loadTests() throws FileNotFoundException, IOException {
        String inputProfiles = "test2014.qrel";
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        while ((line = br.readLine()) != null) {
            String uId = line.split(",")[0];
            String cityId = line.split(",")[1].split("\\s+")[0];
            HashMap<String, HashSet<String>> citySug = test_user_city_suggestions.get(uId);
            if (citySug == null) {
                citySug = new HashMap<>();
            }
            HashSet<String> sugg = citySug.get(cityId);
            if (sugg == null) {
                sugg = new HashSet<>();
            }
//            if(uId.equals("711") && sugg.contains(line.split(",")[1].split("\\s+")[2]))
//                System.out.println(line.split(",")[1].split("\\s+")[2]);
            if (iInfo.getIndexId(line.split(",")[1].split("\\s+")[2]) == null) {
                System.out.println(line.split(",")[1].split("\\s+")[2]);
                continue;
            }
            sugg.add(line.split(",")[1].split("\\s+")[2]);
            citySug.put(cityId, sugg);
            test_user_city_suggestions.put(uId, citySug);
        }
        System.out.println("");
    }

    public void refineTests() throws FileNotFoundException, IOException {
        String inputProfiles = "test2014.txt";
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("test2014.qrel", true)));
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        TreeMap<String, String> lines = new TreeMap<>();
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\\s+");
            String key = arr[0] + " " + arr[1] + " " + arr[2];
            String val = arr[3];
            lines.put(key, val);
        }
        for (Entry<String, String> e : lines.entrySet()) {
            out.println(e.getKey() + " " + e.getValue());
        }
        out.close();
    }
}
