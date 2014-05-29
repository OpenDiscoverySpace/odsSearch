package eu.opendiscoveryspace.search.solrcomp;

import org.apache.lucene.search.BooleanClause;

public interface SearchConstants {

    public static final BooleanClause.Occur MUST = BooleanClause.Occur.MUST;
    public static final BooleanClause.Occur SHOULD = BooleanClause.Occur.SHOULD;
    public static final BooleanClause.Occur MUST_NOT = BooleanClause.Occur.MUST_NOT;


}
