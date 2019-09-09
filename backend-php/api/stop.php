<?php

// This script handles session cancellation for Hauk clients. It removes the
// given session from memcached.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST(
    "sid" // Session ID to delete.
);

$sid = $_POST["sid"];
$memcache = memConnect();

// Fetch the session and tell it to terminate.
$session = new Client($memcache, $sid);
if ($session->exists()) $session->end();

echo "OK\n";
