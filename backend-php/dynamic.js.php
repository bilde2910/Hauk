<?php

// This script dynamically generates JavaScript containing certain configuration
// entries needed for clientside processing.

include("./include/inc.php");
header("Content-Type: text/javascript");

?>
var TILE_URI = <?php echo json_encode(getConfig("map_tile_uri")); ?>;
var ATTRIBUTION = <?php echo json_encode(getConfig("map_attribution")); ?>;
var DEFAULT_ZOOM = <?php echo json_encode(getConfig("default_zoom")); ?>;
var MAX_ZOOM = <?php echo json_encode(getConfig("max_zoom")); ?>;
var MAX_POINTS = <?php echo json_encode(getConfig("max_shown_pts")); ?>;
var VELOCITY_DELTA_TIME = <?php echo json_encode(getConfig("v_data_points")); ?>;
var TRAIL_COLOR = <?php echo json_encode(getConfig("trail_color")); ?>;
var VELOCITY_UNIT = <?php echo json_encode(getConfig("velocity_unit")); ?>;
var OFFLINE_TIMEOUT = <?php echo json_encode(getConfig("offline_timeout")); ?>;
