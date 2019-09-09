<?php

// This script is called by the client to receive location updates. A link ID is
// required to retrieve data.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

foreach (array("id") as $field) if (!isset($_GET[$field])) die("Invalid session!\n");

$memcache = memConnect();

// Get the share from the given share ID.
$share = Share::fromShareID($memcache, $_GET["id"]);

// If the link data key is not set, the session probably expired.
if (!$share->exists()) {
    die("Invalid session!\n");
} else {
    header("Content-Type: text/json");

    // Solo and group shares have different internal structures. Figure out the
    // correct type so that it can be output.
    switch ($share->getType()) {
        case SHARE_TYPE_ALONE:
            $session = $share->getHost();
            if (!$session->exists()) die("Invalid session!\n");
            echo json_encode(array(
                "type" => $share->getType(),
                "expire" => $share->getExpirationTime(),
                "interval" => $session->getInterval(),
                "points" => $session->getPoints()
            ));
            break;

        case SHARE_TYPE_GROUP:
            echo json_encode(array(
                "type" => $share->getType(),
                "expire" => $share->getExpirationTime(),
                "interval" => $share->getAutoInterval(),
                "points" => $share->getAllPoints()
            ));
            break;
    }
}

echo "\n";
