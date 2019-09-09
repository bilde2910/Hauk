<?php

// This script handles session initiation for Hauk clients. It creates a session
// ID and associated share URL, and return these to the client.

include("../include/inc.php");
header("X-Hauk-Version: ".BACKEND_VERSION);

requirePOST("pwd", "dur", "int");

// Verify that the client is authorized to connect.
if (!password_verify($_POST["pwd"], CONFIG["password_hash"])) die("Incorrect password!\n");

// Perform input validation.
$d = intval($_POST["dur"]);
$i = floatval($_POST["int"]);
$mod = isset($_POST["mod"]) ? intval($_POST["mod"]) : SHARE_MODE_CREATE_ALONE;
if ($d > CONFIG["max_duration"]) die("Share period is too long!\n");
if ($i > CONFIG["max_duration"]) die("Ping interval is too long!\n");
if ($i < CONFIG["min_interval"]) die("Ping interval is too short!\n");
$expire = time() + $d;
$adoptable = isset($_POST["ado"]) ? intval($_POST["ado"]) : 0;

switch ($mod) {
    case SHARE_MODE_CREATE_GROUP:
        requirePOST("nic");
        break;
    case SHARE_MODE_JOIN_GROUP:
        requirePOST("nic", "pin");
        break;
}

$memcache = memConnect();
$host = new Client($memcache);
$host
    ->setExpirationTime($expire)
    ->setInterval($i)
    ->save();

$output = array("Unsupported share mode!");

switch ($mod) {
    case SHARE_MODE_CREATE_ALONE:
        $share = new SoloShare($memcache);
        $share
            ->setAdoptable($adoptable)
            ->setHost($host)
            ->setExpirationTime($expire)
            ->save();
        $host->addTarget($share)->save();
        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink()
        );
        break;

    case SHARE_MODE_CREATE_GROUP:
        $nickname = $_POST["nic"];
        $share = new GroupShare($memcache);
        $share
            ->addHost($nickname, $host)
            ->setExpirationTime($expire)
            ->save();
        $host->addTarget($share)->save();
        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink(),
            $share->getGroupPIN()
        );
        break;

    case SHARE_MODE_JOIN_GROUP:
        $nickname = $_POST["nic"];
        $pin = $_POST["pin"];
        $share = Share::fromGroupPIN($memcache, $pin);
        if (!$share->exists()) die("Invalid group PIN!\n");
        $share
            ->addHost($nickname, $host)
            ->save();
        $host->addTarget($share)->save();
        $output = array(
            "OK",
            $host->getSessionID(),
            $share->getViewLink()
        );
        break;
}

foreach ($output as $line) {
    echo $line."\n";
}
