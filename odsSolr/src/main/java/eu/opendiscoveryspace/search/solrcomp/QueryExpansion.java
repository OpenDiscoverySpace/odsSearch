package eu.opendiscoveryspace.search.solrcomp;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.internal.csv.CSVParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * A simple class to convert some extra parameters passed by the drupal into additional queries.
 */
public class QueryExpansion extends QueryComponent implements SearchConstants {

    @Override
    public void init(NamedList args) {
        super.init(args);
        preferredMediaTypes = new HashMap<String, List<String>>();
        preferredLRTypes = new HashMap<String, List<String>>();
        competencyKeys = new TreeSet<String>();
        String resourceName = "all-media-types.txt";

        try { // read all media-types in ram
            IOUtils.readLines(this.getClass().getResourceAsStream(resourceName));
        } catch(Exception x) {
            System.err.println("Sadly, failed processing " + resourceName);
            x.printStackTrace();
        }

        resourceName = "CP and ODS AP connection -AL1.csv";
        try {
            CSVParser parser = new CSVParser(new InputStreamReader(this.getClass().getResourceAsStream(resourceName), "utf-8"));
            String[] line;
            while((line=parser.getLine())!=null) {
                if(line.length==0) continue;
                String key = line[0].toLowerCase();
                key = "c_" + key;
                competencyKeys.add(key);
                preferredMediaTypes.put(key, Arrays.asList(line[1].split(",\\s*")));
                preferredLRTypes.put(key, Arrays.asList(line[1].split(",\\s*")));
            }
        } catch (Exception e) {
            System.err.println("Sadly, failed processing " + resourceName);
            e.printStackTrace();
        }

        System.err.println("Init finished. Values: ");
        System.err.println("preferredMediaTypes: " + preferredMediaTypes);
        System.err.println("preferredLRTypes: " + preferredLRTypes);
    }

    Map<String, List<String>> preferredMediaTypes, preferredLRTypes;
    Set<String> competencyKeys;

    public void prepare(ResponseBuilder rb) throws IOException {
        SolrParams params = null;
        try {
            // make params modifiable
            params = rb.req.getParams();
            dumpQueryParams(params);
            ModifiableSolrParams myparams = new ModifiableSolrParams(params);
            rb.req.setParams(myparams);


            // sorting

            String sort = myparams.get("sort","score");
            // here could, could be reparametrizing some of the sort params (e.g. to a field dedicated to sorting)
            String dir = myparams.get("dir",null);
            if(dir==null) {
                if("score".equals(sort) || "updated".equals(sort)) dir = "DESC";
                else dir = "ASC";
            }
            if(!sort.contains(" ")) sort = sort + " " + dir.toLowerCase();
            myparams.set("sort", sort);
            System.err.println("Sort is now " + sort);


            // let the configured query-parser act (word boundaries, query-parser syntax)
            BooleanQuery bQuery = null;

        /* if(text!=null)
            myparams.set(CommonParams.Q, "rawInput:(" + text + ")");
        else
            myparams.set(CommonParams.Q, "*:*"); */

            // choose the appropriate fields to query to
            String qf = params.get("qf");
            String currentLang = params.get("odsLang", "en");
            System.err.println("QF earlier: " + qf);
            qf = "text_ws^2.0 "
                    + "i18n_label_"+ currentLang + "^1.5 "
                    + "i18n_content_" + currentLang + "^1.2 "
                    + "i18n_taxonomy_names_" + currentLang + "^1.1 "
                    + "i18n_tags_" + currentLang + "^1.1 ";
            // repo? (path alias? site?) URL parts? keywords? tags? author names? year?  phonetic? i18n_teaser? (to obtain highlight)
            System.err.println("QF now: " + qf);
            myparams.add("qf", qf);


            super.prepare(rb);
            Query parsedQuery1 = rb.getQuery();
            System.err.println("Query parsed as : " + parsedQuery1);
            if(parsedQuery1 instanceof BooleanQuery)
                bQuery=(BooleanQuery) parsedQuery1;
            else if("".equals(params.get(CommonParams.Q)) || "*:*".equals(params.get(CommonParams.Q))) { // parsedQuery1 instanceof TermQuery && ((TermQuery)parsedQuery1).getTerm().text().startsWith("*")
                bQuery = new BooleanQuery();
            } else {
                bQuery = new BooleanQuery();
                parsedQuery1.setBoost(5.0f);
                bQuery.add(parsedQuery1, MUST);
            }
            removeQueryInFields(bQuery,
                    new HashSet<String> (Arrays.asList("content", "label", "taxonomy_names", "tos_name","ts_comments")));



            BooleanQuery favoringQuery = new BooleanQuery();
            if(sort.startsWith("score")) {
                // add weighting queries for competencies
                if(preferredLRTypes==null) init(new NamedList());
                // currently disabled
                //addCompetencyProfileFavoringQueries(params, favoringQuery, preferredMediaTypes, "sm_vid_ODS_AP_Technical_Format");
                //addCompetencyProfileFavoringQueries(params, favoringQuery, preferredLRTypes, "sm_vid_ODS_Educational_Resource_Type");

                // add weighting queries for current and own language
                addLanguageFavoringQueries(params, favoringQuery);
            }


            if(favoringQuery.getClauses().length>0)
                bQuery.add(favoringQuery, BooleanClause.Occur.SHOULD);
            rb.setQuery(bQuery);
            System.err.println("Query is now: " + rb.getQuery());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    private boolean removeQueryInFields(Query q, Set<String> fieldNames) {
        if(q instanceof TermQuery) return removeQueryInFields(((TermQuery) q), fieldNames);
        else if(q instanceof BooleanQuery) return removeQueryInFields(((BooleanQuery) q), fieldNames);
        else if(q instanceof DisjunctionMaxQuery)return removeQueryInFields(((DisjunctionMaxQuery) q), fieldNames);
        else return false;
    }

    private boolean removeQueryInFields(TermQuery tq, Set<String> fieldNames) {
        if (fieldNames.contains(tq.getTerm().field())) return true;
        return false;
    }


    private boolean removeQueryInFields(DisjunctionMaxQuery q, Set<String> fieldNames) {
        Iterator<Query> qit = q.getDisjuncts().iterator();
        while(qit.hasNext()) {
            Query cq = qit.next();
            boolean removed = removeQueryInFields(cq, fieldNames);
            if(removed) qit.remove();
        }
        return q.getDisjuncts().isEmpty();
    }
    private boolean removeQueryInFields(BooleanQuery q, Set<String> fieldNames) {
        Iterator<BooleanClause> qit = q.clauses().iterator();
        while(qit.hasNext()) {
            BooleanClause clause = qit.next();
            boolean removed = removeQueryInFields(clause.getQuery(), fieldNames);
            if(removed) qit.remove();;
        }
        return q.clauses().isEmpty();
    }

    private String emptyToNull(String in) {
        if(in==null) return null;
        if(in.length()==0) return null;
        return in;
    }

    private void addLanguageFavoringQueries(SolrParams params, BooleanQuery favoringQuery) {
        String currentLang = params.get("odsLang", null);
        String userProfileLang = params.get("odsUserLang", null);
        currentLang = emptyToNull(currentLang);
        userProfileLang = emptyToNull(userProfileLang);

        if(currentLang==null && userProfileLang!=null) {
            currentLang = userProfileLang; userProfileLang = null;
        }

        if(currentLang==null) return;

        if(userProfileLang==null) {
            TermQuery tq = new TermQuery(new Term("ss_language",currentLang));
            tq.setBoost(2.0f); favoringQuery.add(tq, BooleanClause.Occur.SHOULD);
        } else {
            TermQuery tq = new TermQuery(new Term("ss_language",currentLang));
            tq.setBoost(2.0f); favoringQuery.add(tq, BooleanClause.Occur.SHOULD);
            tq = new TermQuery(new Term("ss_language",userProfileLang));
            tq.setBoost(1.7f); favoringQuery.add(tq, BooleanClause.Occur.SHOULD);
        }
    }

    private void addCompetencyProfileFavoringQueries(SolrParams params, BooleanQuery favoringQuery, Map<String, List<String>> preferredValues, String fieldName) {
        if(competencyKeys==null || preferredValues==null) {
            System.out.println("Abandonning addCompetencyProfileFavoringQueries... competencyKeys==null?" + (competencyKeys == null) + "...");
            return;
        }
        for(String key: competencyKeys) {
            String[] value = params.getParams(key);
            if(value==null) continue;
            //System.err.println("Checking key: " + key + " obtained \"" + Arrays.asList(value) + "\".");
            float boost = -1.0f;
            if(value.length>0 && "1".equals(value[0])) {
                boost = 1.1f;
            } else if(value.length>0 && "0".equals(value[0])) {
                boost = 0.9f;
            } else {
                // nothing to do... probably no competencies filled, ignore
            }
            //System.err.println("Boost is " + boost);
            if(boost>0  && preferredValues.get(key)!=null) for(String preferredVal: preferredValues.get(key)) {
                TermQuery tq = new TermQuery(new Term(fieldName, preferredVal));
                tq.setBoost(boost);
                //System.err.println("Adding query " + tq);
                favoringQuery.add(tq, BooleanClause.Occur.SHOULD);
            }
        }
    }


    private void dumpQueryParams(SolrParams params) {
        try {
            // dump the parameters
            System.err.println(" ========== " + new Date() + " ===============");
            Iterator<String> it = params.getParameterNamesIterator();
            while (it.hasNext()) {
                String name = it.next();
                System.err.print("Param: " + name + " :");
                for (String v : params.getParams(name)) {
                    System.err.print(" " + v);
                }
                System.err.println(".");
            }
            System.err.println("");
            System.err.println("");
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public String getDescription() {
        return "QueryComponent to handle the context of the Open Discovery Space querying person and to perform query expansion.";
    }

    @Override
    public String getSource() {
        // TODO: use a maven api to get things here one day
        return "$Source: $";
    }

    @Override
    public String getVersion() {
        return "0.1-SNAPSHOT";
    }



}
