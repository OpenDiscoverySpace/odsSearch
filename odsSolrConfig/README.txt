
Install SOLR as traditional, using a webapp that is the expansion of
the war file (a zip) called solr.

## Wire the odsSolr project

Build this project using Apache Maven (maven.apache.org).
You obtain a jar in the target directory.
Copy it to WEB-INF/lib of the webapp directory.


## Install the solr/itas requestHandler

Copy the velocity solr-velocity-4.6.1.jar into WEB-INF/lib.
Copy the velocity libs from contrib/velocity/lib to WEB-INF/lib.


Now start the webapp.