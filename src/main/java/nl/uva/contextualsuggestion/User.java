/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.uva.contextualsuggestion;

import java.util.HashMap;
import java.util.HashSet;
import nl.uva.lm.LanguageModel;

/**
 *
 * @author Mostafa Dehghani
 */
public class User {
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(User.class.getName());
    public String uId;
    public HashSet<Suggestion> ratedSuggestion;
    public HashSet<String> suggestionCandidates;
    
    public LanguageModel userPositiveMixtureLM;
    public LanguageModel userNegativeMixtureLM;
    
    public LanguageModel userPositiveMixtureTags;
    public LanguageModel userNegativeMixtureTags; 
    
    
}
