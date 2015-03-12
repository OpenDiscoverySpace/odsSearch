odsSearch
=========

The code for configuration and updating of the Open Discovery Space resources' search.



Installation Synopsis
----------------------
(note: a detailed script is below)

Install SOLR as traditional, using a webapp that is the expansion of
the war file (a zip) called solr and a data and config directory
called solr inside the root of the webapp container

### Wire the odsSolr project

Build the project odsSolr using Apache Maven (maven.apache.org).
You obtain a jar in the target directory.
Copy it to WEB-INF/lib of the webapp directory.

### Install the solr/itas requestHandler

Copy the velocity solr-velocity-4.6.1.jar into WEB-INF/lib.
Copy the velocity libs from contrib/velocity/lib to WEB-INF/lib.

Now start the webapp.



Installation script
-------------------
The above installation synopsis can be executed on a unix system using the
following commands, assuming we are in a directory where there is enough space
(~350Mb) and where a directory called `tomcat` is the webapp container and
that there is [Apache Maven](http://maven.apache.org) in the form of the `mvn`
command in the path.


	  ## clone the odsSearch project
	  git clone https://github.com/OpenDiscoverySpace/odsSearch.git
	  ## build odsSearch/odsSolr
	  cd odsSearch/odsSolr
	  mvn install
	  ## we fetch solr 4.6.1 as this one is the broadest in compatibility
	  wget http://archive.apache.org/dist/lucene/solr/4.6.1/solr-4.6.1.tgz
	  mkdir -p tomcat/webapps/solr/
	  tar zxf solr-4.6.1.tgz 
	  cd tomcat/webapps/solr/
	  unzip ../../../solr-4.6.1.war
	  cd ../../..
	  cp solr-4.6.1/contrib/velocity/lib/*.jar tomcat/webapps/solr/WEB-INF/lib/
	  cp odsSearch/odsSolr/target/ods-search-solrcomp-0.1-SNAPSHOT.jar  tomcat/webapps/solr/WEB-INF/lib/
	  mkdir -p tomcat/solr/collection1/conf/
	  cp odsSearch/odsSolrConfig/conf/* tomcat/solr/collection1/conf/
	  rsync -avc odsSearch/odsSolrConfig/conf/ tomcat/solr/collection1/conf/
	  ## Now the tomcat can be started.
	  
