<?php

// This script is called from the Hauk app to push location updates to the
// server. Each update contains a location and timestamp from when the location
// was fetched by the client.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST(
    "lat",  // Current latitude.
    "lon",  // Current longitude.
    "time", // Current timestamp.
    "sid"   // Session ID to post to.
);

// Perform input validation.
$lat = floatval($_POST["lat"]);
$lon = floatval($_POST["lon"]);
$time = floatval($_POST["time"]);
if ($lat < -90 || $lat > 90 || $lon < -180 || $lon > 180) die($LANG['location_invalid']."\n");

// Not all devices report speed and accuracy, but if available, report them too.
$speed = isset($_POST["spd"]) ? floatval($_POST["spd"]) : null;
$accuracy = isset($_POST["acc"]) ? floatval($_POST["acc"]) : null;

$memcache = memConnect();

// Retrieve the session data from memcached.
$sid = $_POST["sid"];
$session = new Client($memcache, $sid);
if (!$session->exists()) die($LANG['session_expired']."\n");

// The location data object contains the sharing interval (i), duration (d) and
// a location list (l). Each entry in the location list contains a latitude,
// longitude, timestamp, accuracy and speed, in that order, as an array.
$session->addPoint([$lat, $lon, $time, $accuracy, $speed])->save();

if ($session->hasExpired()) {
    echo $LANG['session_expired']."\n";
} else {
    echo "OK\n".getConfig("public_url")."?%s\n".implode(",", $session->getTargetIDs())."\n";
}
