package eu.opendiscoveryspace.search.solrcomp;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.QueryComponent;
import org.apache.solr.handler.component.ResponseBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/**
 * A simple class to convert some extra parameters passed by the drupal into additional queries.
 */
public class QueryExpansion extends QueryComponent implements SearchConstants {


    public void prepare(ResponseBuilder rb) throws IOException {
        // make params modifiable
        SolrParams params = rb.req.getParams();
        ModifiableSolrParams myparams = new ModifiableSolrParams(params);
        rb.req.setParams(myparams );


        // sorting

        String sort = myparams.get("sort","score");
        // here could, could be reparametrizing some of the sort params (e.g. to a field dedicated to sorting)
        String dir = myparams.get("dir","ASC");
        if(!sort.contains(" "))
            myparams.set("sort", sort + " " + dir.toLowerCase());


        // query terms
        String text = params.get("terms", null); // TODO: other param-name? q?

        // let the configured query-parser act (word boundaries, query-parser syntax)
        BooleanQuery parsedTextQuery = null;

        /*
        if(text!=null)
            myparams.set(CommonParams.Q, "rawInput:(" + text + ")");
        else
            myparams.set(CommonParams.Q, "*:*");
        super.prepare(rb);
        Query parsedQuery1 = rb.getQuery();
        if(parsedQuery1 instanceof BooleanQuery) parsedTextQuery=(BooleanQuery) parsedQuery1;
        else {
            parsedTextQuery = new BooleanQuery();
            parsedTextQuery.add(parsedQuery1, MUST);
        }
        if(text!=null)
            myparams.set(CommonParams.Q,parsedTextQuery.toString());
        else {
            myparams.set(CommonParams.Q, "*:*");
            parsedTextQuery = null;
        } */


        // make sure the first language is queried (odsLang) then the user language, if any, then any other language


        // dump the parameters
        System.err.println(" ========== " + new Date() + " ===============");
        Iterator<String> it = params.getParameterNamesIterator();
        while(it.hasNext()) {
            String name = it.next();
            System.err.print("Param: " + name + " :");
            for(String v: params.getParams(name)) {
                System.err.print(" " + v);
            }
            System.err.println(".");
        }
        System.err.println("");
        System.err.println("");


        // add weighting queries for competencies



    }

    @Override
    public String getDescription() {
        return "QueryComponent to handle the form-like queries specific to Curriki and to perform query expansion.";
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
