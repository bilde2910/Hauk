<?php

// An include file containing constants and common functions for the Hauk
// backend. It loads the configuration file and declares it as a constant.

const SESSION_ID_SIZE = 32;
const EARTH_DIAMETER_KM = 6371 * 2;

const KILOMETERS_PER_HOUR = array(
    // Relative distance per second multiplied by number of seconds per hour.
    "havMod" => EARTH_DIAMETER_KM * 3600,
    "mpsMod" => 3.6,
    "unit" => "km/h"
);
const MILES_PER_HOUR = array(
    // Same as for KILOMETERS_PER_HOUR, but convert kilometers to miles.
    "havMod" => EARTH_DIAMETER_KM * 3600 * 0.6213712,
    "mpsMod" => 3.6 * 0.6213712,
    "unit" => "mph"
);
const METERS_PER_SECOND = array(
    // Relative distance per second in kilometers, multiplied by meters per km.
    "havMod" => EARTH_DIAMETER_KM * 1000,
    "mpsMod" => 1,
    "unit" => "m/s"
);

// Configuration can be stored either in /etc/hauk/config.php (e.g. Docker
// installations) or relative to this file as config.php. Only include the first
// one found from this list.
const CONFIG_PATHS = array(
    "/etc/hauk/config.php",
    __DIR__."/config.php"
);

foreach (CONFIG_PATHS as $path) {
    if (file_exists($path)) {
        include($path);
        break;
    }
}

if (!defined("CONFIG")) die("Unable to find config.php!\n");
$PREFIX_SESSION = CONFIG["memcached_prefix"]."-session-";
$PREFIX_LOCDATA = CONFIG["memcached_prefix"]."-locdata-";

// Function for converting session IDs to link IDs. The function is:
// First and last four digits of the base36 SHA256 sum of the binary session ID.
// This is converted to uppercase and the two parts separated by a dash.
function sessionToID($session) {
    if (!preg_match("/^[0-9a-fA-F]{".(SESSION_ID_SIZE * 2)."}$/", $session)) return false;
    $s = strtoupper(base_convert(hash("sha256", hex2bin($session));, 16, 36));
    return substr($s, 0, 4)."-".substr($s, -4);
}

// Hauk maintains compatibility with both `memcache` and `memcached`, meaning we
// need an abstraction layer between Hauk and the extensions to ensure a unified
// interface for both. This code loads the correct memcache wrapper.
if (extension_loaded("memcached")) {
    include_once(__DIR__."/wrapper/memcached.php");
} else if (extension_loaded("memcache")) {
    include_once(__DIR__."/wrapper/memcache.php");
} else {
    die("No compatible memcached extension (memcache or memcached) is enabled in your PHP config!\n");
}

// Returns a memcached instance.
function memConnect() {
    $memcache = new MemWrapper(CONFIG["memcached_host"], CONFIG["memcached_port"]);
    return $memcache;
}

?>
