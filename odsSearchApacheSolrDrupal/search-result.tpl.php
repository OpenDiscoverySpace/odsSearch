<?php

/**
 * @file
 * Default theme implementation for displaying a single search result.
 *
 * This template renders a single search result and is collected into
 * search-results.tpl.php. This and the parent template are
 * dependent to one another sharing the markup for definition lists.
 *
 * Available variables:
 * - $url: URL of the result.
 * - $title: Title of the result.
 * - $snippet: A small preview of the result. Does not apply to user searches.
 * - $info: String of all the meta information ready for print. Does not apply
 *   to user searches.
 * - $info_split: Contains same data as $info, split into a keyed array.
 * - $module: The machine-readable name of the module (tab) being searched, such
 *   as "node" or "user".
 * - $title_prefix (array): An array containing additional output populated by
 *   modules, intended to be displayed in front of the main title tag that
 *   appears in the template.
 * - $title_suffix (array): An array containing additional output populated by
 *   modules, intended to be displayed after the main title tag that appears in
 *   the template.
 *
 * Default keys within $info_split:
 * - $info_split['type']: Node type (or item type string supplied by module).
 * - $info_split['user']: Author of the node linked to users profile. Depends
 *   on permission.
 * - $info_split['date']: Last update of the node. Short formatted.
 * - $info_split['comment']: Number of comments output as "% comments", %
 *   being the count. Depends on comment.module.
 *
 * Other variables:
 * - $classes_array: Array of HTML class attribute values. It is flattened
 *   into a string within the variable $classes.
 * - $title_attributes_array: Array of HTML attributes for the title. It is
 *   flattened into a string within the variable $title_attributes.
 * - $content_attributes_array: Array of HTML attributes for the content. It is
 *   flattened into a string within the variable $content_attributes.
 *
 * Since $info_split is keyed, a direct print of the item is possible.
 * This array does not apply to user searches so it is recommended to check
 * for its existence before printing. The default keys of 'type', 'user' and
 * 'date' always exist for node searches. Modules may provide other data.
 * @code
 *   <?php if (isset($info_split['comment'])): ?>
 *     <span class="info-comment">
 *       <?php print $info_split['comment']; ?>
 *     </span>
 *   <?php endif; ?>
 * @endcode
 *
 * To check for all available data within $info_split, use the code below.
 * @code
 *   <?php print '<pre>'. check_plain(print_r($info_split, 1)) .'</pre>'; ?>
 * @endcode
 *
 * @see template_preprocess()
 * @see template_preprocess_search_result()
 * @see template_process()
 *
 * @ingroup themeable
 */

// find out the current language
global $language;
$lang = $language;
//dd("Language: "); dd($lang);
if(gettype($lang)==="object") {
    $lang = $lang->language;
}
if(gettype($lang)==="NULL" || $lang==="") {
    //$lang = $user -> language;
    if(!$lang) $lang="en";
}

$fields = $result['fields'];

// find out title
$title_aside=$title;
//dd("Fields:");
//dd($result["fields"]);

$title = odsSearchApacheSolrDrupal_findFirstSignificant(array(
    $fields["i18n_label_$lang"],
    $fields["label"],
    $title_aside,
    t("Missing title")));

$teaser = odsSearchApacheSolrDrupal_findFirstSignificant(array(
    $fields["i18n_teaser_$lang"],
    $fields["teaser"],
    t("Missing description")));

$source = $source = odsSearchApacheSolrDrupal_findFirstSignificant(array(
    $fields['source']), t("Missing source"));

$author = $fields['author'];
if(isset($author)) $author = trim($author);
if ($author==="Usuario" || $author==="Unknown") $author = "";
if($author!=="") $source.", ".$author;

$url = url("node/".$fields['entity_id']);

/* if($result["fields"]["i18n_label_$lang"] && $result["fields"]["i18n_label_$lang"]!="____") {
    $title = $result["fields"]["i18n_label_$lang"];
    print("<!-- title from i18n_label_$lang -->");
}  elseif($result["fields"]["label"] && $result["fields"]["label"]!="____") {
    $title=$result["fields"]["label"];
    print("<!-- title from label -->");
}
if(!$title || $title=="____") {
    $title = $title_aside;
    print("<!-- missing title -->");
}
if(!$title || $title=="_____") {
    $title = "Missing-title";
    print("<!-- missing title -->");
}*/
?>
<li class="<?php print $classes; ?>"<?php print $attributes; ?>>
  <?php print render($title_prefix); ?>
  <h3 class="title"<?php print $title_attributes;
    ?>><a href="<?php print $url ?>" style="text-decoration:none" title="<?php print t('Go to the summary page')
      ?>"><?php print $title; ?></a></h3><!-- score: <?php print $result["score"] ?> --><?php print render($title_suffix); ?>
  <div class="search-snippet-info">
    <table>
	<tr>
	  <?php if ($fields['source']){ ?>
	    <td rowspan=2 valign="middle">
	      <div style = "width:100px; overflow:hidden;"><a href="<?php print $url?>">
		<?php global $base_url; $logo_url = str_replace(" ", "%20",$base_url."/sites/default/files/repository_logos/".$fields['source'].".png"); ?>
		<?php if (odsSearchApacheSolrDrupal_url_exists($logo_url)) { ?><!-- logo available -->
			<img src="<?php global $base_url; print $base_url.'/sites/default/files/repository_logos/'.$fields['source'].'.png';?>" alt="<?php print $fields['source']?>" />
		<?php } else { ?><!-- logo unavailable -->
			<img src="<?php global $base_url; print $base_url.'/sites/default/files/repository_logos/imageNotAvailable.png';?>" />
		<?php } ?>
              </a></div>
	    </td>
          <?php } else { ?>
	    <td rowspan=2></td>
	  <?php } ?>
        <td class="search-teaser"><a href="<?php print $url?>"><?php print $teaser ?></a></td>
	</tr>
	<tr>
	  <td><div class="search-info" style="font-size:12px"><a href="<?php print $url?>"><b><?php
                      print t("Source: "); ?></b> <?php print $source ?></a></div>
	  </td>
        </tr>
    </table>
   </div>
</li>
