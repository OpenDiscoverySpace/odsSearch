// this optional file can be activated using
//  drupal_add_js( drupal_get_path('module', 'odsSearchApacheSolrDrupal') . '/keepIndexing.js', array('every_page' =>FALSE, 'preprocess'=>FALSE));)
// Doing this keeps the window that loads /admin/config/search/apachesolr/settings/solr/index
// to resubmit "Index queued content".
// typically, it may be a good idea to connect this with a prior  drush variable-set apachesolr_cron_limit 200

jQuery(document).ready(function() {
    window.keepIndexing = window.setInterval(function() {
        var d = document.getElementById('edit-cron');
        if(d && d.click()) {d.click();}
        clearInterval(window.keepIndexing);
    }, 2000);
});