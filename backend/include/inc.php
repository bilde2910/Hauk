<?php

// An include file containing constants and common functions for the Hauk
// backend. It loads the configuration file and declares it as a constant.

const BACKEND_VERSION = "1.1";

const SHARE_MODE_CREATE_ALONE = 0;
const SHARE_MODE_CREATE_GROUP = 1;
const SHARE_MODE_JOIN_GROUP = 2;

const SHARE_TYPE_ALONE = 0;
const SHARE_TYPE_GROUP = 1;

const SESSION_ID_SIZE = 32;
const LINK_ID_RAND_BYTES = 32;
const GROUP_PIN_MIN = 100000;
const GROUP_PIN_MAX = 999999;
const EARTH_DIAMETER_KM = 6371 * 2;

const KILOMETERS_PER_HOUR = array(
    // Relative distance per second multiplied by number of seconds per hour.
    "havMod" => EARTH_DIAMETER_KM * 3600,
    "mpsMod" => 3.6,
    "unit" => "km/h"
);
const MILES_PER_HOUR = array(
    // Same as for KILOMETERS_PER_HOUR, but convert kilometers to miles.
    "havMod" => EARTH_DIAMETER_KM * 3600 * 0.6213712,
    "mpsMod" => 3.6 * 0.6213712,
    "unit" => "mph"
);
const METERS_PER_SECOND = array(
    // Relative distance per second in kilometers, multiplied by meters per km.
    "havMod" => EARTH_DIAMETER_KM * 1000,
    "mpsMod" => 1,
    "unit" => "m/s"
);

// Configuration can be stored either in /etc/hauk/config.php (e.g. Docker
// installations) or relative to this file as config.php. Only include the first
// one found from this list.
const CONFIG_PATHS = array(
    "/etc/hauk/config.php",
    __DIR__."/config.php"
);

foreach (CONFIG_PATHS as $path) {
    if (file_exists($path)) {
        include($path);
        break;
    }
}

if (!defined("CONFIG")) die("Unable to find config.php!\n");
define("PREFIX_SESSION", CONFIG["memcached_prefix"]."-session-");
define("PREFIX_LOCDATA", CONFIG["memcached_prefix"]."-locdata-");
define("PREFIX_GROUPID", CONFIG["memcached_prefix"]."-groupid-");

class Share {
    protected $memcache;
    protected $shareID;
    protected $shareData;

    public static function fromShareID($memcache, $shareID) {
        $shareData = $memcache->get(PREFIX_LOCDATA.$shareID);
        if ($shareData === false) return new Share($memcache, false);
        $share = null;
        switch ($shareData["type"]) {
            case SHARE_TYPE_ALONE:
                $share = new SoloShare($memcache, $shareData);
                break;
            case SHARE_TYPE_GROUP:
                $share = new GroupShare($memcache, $shareData);
                break;
        }
        $share->shareID = $shareID;
        return $share;
    }

    public static function fromGroupPIN($memcache, $pin) {
        $linkID = $memcache->get(PREFIX_GROUPID.$pin);
        if ($linkID === false) {
            return new GroupShare($memcache, false);
        } else {
            return self::fromShareID($memcache, $linkID);
        }
    }

    protected function __construct($memcache, $shareData = null) {
        $this->memcache = $memcache;
        if ($shareData === null) {
            $this->shareData = array(
                "expire" => 0
            );
            $this->shareID = $this->generateLinkID();
        } else {
            $this->shareData = $shareData;
        }
    }

    public function exists() {
        return $this->shareData !== false;
    }

    public function save() {
        if ($this->shareData["expire"] === 0) {
            throw new UnexpectedValueException("Share cannot be indefinite");
        }
        if (!$this->hasExpired()) {
            $this->memcache->set(PREFIX_LOCDATA.$this->shareID, $this->shareData, $this->getExpirationTime());
        } else {
            $this->memcache->delete(PREFIX_LOCDATA.$this->shareID);
        }
        return $this;
    }

    public function getType() {
        return $this->shareData["type"];
    }

    public function getShareID() {
        return $this->shareID;
    }

    public function getViewLink() {
        return CONFIG["public_url"]."?".$this->shareID;
    }

    public function setExpirationTime($expire) {
        $this->shareData["expire"] = $expire;
        return $this;
    }

    public function getExpirationTime() {
        return $this->shareData["expire"];
    }

    public function hasExpired() {
        return $this->getExpirationTime() <= time();
    }

    // Function for generating link IDs. The function is:
    // First and last four digits of the base36 SHA256 sum of random binary data.
    // This is converted to uppercase and the two parts separated by a dash.
    protected function generateLinkID() {
        $s = "";
        do {
            $s = strtoupper(base_convert(hash("sha256", openssl_random_pseudo_bytes(LINK_ID_RAND_BYTES)), 16, 36));
            $s = substr($s, 0, 4)."-".substr($s, -4);
        } while ($this->memcache->get(PREFIX_LOCDATA.$s) !== false);
        return $s;
    }
}

class SoloShare extends Share {
    function __construct($memcache, $shareData = null) {
        parent::__construct($memcache, $shareData);
        if ($shareData === null) {
            $this->shareData["type"] = SHARE_TYPE_ALONE;
            $this->shareData["host"] = null;
            $this->shareData["adoptable"] = false;
        }
    }

    public function end() {
        $this->memcache->delete(PREFIX_LOCDATA.$this->getShareID());
    }

    public function setAdoptable($adoptable) {
        $this->shareData["adoptable"] = $adoptable;
        return $this;
    }

    public function isAdoptable() {
        return $this->shareData["adoptable"];
    }

    public function setHost($host) {
        $this->shareData["host"] = $host->getSessionID();
        return $this;
    }

    public function getHost() {
        return new Client($this->memcache, $this->shareData["host"]);
    }
}

class GroupShare extends Share {
    function __construct($memcache, $shareData = null) {
        parent::__construct($memcache, $shareData);
        if ($shareData === null) {
            $this->shareData["type"] = SHARE_TYPE_GROUP;
            $this->shareData["hosts"] = null;
            $this->shareData["groupPin"] = $this->generateGroupPIN();
        }
    }

    public function save() {
        parent::save();
        $this->memcache->set(PREFIX_GROUPID.$this->getGroupPIN(), $this->getShareID(), $this->getExpirationTime());
        return $this;
    }

    public function isAdoptable() {
        return false;
    }

    public function getGroupPIN() {
        return $this->shareData["groupPin"];
    }

    public function addHost($nick, $host) {
        $this->shareData["hosts"][$nick] = $host->getSessionID();
        $this->setAutoExpirationTime();
        return $this;
    }

    public function clean() {
        $hosts = $this->getHosts();
        foreach ($hosts as $nick => $host) {
            if (!$host->exists() || $host->hasExpired()) {
                unset($this->shareData["hosts"][$nick]);
            }
        }
        if (empty($this->shareData["hosts"])) {
            $this->memcache->delete(PREFIX_LOCDATA.$this->getShareID());
            $this->memcache->delete(PREFIX_GROUPID.$this->getGroupPIN());
        } else {
            $this->setAutoExpirationTime()->save();
        }
    }

    public function getHosts() {
        $hosts = array();
        foreach ($this->shareData["hosts"] as $nick => $sessionID) {
            $hosts[$nick] = new Client($this->memcache, $sessionID);
        }
        return $hosts;
    }

    public function setAutoExpirationTime() {
        $this->shareData["expire"] = $this->getAutoExpirationTime();
        return $this;
    }

    public function getAutoExpirationTime() {
        $expire = 0;
        $hosts = $this->getHosts();
        foreach ($hosts as $nick => $host) {
            if ($host->exists() && $host->getExpirationTime() > $expire) {
                $expire = $host->getExpirationTime();
            }
        }
        return $expire;
    }

    public function getAutoInterval() {
        $interval = PHP_INT_MAX;
        $hosts = $this->getHosts();
        foreach ($hosts as $nick => $host) {
            if ($host->exists() && $host->getInterval() < $interval) {
                $interval = $host->getInterval();
            }
        }
        return $interval;
    }

    public function getAllPoints() {
        $points = array();
        $hosts = $this->getHosts();
        foreach ($hosts as $nick => $host) {
            if ($host->exists()) {
                $points[$nick] = $host->getPoints();
            }
        }
        return $points;
    }

    private function generateGroupPIN() {
        $pin = 0;
        do $pin = rand(GROUP_PIN_MIN, GROUP_PIN_MAX);
        while ($this->memcache->get(PREFIX_GROUPID.$pin) !== false);
        return $pin;
    }
}

class Client {
    private $memcache;
    private $sessionID;
    private $sessionData;

    function __construct($memcache, $sid = null) {
        $this->memcache = $memcache;
        if ($sid === null) {
            $this->sessionData = array(
                "expire" => 0,
                "interval" => null,
                "targets" => array(),
                "points" => array()
            );
            $this->sessionID = $this->generateSessionID();
        } else {
            $this->sessionData = $memcache->get(PREFIX_SESSION.$sid);
            $this->sessionID = $sid;
        }
    }

    public function exists() {
        return $this->sessionData !== false;
    }

    public function save() {
        if ($this->sessionData["interval"] === null) {
            throw new UnexpectedValueException("Session interval is undefined");
        } elseif ($this->sessionData["expire"] === 0) {
            throw new UnexpectedValueException("Session cannot be indefinite");
        }
        if (!$this->hasExpired()) {
            $this->memcache->set(PREFIX_SESSION.$this->sessionID, $this->sessionData, $this->getExpirationTime());
        } else {
            $this->memcache->delete(PREFIX_SESSION.$this->sessionID);
        }
        return $this;
    }

    public function end() {
        $this->memcache->delete(PREFIX_SESSION.$this->sessionID);
        $targets = $this->getTargets();
        foreach ($targets as $share) {
            if ($share->exists()) {
                switch ($share->getType()) {
                    case SHARE_TYPE_ALONE:
                        $share->end();
                        break;
                    case SHARE_TYPE_GROUP:
                        $share->clean();
                        break;
                }
            }
        }
    }

    public function getSessionID() {
        return $this->sessionID;
    }

    public function setExpirationTime($expire) {
        $this->sessionData["expire"] = $expire;
        return $this;
    }

    public function getExpirationTime() {
        return $this->sessionData["expire"];
    }

    public function hasExpired() {
        return $this->getExpirationTime() <= time();
    }

    public function setInterval($interval) {
        $this->sessionData["interval"] = $interval;
        return $this;
    }

    public function getInterval() {
        return $this->sessionData["interval"];
    }

    public function addTarget($share) {
        $this->sessionData["targets"][] = $share->getShareID();
        return $this;
    }

    public function getTargets() {
        $shares = array();
        foreach ($this->sessionData["targets"] as $shareID) {
            $shares[] = Share::fromShareID($this->memcache, $shareID);
        }
        return $shares;
    }

    public function addPoint($point) {
        $this->sessionData["points"][] = $point;
        // Ensure that we don't exceed the maximum number of points stored in
        // memcached.
        while (count($this->sessionData["points"]) > CONFIG["max_cached_pts"]) {
            array_shift($this->sessionData["points"]);
        }
        return $this;
    }

    public function getPoints() {
        return $this->sessionData["points"];
    }

    private function generateSessionID() {
        $sid = "";
        do $sid = bin2hex(openssl_random_pseudo_bytes(SESSION_ID_SIZE));
        while ($this->memcache->get(PREFIX_SESSION.$this->sessionID) !== false);
        return $sid;
    }
}

function requirePOST(...$args) {
    foreach ($args as $field) {
        if (!isset($_POST[$field])) die("Missing data!\n");
    }
}

// Hauk maintains compatibility with both `memcache` and `memcached`, meaning we
// need an abstraction layer between Hauk and the extensions to ensure a unified
// interface for both. This code loads the correct memcache wrapper.
if (extension_loaded("memcached")) {
    include_once(__DIR__."/wrapper/memcached.php");
} else if (extension_loaded("memcache")) {
    include_once(__DIR__."/wrapper/memcache.php");
} else {
    die("No compatible memcached extension (memcache or memcached) is enabled in your PHP config!\n");
}

// Returns a memcached instance.
function memConnect() {
    $memcache = new MemWrapper(CONFIG["memcached_host"], CONFIG["memcached_port"]);
    return $memcache;
}
