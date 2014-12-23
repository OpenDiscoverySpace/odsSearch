<?php

/**
 * @file
 * Default theme implementation for displaying search results.
 *
 * This template collects each invocation of theme_search_result(). This and
 * the child template are dependent to one another sharing the markup for
 * definition lists.
 *
 * Note that modules may implement their own search type and theme function
 * completely bypassing this template.
 *
 * Available variables:
 * - $search_results: All results as it is rendered through
 *   search-result.tpl.php
 * - $module: The machine-readable name of the module (tab) being searched, such
 *   as "node" or "user".
 *
 *
 * @see template_preprocess_search_results()
 *
 * @ingroup themeable
 */
?>
<?php $param_sort = $_GET["solrsort"];  

function changeSortParam ($sort_value, $url_search) {
   $new_search = $url_search;
   //Find the string "?". 
   $pos_start_param = strpos($url_search, "?");
   if ($pos_start_param==false){
      //The string has not been found, therefore, we don't have parameters. We add the new sort value.
      $new_search = $url_search . "?" . $sort_value;
   } else {
	//We have at least one parameter.
	//We have to check if we have the "solrsort" parameter.
	$pos_sort_param = strpos($url_search, "solrsort");
	if ($pos_sort_param == false) {
		//We have not found this parameter, we have to add it.
		$new_search = $url_search . "&" . $sort_value;
	} else {
		//We have to change the sort param.
		//First we check if we have the symbol "&" because we have more parameters
		$pos_next_param = strpos($url_search, "&", $pos_sort_param + 1);
		if ($pos_next_param == false){
			//There are no more parameters after that. We have to overwrite the previous sorting.
			$new_search = substr($url_search, 0, $pos_sort_param) . $sort_value;
		} else {
			//We have to change the sort param.
			$new_search = substr($url_search, 0, $pos_sort_param) . $sort_value . substr($url_search, $pos_next_param);                 
		}
	}
   }
   return $new_search;
}
?>

<?php if ($search_results): ?>
  <table>
  <tr>
    <td>
      <h4><?php 
        global $pager_page_array, $pager_total_items;

        $itemsPerPage = 10;

        // Determine which page is being viewed.
        // If $_REQUEST['page'] is not set, we are on page 1.
        $currentPage = (isset($_REQUEST['page']) ? $_REQUEST['page'] : 0) + 1;

        // Get the total number of results from the global pager.
        $total = $pager_total_items[0];

        // Determine which results are being shown.
        $start = (10 * $currentPage) - 9;
        // If on the last page, only go up to $total, not the total that could be
        // shown on the page. This prevents things like "Showing  11 to 20 of 17 results found".
        $end = (($itemsPerPage * $currentPage) >= $total) ? $total : ($itemsPerPage * $currentPage);

        //We have more than one page of results.
        if ($total > $itemsPerPage) {
          print t('Showing %start to %end out of %total results found',
            array(
              '%start' => $start,
              '%end' => $end,
              '%total' => $total
            )
          );    
        }
        else {
          // Only one page of results.
          print t('Showing %total %results_label found',
            array(
              '%total' => $total,
              '%results_label' => format_plural($total, 'result', 'results'),
            )
          );    
        }
      ?>
      </h4>
    </td>
    <td>
   	<div style="margin-left:95px;"><h4>Sort by:</h4></div>
  </td>
  <td>
	<select onchange="if (this.value) window.location.href=this.value"
		style="color:#ef5033;" name="this.value">
        <?php $url_search = $_SERVER["REQUEST_URI"]; ?>
        <?php $new_url_search = changeSortParam("", $url_search); ?>
        <?php if ($param_sort == "") { ?>
           <option value="<?php print $new_url_search; ?>" selected>Relevancy</option>
        <?php } else { ?>
           <option value="<?php print $new_url_search; ?>">Relevancy</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=sort_label%20asc", $url_search); ?>
        <?php if ($param_sort == "sort_label asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Title</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Title</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=updated%20asc", $url_search); ?>
        <?php if ($param_sort == "updated asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Date</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Date</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=comment_count%20asc", $url_search); ?>
        <?php if ($param_sort == "comment_count asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Most comments</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Most comments</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=author%20asc", $url_search); ?>
        <?php if ($param_sort == "author asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Contributor</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Contributor</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=source%20asc", $url_search); ?>
        <?php if ($param_sort == "source asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Repository</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Repository</option>
        <?php } ?>
        <?php $new_url_search = changeSortParam("solrsort=granularity%20asc", $url_search); ?>
        <?php if ($param_sort == "granularity asc") { ?>
    	   <option value="<?php print $new_url_search; ?>" selected>Granularity</option>
        <?php } else { ?>
    	   <option value="<?php print $new_url_search; ?>">Granularity</option>
        <?php } ?>
   	</select>
    </td>
  </tr>
  </table>

  <ol class="search-results <?php print $module; ?>-results">
    <?php print $search_results; ?>
  </ol>
  <?php print $pager; ?>
<?php else : ?>
  <h2><?php print t('Your search yielded no results');?></h2>
  <?php print search_help('search#noresults', drupal_help_arg()); ?>
<?php endif; ?>
