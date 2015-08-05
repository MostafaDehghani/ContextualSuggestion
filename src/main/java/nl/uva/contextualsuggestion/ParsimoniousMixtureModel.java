/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.contextualsuggestion;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map.Entry;
import nl.uva.lm.LanguageModel;
import nl.uva.lm.CollectionSLM;
import nl.uva.lm.MixtureLM;
import nl.uva.lm.ParsimoniousLM;
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
public final class ParsimoniousMixtureModel extends LanguageModel {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ParsimoniousMixtureModel.class.getName());
    
    public ParsimoniousMixtureModel(Double rateSum, HashSet<Prefrence> ps) throws IOException {
        String field = "TEXT";
        String indexPathString = configFile.getProperty("INDEX_PATH");
        Path ipath = FileSystems.getDefault().getPath(indexPathString);
        IndexReader ireader = DirectoryReader.open(FSDirectory.open(ipath));
        IndexInfo iInfo = new IndexInfo(ireader);
        HashSet<Entry<Double,LanguageModel>> prob_pref = new HashSet<>();
        for(Prefrence p:ps){
            Integer indexId = iInfo.getIndexId(p.docID);
            LanguageModel docSLM = new StandardLM(ireader,indexId,field);
            Double prob = p.rate / rateSum;
            Entry<Double,LanguageModel> e = new AbstractMap.SimpleEntry(prob,docSLM);
            prob_pref.add(e);
        }
        CollectionSLM CLM = new CollectionSLM(ireader, field);
        MixtureLM MLM = new MixtureLM(prob_pref);
        this.setModel(new ParsimoniousLM(CLM, MLM).getModel());
    }
}
