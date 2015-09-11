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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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
            qf = "tws_label_"+currentLang+"^2.0 "
                    + "i18n_label_"+ currentLang + "^1.5 "
                    + "label^1.2 " +
                 "twm_content_" + currentLang + "^1.5 "
                    + "i18n_content_" + currentLang + "^1.2 "
                    + "content_background^1.1 " +
                  "i18n_teaser_" + currentLang + "^1.2 " + " teaser^1.1 " ;
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
            //removeQueryInFields(bQuery,
            //        new HashSet<String> (Arrays.asList("content", "label")));
            // taxonomy_names: i18n_tm_${lang}_topic , "taxonomy_names", "tos_name","ts_comments"



            BooleanQuery favoringQuery = new BooleanQuery();
            if(sort.startsWith("score")) {
                // add weighting queries for competencies
                if(preferredLRTypes==null) init(new NamedList());
                addCompetencyProfileFavoringQueries(params, favoringQuery, preferredMediaTypes, "sm_vid_ODS_AP_Technical_Format");
                addCompetencyProfileFavoringQueries(params, favoringQuery, preferredLRTypes, "sm_vid_ODS_Educational_Resource_Type");

                // add weighting queries for current and own language
                addLanguageFavoringQueries(params, favoringQuery);

                // add weighting queries for higher ranked resources
                addRatingFavoringQueries(params, favoringQuery);

                // add weighting queries for newer resources
                addRecencyFavoringQueries(params, favoringQuery);
            }


            if(favoringQuery.getClauses().length>0)
                bQuery.add(favoringQuery, SHOULD);
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
            if(removed) qit.remove();
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
            tq.setBoost(2.0f); favoringQuery.add(tq, SHOULD);
        } else {
            TermQuery tq = new TermQuery(new Term("ss_language",currentLang));
            tq.setBoost(2.0f); favoringQuery.add(tq, SHOULD);
            tq = new TermQuery(new Term("ss_language",userProfileLang));
            tq.setBoost(1.7f); favoringQuery.add(tq, SHOULD);
        }
    }

    private void addCompetencyProfileFavoringQueries(SolrParams params, BooleanQuery favoringQuery, Map<String, List<String>> preferredValues, String fieldName) {
        BooleanQuery bq = new BooleanQuery();
        favoringQuery.add(bq, SHOULD);
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
                boost = 1.0005f;
            } else if(value.length>0 && "0".equals(value[0])) {
                boost = 0.9995f;
            } else {
                // nothing to do... probably no competencies filled, ignore
            }
            //System.err.println("Boost is " + boost);
            if(boost>0  && preferredValues.get(key)!=null) for(String preferredVal: preferredValues.get(key)) {
                TermQuery tq = new TermQuery(new Term(fieldName, preferredVal));
                tq.setBoost(boost);
                //System.err.println("Adding query " + tq);
                bq.add(tq, SHOULD);
            }
        }
    }


    private void addRatingFavoringQueries(SolrParams params, BooleanQuery favoringQuery) {
        class RR {
            int ratingMin, ratingMax;
            float boost;
            RR(int r1, int r2, float b) {
                ratingMin=r1; ratingMax=r2;
                boost=b;
            }
        }
        class RS {
            RR[] rr;
            int countMin, countMax;
            RS(int c1, int c2, RR[] r) {
                rr = r;
                countMin = c1; countMax=c2;
            }
        }
        RS[] ranges = new RS[] {
                new RS(  1,2,   new RR[]{ new RR(1,20, 0.9995f), new RR(21,40, 0.9997f), new RR(41,60, 1.0001f), new RR(61,80, 1.0003f), new RR(81,99, 1.0005f)}),
                new RS(  3,6,   new RR[]{ new RR(1,20, 0.9990f), new RR(21,40, 0.9995f), new RR(41,60, 1.0001f), new RR(61,80, 1.0005f), new RR(81,99, 1.0010f)}),
                new RS(  7,15,  new RR[]{ new RR(1,20, 0.9985f), new RR(21,40, 0.9992f), new RR(41,60, 1.0001f), new RR(61,80, 1.0008f), new RR(81,99, 1.0015f)}),
                new RS( 15,30,  new RR[]{ new RR(1,20, 0.9980f), new RR(21,40, 0.9999f), new RR(41,60, 1.0001f), new RR(61,80, 1.0010f), new RR(81,99, 1.0020f)})
        };

        BooleanQuery favoringQ = new BooleanQuery();
        favoringQuery.add(favoringQ, SHOULD);
        NumberFormat formatter = DecimalFormat.getNumberInstance();

        for(RS countRange: ranges) {
            BooleanQuery pair = new BooleanQuery();
            favoringQ.add(pair, SHOULD);
            BooleanQuery countQueries = new BooleanQuery();
            BooleanQuery ratingQueries = new BooleanQuery();
            pair.add(countQueries, MUST);
            pair.add(ratingQueries, MUST);

            for(int c=countRange.countMin, max=countRange.countMax; c<max; c++) {
                countQueries.add(new TermQuery(new Term("ratingCount", formatter.format(c))), SHOULD);
            }

            for(RR ratingRange: countRange.rr) {
                BooleanQuery bq = new BooleanQuery();
                for(int r=ratingRange.ratingMin, m=ratingRange.ratingMax; r<m; r++ ) {
                    bq.add(new TermQuery(new Term("ratingMean", formatter.format(r))), SHOULD);
                }
                bq.setBoost(ratingRange.boost);
                ratingQueries.add(bq, SHOULD);
            }
        }
    }

    private String cutWithStar(String x, int pos) {
        return x.substring(0, pos) + "*";
    }

    static ThreadLocal<DateFormat> dfTL = new ThreadLocal<DateFormat>(){
        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            return df;
        }
    };

    private void addUdpatedQuery(Calendar cal, int cut, float boostBonus, BooleanQuery bq) {
        String t = dfTL.get().format(cal.getTime());
        t = t.substring(0, cut)+"*";
        Query  query = new WildcardQuery(new Term("updated", t));
        query.setBoost(1.0f+boostBonus);
        bq.add(query, SHOULD);
    }
    private void addRecencyFavoringQueries(SolrParams params, BooleanQuery favoringQuery) {
        BooleanQuery fq = new BooleanQuery();
        favoringQuery.add(fq, SHOULD);
        // typical format 2011-05-17T22:07:37.984Z
        Calendar cal = GregorianCalendar.getInstance();

        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);
        // in the current minute
        addUdpatedQuery(cal, 17, 0.05f, fq );

        // in the last 5 minutes
        for(int i=0; i<5; i++) {
            cal.setTimeInMillis(now-i*minute);
            addUdpatedQuery(cal, 17, 0.03f, fq );
        }

        // in the current hour
        addUdpatedQuery(cal, 14, 0.01f, fq );

        // in the last 6 hours
        for(int i=1; i<=6; i++) {
            cal.setTimeInMillis(now-i*hour);
            addUdpatedQuery(cal, 14, 0.005f, fq );
        }

        // in the current day
        addUdpatedQuery(cal, 11, 0.002f, fq );

        // in the last week
        for(int i=1; i<=7; i++) {
            cal.setTimeInMillis(now-i*day);
            addUdpatedQuery(cal, 11, 0.001f, fq );
        }

        // in the current month
        addUdpatedQuery(cal, 8, 0.002f, fq );

        // in the current year
        addUdpatedQuery(cal, 5, 0.0005f, fq );

        // in the last ten years
        for(int i=1; i<=7; i++) {
            cal.setTimeInMillis(now-i*year);
            addUdpatedQuery(cal, 5, (float) Math.pow(0.0005f,i), fq );
        }

    }

    private static BooleanClause.Occur SHOULD = BooleanClause.Occur.SHOULD, MUST = BooleanClause.Occur.MUST;

    private static long now = System.currentTimeMillis();
    private static long minute = 60L*1000L, hour=60L*60L*1000L, day=24*hour, week=7*hour, month=31*day, year=365*day;
    private static float dateIncr = 0.05f;
    private static class DateRange {
        long start, end; float boost;

        DateRange(long s, long e, double b) { start =s; end=e; boost = (float) b;}
    }
    private static DateRange[] dateRanges = new DateRange[] {
            new DateRange(now, now - 5*minute, dateIncr),
            new DateRange(now-5*minute, now-15*minute, Math.pow(dateIncr,2)),
            new DateRange(now-15*minute, now-60*minute, Math.pow(dateIncr,3)),
            new DateRange(now-60*minute, now-6*hour, Math.pow(dateIncr,4)),
            new DateRange(now-6*hour, now-24*hour, Math.pow(dateIncr,5)),
            new DateRange(now-24*hour, now-2*day, Math.pow(dateIncr,6)),
            new DateRange(now-2*day, now-week, Math.pow(dateIncr,7)),
            new DateRange(now-week, now-month, Math.pow(dateIncr,8)),
            new DateRange(now-month, now-3*month, Math.pow(dateIncr,9)),
            new DateRange(now-3*month, now-year, Math.pow(dateIncr,10)),
            new DateRange(now-year, now-2*year, Math.pow(dateIncr,11)),
            new DateRange(now-2*year, now-5*year, Math.pow(dateIncr,12)),
            new DateRange(now-5*month, now-10*year, Math.pow(dateIncr,13))
    };


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
