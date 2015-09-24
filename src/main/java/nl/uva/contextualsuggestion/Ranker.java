/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.contextualsuggestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import nl.uva.lm.CollectionSLM;
import nl.uva.lm.Divergence;
import nl.uva.lm.LanguageModel;
import static nl.uva.lm.LanguageModel.sortByValues;
import nl.uva.lm.ParsimoniousLM;
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
public class Ranker {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Ranker.class.getName());
    private String indexPathString;
    private Path ipath;
    private IndexReader ireader;
    private IndexInfo iInfo;

    public Ranker() throws IOException {
        indexPathString = configFile.getProperty("INDEX_PATH");
        ipath = FileSystems.getDefault().getPath(indexPathString);
        ireader = DirectoryReader.open(FSDirectory.open(ipath));
        iInfo = new IndexInfo(ireader);
    }

    public User GetUserInfo(String Line) throws FileNotFoundException, IOException {

        UserInfo ui = new UserInfo(Line);
        String uId = ui.getProfileID();
        String reqId = ui.getRequestID();
        JsonArray jarray1 = ui.getSuggestionCandidates();
        HashSet<String> suggestionCandidatesArray = new HashSet<>();
        for (int i = 0; i < jarray1.size(); i++) {
            String docID =jarray1.get(i).toString().replaceAll("\"", "").trim();
            //Filter non-crowled Docs
            if (iInfo.getIndexId(docID) == null){
                System.out.println(docID);
                continue;
            }
            suggestionCandidatesArray.add(docID);
        }
        
        HashSet<Prefrence> Prefrences = new HashSet<>();
        JsonArray jarray2 = ui.getPreferences();
        for (int i = 0; i < jarray2.size(); i++) {
            Double rating = null;
            JsonObject jobjectIterator = jarray2.get(i).getAsJsonObject();
            String docID = jobjectIterator.get("documentId").toString().replaceAll("\"", "").trim();
            
            //Filter non-crowled Docs
            if (iInfo.getIndexId(docID) == null){
                System.out.println(docID);
                continue;
            }
                
            if (jobjectIterator.get("rating") != null) {
                rating = Double.parseDouble(jobjectIterator.get("rating").toString());
            } else {
                System.out.println("ERR: No rating! for docID: " + docID);
            }

            Prefrence pr = null;
            if (jobjectIterator.getAsJsonArray("tags") == null) {
                pr = new Prefrence(docID, rating);
            } else {
                JsonArray jarray3 = jobjectIterator.getAsJsonArray("tags");
                HashSet<String> tags = new HashSet<>();
                for (int j = 0; j < jarray3.size(); j++) {
                    tags.add(jarray3.get(j).toString());
                }
                pr = new Prefrence(docID, rating, tags);
            }
            Prefrences.add(pr);
        }
        User user = new User(reqId, uId, Prefrences, suggestionCandidatesArray);
        return user;
    }

    public void main() throws FileNotFoundException, IOException {
        File f = new File("response.json");
            f.delete();
        File f2 = new File("response-treceval.res");
            f2.delete();
        String field = "TEXT";
        indexPathString = configFile.getProperty("INDEX_PATH");
        ipath = FileSystems.getDefault().getPath(indexPathString);
        ireader = DirectoryReader.open(FSDirectory.open(ipath));
        iInfo = new IndexInfo(ireader);
        CollectionSLM CLM = new CollectionSLM(ireader, field);

        String inputProfiles = configFile.getProperty("USERS");
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        Integer cnt = 1;
        while ((line = br.readLine()) != null) {
            User user = this.GetUserInfo(line);
            HashMap<String, Double> scores1 = new HashMap<>();
            HashMap<String, Double> scores2 = new HashMap<>();
            for (String candidate : user.suggestionCandidates) {
                Integer indexId = iInfo.getIndexId(candidate);
                LanguageModel candidateSLM = new StandardLM(ireader, indexId, field);
                SmoothedLM candidateSLM_smoothed = new SmoothedLM(candidateSLM, CLM);
                SmoothedLM PM_PLMsmoothed = new SmoothedLM(user.userPositiveMixturePLM, CLM);
                SmoothedLM NM_PLMsmoothed = new SmoothedLM(user.userNegativeMixturePLM, CLM);
                SmoothedLM PT_PLMsmoothed = new SmoothedLM(user.userPositiveMixtureTags, CLM);
                SmoothedLM NT_PLMsmoothed = new SmoothedLM(user.userNegativeMixtureTags, CLM);
                
                Divergence div1 = new Divergence(candidateSLM_smoothed, PM_PLMsmoothed);
                Double score1 = div1.getJsdSimScore();
//                Divergence div2 = new Divergence(candidateSLM_smoothed, NM_PLMsmoothed);
//                Double score2 = div2.getJsdSimScore();
                Divergence div3 = new Divergence(candidateSLM_smoothed, PT_PLMsmoothed);
                Double score3 = div3.getJsdSimScore();
//                Divergence div4 = new Divergence(candidateSLM_smoothed, NT_PLMsmoothed);
//                Double score4 = div4.getJsdSimScore();
                Double FinalScore1 = score1 + (2 * score3);
                

                ParsimoniousLM PLM = new ParsimoniousLM(PM_PLMsmoothed, NM_PLMsmoothed);
                SmoothedLM PLM_smoothed = new SmoothedLM(PLM, CLM);
                Divergence div1_ = new Divergence(candidateSLM_smoothed, PLM_smoothed);
                Double score1_ = div1_.getJsdSimScore();
                ParsimoniousLM PTM = new ParsimoniousLM(PT_PLMsmoothed, NT_PLMsmoothed);
                SmoothedLM PTM_smoothed = new SmoothedLM(PTM, CLM);
                Divergence div2_ = new Divergence(candidateSLM_smoothed, PTM_smoothed);
                Double score2_ = div2_.getJsdSimScore();
                Double FinalScore2 = score1_ + 2* score2_;
                
                
//                Divergence div1 = new Divergence(candidateSLM, user.userPositiveMixturePLM);
//                Double score1 = div1.getJsdSimScore();
//                Divergence div2 = new Divergence(candidateSLM, user.userNegativeMixturePLM);
//                Double score2 = div2.getJsdSimScore();
//                Divergence div3 = new Divergence(candidateSLM, user.userPositiveMixtureTags);
//                Double score3 = div3.getJsdSimScore();
//                Divergence div4 = new Divergence(candidateSLM, user.userNegativeMixtureTags);
//                Double score4 = div4.getJsdSimScore();
//                Double FinalScore = score1 - score2 + 2 * (score3 - score4);

//                ParsimoniousLM PLM = new ParsimoniousLM(user.userPositiveMixturePLM, user.userNegativeMixturePLM);
//                Divergence div1 = new Divergence(candidateSLM, PLM);
//                Double score1 = div1.getJsdSimScore();
//                ParsimoniousLM PTM = new ParsimoniousLM(user.userPositiveMixtureTags, user.userNegativeMixtureTags);
//                Divergence div2 = new Divergence(candidateSLM, PTM);
//                Double score2 = div2.getJsdSimScore();
//                Double FinalScore = score1 + 2* score2;
                
                scores1.put(candidate, FinalScore1);
                scores2.put(candidate, FinalScore2);
            }
            List<Map.Entry<String, Double>> sortedCandidates1 = sortByValues(scores1, false);
            List<Map.Entry<String, Double>> sortedCandidates2 = sortByValues(scores2, false);
            System.out.println("Request: " + cnt++ + " - " + user.reqId);
//            System.out.println(sortedCandidates.toString());
            this.OutputGenerator(user.reqId, sortedCandidates1, "response_P.json");
            this.OutputGenerator(user.reqId, sortedCandidates2, "response_PLM.json");
//            this.OutputGenerator_trecEval(user.reqId, sortedCandidates);
        }
        
    }
    
    private void OutputGenerator(String rId, List<Map.Entry<String, Double>> sortedCandidates, String FileName) throws IOException{
        JSONObject obj = new JSONObject();
        JSONObject body = new JSONObject();
            JSONArray suggestions = new JSONArray();
            for(Map.Entry<String, Double> e : sortedCandidates){
                suggestions.add(e.getKey());
            }
	body.put("suggestions", suggestions);
        obj.put("body", body);
	obj.put("groupid", "UAmsterdam");
	obj.put("id", new Integer(rId));
	obj.put("runid", "PLM");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(FileName, true)));
        out.println(obj);
        out.close();
//        System.out.print(obj);
    }
    
    private void OutputGenerator_trecEval(String rId, List<Map.Entry<String, Double>> sortedCandidates) throws IOException{
        //query-number    Q0  document-id rank    score   Exp
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("response-treceval_PLM_lenient.res", true)));
        Integer rank = 1;
        for(Map.Entry<String, Double> e : sortedCandidates){
                String line = rId + " Q0 " + e.getKey() + " " + rank + " " + e.getValue();
                out.println(line);
                rank++;
        }
        out.close();
    }

    public static void main(String[] args) throws IOException {
        Ranker r = new Ranker();
        r.main();
        System.out.println("...");
    }

}
