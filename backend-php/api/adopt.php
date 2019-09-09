<?php

// This script is called from the Hauk app to push location updates to the
// server. Each update contains a location and timestamp from when the location
// was fetched by the client.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST(
    "sid", // Session ID of adopting user.
    "nic", // Nickname for the adopted share.
    "aid", // Link ID of the share to adopt.
    "pin"  // Group PIN for the share being adopted into.
);

$memcache = memConnect();
$nickname = $_POST["nic"];

// Retrieve the session data from memcached.
$sid = $_POST["sid"];
$session = new Client($memcache, $sid);
if (!$session->exists()) die("Session expired!\n");

// Retrieve adopted share data from memcached.
$shid = $_POST["aid"];
$share = Share::fromShareID($memcache, $shid);
if (!$share->exists()) die("The given share does not exist!\n");
if (!$share->getType() === SHARE_TYPE_ALONE) die("You cannot adopt group shares!\n");
if (!$share->isAdoptable()) die("The host of the given share does not permit adoption!\n");

// Retrieve the target share.
$pin = $_POST["pin"];
$target = Share::fromGroupPIN($memcache, $pin);
if (!$target->exists()) die("Session expired!\n");

// Join the shares.
$target->addHost($nickname, $share->getHost())->save();
$share->getHost()->addTarget($target)->save();

echo "OK\n";
