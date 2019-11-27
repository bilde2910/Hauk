<?php

// This script is called from the Hauk app to adopt an existing single-user
// share into a group share.

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
if (!$session->exists()) die($LANG['session_expired']."\n");

// Retrieve adopted share data from memcached.
$shid = $_POST["aid"];
$share = Share::fromShareID($memcache, $shid);
if (!$share->exists()) die($LANG['share_not_found']."\n");
if (!$share->getType() === SHARE_TYPE_ALONE) die($LANG['group_share_not_adoptable']."\n");
if (!$share->isAdoptable()) die($LANG['share_adoption_not_allowed']."\n");
if ($share->getHost()->isEncrypted()) die($LANG['e2e_adoption_not_allowed']."\n");

// Retrieve the target share.
$pin = $_POST["pin"];
$target = Share::fromGroupPIN($memcache, $pin);
if (!$target->exists()) die($LANG['session_expired']."\n");

// Join the shares.
$target->addHost($nickname, $share->getHost())->save();
$share->getHost()->addTarget($target)->save();

echo "OK\n";
