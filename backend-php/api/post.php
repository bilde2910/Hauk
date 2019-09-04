<?php

// This script is called from the Hauk app to push location updates to the
// server. Each update contains a location and timestamp from when the location
// was fetched by the client.

foreach (array("lat", "lon", "time", "sid") as $field) if (!isset($_POST[$field])) die("Missing data!");

// Perform input validation.
$lat = floatval($_POST["lat"]);
$lon = floatval($_POST["lon"]);
$time = floatval($_POST["time"]);
if ($lat < -90 || $lat > 90 || $lon < -180 || $lon > 180) die("Invalid location!\n");

// Not all devices report speed and accuracy, but if available, report them too.
$speed = isset($_POST["spd"]) ? floatval($_POST["spd"]) : null;
$accuracy = isset($_POST["acc"]) ? floatval($_POST["acc"]) : null;

include("../include/inc.php");
$memcache = memConnect();

// Retrieve the session and associated location data from memcached.
$sid = $_POST["sid"];
$session = $memcache->get($PREFIX_SESSION.$sid);
$locdata = $memcache->get($PREFIX_LOCDATA.sessionToID($sid));
if ($session === false) die("Session expired!\n");
if ($locdata === false) $locdata = ["l" => []];

// The location data object contains the sharing interval (i), duration (d) and
// a location list (l). Each entry in the location list contains a latitude,
// longitude, timestamp, accuracy and speed, in that order, as an array.
$locdata["i"] = $session["interval"];
$locdata["x"] = $session["expire"];
$locdata["l"][] = [$lat, $lon, $time, $accuracy, $speed];

// Ensure that we don't exceed the maximum number of points stored in memcached.
while (count($locdata["l"]) > CONFIG["max_cached_pts"]) array_shift($locdata["l"]);

// Check if the session expired; otherwise, return the location data.
$remain = $session["expire"] - time();
if ($remain > 0) {
    $memcache->replace($PREFIX_LOCDATA.sessionToID($sid), $locdata);
    echo "OK\n";
} else echo "Session expired!\n";

?>
