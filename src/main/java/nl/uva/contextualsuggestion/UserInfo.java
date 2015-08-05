package nl.uva.contextualsuggestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashSet;

public class UserInfo {

    String profileJson;

    public UserInfo(String profileJson) {
        this.profileJson = profileJson;
    }

    public HashSet<String> getSuggestionCandidates() {
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("candidates");
        HashSet<String> suggestionCandidatesArray = new HashSet<>();
        for (int i = 0; i < jarray.size(); i++) {
            suggestionCandidatesArray.add(jarray.get(i).toString().replaceAll("\"", "").trim());
        }
        return suggestionCandidatesArray;
    }

    public JsonArray getPreferences() {
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("body");
        JsonObject jPersonObject = jobject.getAsJsonObject("person");
        JsonArray jarray = jPersonObject.getAsJsonArray("preferences");

        return jarray;
    }

    public String getProfileID() {
        String profileID = "";
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("body");
        JsonObject jPersonObject = jobject.getAsJsonObject("person");

        profileID = jPersonObject.get("id").toString();

        return profileID;
    }
}
