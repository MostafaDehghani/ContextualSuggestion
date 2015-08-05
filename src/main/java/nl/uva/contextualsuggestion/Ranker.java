/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.contextualsuggestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import nl.uva.lm.SmoothedLM;
import nl.uva.lm.StandardLM;
import nl.uva.lucenefacility.IndexInfo;
import static nl.uva.settings.Config.configFile;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Mostafa Dehghani
 */
public class Ranker {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Ranker.class.getName());

    public User GetUserInfo(String Line) throws FileNotFoundException, IOException {

        UserInfo ui = new UserInfo(Line);
        String uId = ui.getProfileID();
        HashSet<String> suggestioncandidates = ui.getSuggestionCandidates();
        HashSet<Prefrence> Prefrences = new HashSet<>();

        JsonArray jarray = ui.getPreferences();
        for (int i = 0; i < jarray.size(); i++) {
            Double rating = null;
            JsonObject jobjectIterator = jarray.get(i).getAsJsonObject();
            String docID = jobjectIterator.get("documentId").toString().replaceAll("\"", "").trim();

            if (jobjectIterator.get("rating") != null) {
                rating = Double.parseDouble(jobjectIterator.get("rating").toString());
            } else {
                System.out.println("ERR: No rating! for docID: " + docID);
            }

            Prefrence pr = null;
            if (jobjectIterator.getAsJsonArray("tags") == null) {
                pr = new Prefrence(docID, rating);
            } else {
                JsonArray jarray2 = jobjectIterator.getAsJsonArray("tags");
                HashSet<String> tags = new HashSet<>();
                for (int j = 0; j < jarray2.size(); j++) {
                    tags.add(jarray2.get(j).toString());
                }
                pr = new Prefrence(docID, rating, tags);
            }
            Prefrences.add(pr);
        }
        User user = new User(uId, Prefrences, suggestioncandidates);
        return user;
    }

    public void main() throws FileNotFoundException, IOException {

        String field = "TEXT";
        String indexPathString = configFile.getProperty("INDEX_PATH");
        Path ipath = FileSystems.getDefault().getPath(indexPathString);
        IndexReader ireader = DirectoryReader.open(FSDirectory.open(ipath));
        IndexInfo iInfo = new IndexInfo(ireader);
        CollectionSLM CLM = new CollectionSLM(ireader, field);

        String inputProfiles = configFile.getProperty("USERS");
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        while ((line = br.readLine()) != null) {
            User user = this.GetUserInfo(line);
            HashMap<String,Double> scores = new HashMap<>();
            for (String candidate : user.suggestionCandidates) {
                Integer indexId = iInfo.getIndexId(candidate);
                LanguageModel candidateSLM = new StandardLM(ireader, indexId, field);
                SmoothedLM candidateSLM_smoothed = new SmoothedLM(candidateSLM,CLM);
                SmoothedLM PM_PLMsmoothed  = new SmoothedLM(user.userPositiveMixturePLM,CLM);
                SmoothedLM NM_PLMsmoothed  = new SmoothedLM(user.userNegativeMixturePLM,CLM);
                SmoothedLM PT_PLMsmoothed  = new SmoothedLM(user.userPositiveMixtureTags,CLM);
                SmoothedLM NT_PLMsmoothed  = new SmoothedLM(user.userNegativeMixtureTags,CLM);
                Divergence div = new Divergence();
                Double score1 = div.JsdScore(candidateSLM_smoothed, PM_PLMsmoothed);
                Double score2 = div.JsdScore(candidateSLM_smoothed, NM_PLMsmoothed);
                Double score3 = div.JsdScore(candidateSLM_smoothed, PT_PLMsmoothed);
                Double score4 = div.JsdScore(candidateSLM_smoothed, NT_PLMsmoothed);
                Double FinalScore = score1 - score2 + score3 - score4;
                scores.put(candidate, FinalScore);
            }
            List<Map.Entry<String, Double>> sortedCandidates = sortByValues(scores, false);
        }
    }

    public static void main(String[] args) throws IOException {
        Ranker r = new Ranker();
        r.main();
        System.out.println("...");
    }

}
