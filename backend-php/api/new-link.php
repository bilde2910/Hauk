<?php

// This script allows Hauk clients to attach themselves to a new single-user
// share.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST(
    "sid", // Existing session ID.
    "ado", // Whether or not to allow adoption.
);

$memcache = memConnect();

// Retrieve the session data from memcached.
$sid = $_POST["sid"];
$session = new Client($memcache, $sid);
if (!$session->exists()) die($LANG['session_expired']."\n");

// Create a new solo share and set its arguments.
$share = new SoloShare($memcache);
$share
    ->setAdoptable(intval($_POST["ado"]))
    ->setHost($session)
    ->setExpirationTime($session->getExpirationTime())
    ->save();

// Tell the session that it is posting to this share.
$session->addTarget($share)->save();

$output = array(
    "OK",
    $share->getViewLink(),
    $share->getShareID()
);

foreach ($output as $line) {
    echo $line."\n";
}
