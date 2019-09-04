<?php

// This script is called by the client to receive location updates. A link ID is
// required to retrieve data.

foreach (array("id") as $field) if (!isset($_GET[$field])) die("Invalid session!\n");

include("../include/inc.php");
$memcache = memConnect();
$locdata = $memcache->get($PREFIX_LOCDATA.$_GET["id"]);

// If the link data key is not set, the session probably expired.
if ($locdata === false) die("Invalid session!\n");
else { header("Content-Type: text/json"); echo json_encode($locdata); }

?>
