<?php

// This script handles session initiation for Hauk clients. It creates a session
// ID and associated share URL, and return these to the client.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST(
    "dur", // Duration in seconds.
    "int"  // Interval in seconds.
);

// Verify that the client is authorized to connect.
if (!authenticated()) die($LANG['incorrect_password']."\n");

// Perform input validation.
$d = intval($_POST["dur"]);
$i = floatval($_POST["int"]);
$mod = isset($_POST["mod"]) ? intval($_POST["mod"]) : SHARE_MODE_CREATE_ALONE;
if ($d > getConfig("max_duration")) die($LANG['share_too_long']."\n");
if ($i > getConfig("max_duration")) die($LANG['interval_too_long']."\n");
if ($i < getConfig("min_interval")) die($LANG['interval_too_short']."\n");

// Adopting a share means incorporating it into another group share. It is
// enabled by default in the app, but users are free to change this.
$adoptable = isset($_POST["ado"]) ? intval($_POST["ado"]) : 0;
$expire = time() + $d;
$encrypted = isset($_POST["e2e"]) ? intval($_POST["e2e"]) : 0;

// Require additional arguments depending on the given sharing mode.
switch ($mod) {
    case SHARE_MODE_CREATE_GROUP:
        // End-to-end encryption is not supported for group shares.
        if ($encrypted > 0) die($LANG['group_e2e_unsupported']."\n");
        requirePOST(
            "nic"  // Nickname to join the group share with.
        );
        break;
    case SHARE_MODE_JOIN_GROUP:
        // End-to-end encryption is not supported for group shares.
        if ($encrypted > 0) die($LANG['group_e2e_unsupported']."\n");
        requirePOST(
            "nic", // Nickname to join the group share with.
            "pin"  // Group PIN for the group to attach to.
        );
        break;
}

// Also require salt when creating encrypted shares.
if ($encrypted) {
    requirePOST(
        "salt" // Salt used to derive the key in PBKDF2.
    );
}

// If a custom link is requested, validate the ID.
$custom = filter_input(INPUT_POST, "lid", FILTER_VALIDATE_REGEXP, array("options" => array("regexp" => "/^[\w-]+$/")));
if ($custom === false) $custom = null;

$memcache = memConnect();

// Create a new client session.
$host = new Client($memcache);
$host
    ->setExpirationTime($expire)
    ->setInterval($i)
    ->save();

// This is the default output.
$output = array($LANG['share_mode_unsupported']);

switch ($mod) {
    case SHARE_MODE_CREATE_ALONE:
        // Create a new solo share and set its arguments.
        $share = new SoloShare($memcache);
        if ($custom !== null) $share->requestCustomLink($custom);
        $share
            ->setAdoptable($adoptable)
            ->setHost($host)
            ->setExpirationTime($expire)
            ->save();

        // Tell the session that it is posting to this share.
        $host
            ->addTarget($share)
            ->setEncrypted($encrypted, $_POST["salt"])
            ->save();

        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink(),
            $share->getShareID()
        );
        break;

    case SHARE_MODE_CREATE_GROUP:
        $nickname = $_POST["nic"];

        // Create a new group share and set its arguments.
        $share = new GroupShare($memcache);
        if ($custom !== null) $share->requestCustomLink($custom);
        $share
            ->addHost($nickname, $host)
            ->setExpirationTime($expire)
            ->save();

        // Tell the session that it is posting to this share.
        $host->addTarget($share)->save();

        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink(),
            $share->getGroupPIN(),
            $share->getShareID()
        );
        break;

    case SHARE_MODE_JOIN_GROUP:
        $nickname = $_POST["nic"];
        $pin = $_POST["pin"];

        // Fetch an existing group share by its group PIN and add the new host.
        $share = Share::fromGroupPIN($memcache, $pin);
        if (!$share->exists()) die($LANG['group_pin_invalid']."\n");
        $share
            ->addHost($nickname, $host)
            ->save();

        // Tell the session that it is posting to this share.
        $host->addTarget($share)->save();

        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink(),
            $share->getShareID()
        );
        break;
}

foreach ($output as $line) {
    echo $line."\n";
}
