package nl.uva.contextualsuggestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

public class GetUserInfo {

    private HashSet<String> getSuggestionCandidates(String profileJson) {
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("candidates");
        HashSet<String> suggestionCandidatesArray = new HashSet<>();
        for (int i = 0; i < jarray.size(); i++) {
            System.out.println(jarray.get(i));
            suggestionCandidatesArray.add(jarray.get(i).toString());
        }
        return suggestionCandidatesArray;
    }

    private JsonArray getPreferences(String profileJson) {
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("body");
        JsonObject jPersonObject = jobject.getAsJsonObject("person");
        JsonArray jarray = jPersonObject.getAsJsonArray("preferences");

        return jarray;
    }

    private String getProfileID(String profileJson) {
        String profileID = "";
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("body");
        JsonObject jPersonObject = jobject.getAsJsonObject("person");

        profileID = jPersonObject.get("id").toString();

        return profileID;
    }

    
    public GetUserInfo(String Line) throws FileNotFoundException, IOException {
        
//        String inputProfiles = configFile.getProperty("USER");
//        String line;
//        BufferedReader br = new BufferedReader(new FileReader(inputProfiles));
//        while ((line = br.readLine()) != null) {
        
        User user = new User(GetUserInfo.getProfileID(line));
        
        //System.out.println(profiles.getProfileID(line));
            /*String[] suggestioncandidates = profiles.getSuggestionCandidates(line);
             for(int i = 0; i < suggestioncandidates.length; i++){
             System.out.println(suggestioncandidates[i]);
             }
             */
            JsonArray jarray = users.getPreferences(line);
            for (int i = 0; i < jarray.size(); i++) {
                System.out.println(i + ":");
                int rating = 2;
                JsonObject jobjectIterator = jarray.get(i).getAsJsonObject();
                if (jobjectIterator.get("rating") != null) {
                    rating = Integer.parseInt(jobjectIterator.get("rating").toString());
                    System.out.println("rating: " + rating);
                }
                String docID = jobjectIterator.get("documentId").toString();
                System.out.println("docID: " + docID);
                System.out.println("---------------");
            }

        }

    }

}
