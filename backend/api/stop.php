<?php

// This script handles session cancellation for Hauk clients. It removes the
// given session from memcached.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST("sid");
$sid = $_POST["sid"];

$memcache = memConnect();
$session = new Client($memcache, $sid);
if ($session->exists()) $session->end();

echo "OK\n";
