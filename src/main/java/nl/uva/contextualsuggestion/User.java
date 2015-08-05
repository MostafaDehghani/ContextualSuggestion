/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.uva.contextualsuggestion;

import java.io.IOException;
import java.util.HashSet;
import nl.uva.lm.LanguageModel;

/**
 *
 * @author Mostafa Dehghani
 */
public class User {
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(User.class.getName());
    
    public Integer nuteralRate = 2;
    public String uId;
    public HashSet<Prefrence> ratedPrefrences;
    public HashSet<String> suggestionCandidates;
    
    public LanguageModel userPositiveMixturePLM;
    public LanguageModel userNegativeMixturePLM;
    
    public LanguageModel userPositiveMixtureTags;
    public LanguageModel userNegativeMixtureTags; 

    public User(String uId, HashSet<Prefrence> ratedPrefrences, HashSet<String> suggestionCandidates) throws IOException {
        this.uId = uId;
        this.ratedPrefrences = ratedPrefrences;
        this.suggestionCandidates = suggestionCandidates;
        this.generateUserLM();
        
    }
    
    private void generateUserLM() throws IOException{
        HashSet<Prefrence> positivePrefrences = new HashSet<>();
        HashSet<Prefrence> negativePrefrences = new HashSet<>();
        Double sumPR =0D;
        Double sumNR =0D;
        for(Prefrence p : this.ratedPrefrences){
           
            if(p.rate == -1 || p.rate == 2)
                continue;
            
            if(p.rate > 2){
                if(p.rate == 4.0)
                    p.setNewRate(2.0);
                if(p.rate == 3.0)
                    p.setNewRate(1.0);
                positivePrefrences.add(p);
                sumPR += p.rate;
            }
            
            else if(p.rate < 2){
                if(p.rate == 0.0)
                    p.setNewRate(2.0);
                if(p.rate == 1.0)
                    p.setNewRate(1.0);
                negativePrefrences.add(p);
                sumNR += p.rate;
            }
        }
        this.userPositiveMixturePLM = new ParsimoniousMixtureModel(sumPR, positivePrefrences);
        this.userNegativeMixturePLM = new ParsimoniousMixtureModel(sumNR, negativePrefrences);
        this.userPositiveMixtureTags = new MixtureTags(sumPR, positivePrefrences);
        this.userNegativeMixtureTags = new MixtureTags(sumNR, negativePrefrences);
    }
}
