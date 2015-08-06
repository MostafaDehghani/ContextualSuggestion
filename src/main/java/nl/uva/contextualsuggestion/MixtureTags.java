/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.contextualsuggestion;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import nl.uva.lm.LanguageModel;
import nl.uva.lm.MixtureLM;

/**
 *
 * @author Mostafa Dehghani
 */
public final class MixtureTags extends LanguageModel {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MixtureTags.class.getName());
    
    public MixtureTags(Double rateSum, HashSet<Prefrence> ps) throws IOException {
        HashSet<Entry<Double,LanguageModel>> prob_pref = new HashSet<>();
        for(Prefrence p:ps){
            Double prob = p.rate / rateSum;
            LanguageModel TagSLM = new LanguageModel();
            //Skip doc without tags:
            if(p.tags!=null){
                Integer numOfAllTags = p.tags.size();
                HashMap<String,Double> lm = new HashMap<>();
                for(String t: p.tags){
                    lm.put(t, 1.0 / numOfAllTags);
                }
                TagSLM = new LanguageModel(lm);
            }
            Entry<Double,LanguageModel> e = new AbstractMap.SimpleEntry(prob,TagSLM);
            prob_pref.add(e);
        }
        MixtureLM MLM = new MixtureLM(prob_pref);
        this.setModel(MLM.getModel());
    }
}
