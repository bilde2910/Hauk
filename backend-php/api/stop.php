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
$session = new Client($memcache, $sid);

if (isset($_POST["lid"])) {
    // Terminate the given share membership.
    $lid = $_POST["lid"];
    // Check that the share exists and that the user actually owns it.
    if ($session->exists() && in_array($lid, $session->getTargetIDs())) {
        $share = Share::fromShareID($memcache, $lid);
        if ($share->exists()) {
            switch ($share->getType()) {
                case SHARE_TYPE_ALONE:
                    $share->end();
                    break;
                case SHARE_TYPE_GROUP:
                    $share->removeHost($session)->clean();
                    break;
            }
        }
        $session->removeTarget($share)->save();
    }
} else {
    // Tell the entire session to terminate.
    if ($session->exists()) $session->end();
}

echo "OK\n";
