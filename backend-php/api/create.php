<?php

// This script handles session initiation for Hauk clients. It creates a session
// ID and associated share URL, and return these to the client.

foreach (array("pwd", "dur", "int") as $field) if (!isset($_POST[$field])) die("Missing data!\n");

// Verify that the client is authorized to connect.
include("../include/inc.php");
if (!password_verify($_POST["pwd"], CONFIG["password_hash"])) die("Incorrect password!\n");

// Perform input validation.
$d = intval($_POST["dur"]);
$i = floatval($_POST["int"]);
if ($d > CONFIG["max_duration"]) die("Share period is too long!\n");
if ($i > CONFIG["max_duration"]) die("Ping interval is too long!\n");
if ($i < CONFIG["min_interval"]) die("Ping interval is too short!\n");
$expire = time() + $d;

$memcache = memConnect();

// Create a session ID and verify that there is no colliding link ID.
$sid = "";
do $sid = bin2hex(openssl_random_pseudo_bytes(SESSION_ID_SIZE));
while ($memcache->get($PREFIX_LOCDATA.sessionToID($sid)) !== false);

$memcache->set($PREFIX_SESSION.$sid, array(
    "expire" => $expire,
    "interval" => $i
), $d);
$memcache->set($PREFIX_LOCDATA.sessionToID($sid), array(
    "i" => $i,
    "x" => $expire,
    "l" => array()
), $d);

// Convert the session ID to a link ID and return these two to the client.
echo "OK\n{$sid}\n".CONFIG["public_url"]."?".sessionToID($sid)."\n";

?>
