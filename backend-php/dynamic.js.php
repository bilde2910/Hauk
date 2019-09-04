<?php

// This script dynamically generates JavaScript containing certain configuration
// entries needed for clientside processing.

include("./include/inc.php");
header("Content-Type: text/javascript");

?>
var TILE_URI = <?php echo json_encode(CONFIG["map_tile_uri"]); ?>;
var ATTRIBUTION = <?php echo json_encode(CONFIG["map_attribution"]); ?>;
var DEFAULT_ZOOM = <?php echo json_encode(CONFIG["default_zoom"]); ?>;
var MAX_ZOOM = <?php echo json_encode(CONFIG["max_zoom"]); ?>;
var MAX_POINTS = <?php echo json_encode(CONFIG["max_shown_pts"]); ?>;
var VELOCITY_DELTA_TIME = <?php echo json_encode(CONFIG["v_data_points"]); ?>;
var TRAIL_COLOR = <?php echo json_encode(CONFIG["trail_color"]); ?>;
var VELOCITY_UNIT = <?php echo json_encode(CONFIG["velocity_unit"]); ?>;
