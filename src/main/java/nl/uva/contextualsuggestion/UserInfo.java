package nl.uva.contextualsuggestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserInfo {

    String profileJson;

    public UserInfo(String profileJson) {
        this.profileJson = profileJson;
    }

    public JsonArray getSuggestionCandidates() {
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonArray jarray = jobject.getAsJsonArray("candidates");
        return jarray;
       
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

    String getRequestID() {
        String requestID = "";
        JsonElement jelement = new JsonParser().parse(profileJson);
        JsonObject jobject = jelement.getAsJsonObject();
        requestID = jobject.get("id").toString();

        return requestID;
    }
}
