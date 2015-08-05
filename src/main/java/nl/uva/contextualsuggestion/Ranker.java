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
import java.util.HashSet;
import static nl.uva.settings.Config.configFile;

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
            }
            else
                System.out.println("ERR: No rating! for docID: " + docID);

            Prefrence pr = null;
            if (jobjectIterator.getAsJsonArray("tags") == null) {
                pr = new Prefrence(docID, rating);
            }
            else{
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
        String inputProfiles = configFile.getProperty("USERS");
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
        while ((line = br.readLine()) != null){
            User user = this.GetUserInfo(line);
        }
    }
    
    public static void main(String[] args) throws IOException {
        Ranker r = new Ranker();
        r.main();
        System.out.println("...");
    }

}
