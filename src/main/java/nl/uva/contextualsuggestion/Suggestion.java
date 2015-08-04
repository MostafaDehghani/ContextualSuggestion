/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.uva.contextualsuggestion;

import java.util.HashSet;
import java.util.Objects;
import nl.uva.lm.LanguageModel;

/**
 *
 * @author Mostafa Dehghani
 */
public class Suggestion {
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Suggestion.class.getName());
    public String docID;
    public Integer rate;
    public HashSet<String> tags;
    public LanguageModel docSLM;

    public Suggestion(String docID, Integer rate) {
        this.docID = docID;
        this.rate = rate;
    }

    public Suggestion(String docID, Integer rate, HashSet<String> tags) {
        this.docID = docID;
        this.rate = rate;
        this.tags = tags;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.docID);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Suggestion other = (Suggestion) obj;
        if (!Objects.equals(this.docID, other.docID)) {
            return false;
        }
        return true;
    }

    
    
}
