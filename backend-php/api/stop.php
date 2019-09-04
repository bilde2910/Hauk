<?php

// This script handles session cancellation for Hauk clients. It removes the
// given session from memcached.

foreach (array("sid") as $field) if (!isset($_POST[$field])) die("Missing data!\n");

include("../include/inc.php");
$memcache = memConnect();

// Delete the session keys from memcached.
$sid = $_POST["sid"];
$session = $memcache->delete($PREFIX_SESSION.$sid);
$locdata = $memcache->delete($PREFIX_LOCDATA.sessionToID($sid));

// Convert the session ID to a link ID and return these two to the client.
echo "OK\n";

?>
