<?php

// This class is a wrapper class for the `redis` extension in PHP, for
// communication with Redis backends.

class MemWrapper {
    private $redis = null;

    function __construct() {
        $this->redis = new Redis();
        if(substr(getConfig("redis_host"), 0, 1) === "/") {
                $this->redis->connect(getConfig("redis_host"))
                           or die ("Server could not connect to Redis socket!\n");
        } else {
                $this->redis->connect(getConfig("redis_host"), getConfig("redis_port"))
                           or die ("Server could not connect to Redis!\n");
        }
        if (getConfig("redis_use_auth")) {
            $this->redis->auth(getConfig("redis_auth"))
                or die("Incorrect Redis authentication!");
        }
    }

    function get($key) {
        $data = $this->redis->get(getConfig("redis_prefix").$key);
        if ($data === false) return false;
        return json_decode($data, true);
    }

    function set($key, $data, $expire) {
        $this->redis->setEx(getConfig("redis_prefix").$key, $expire - time(), json_encode($data));
    }

    function delete($key) {
        $this->redis->del(getConfig("redis_prefix").$key);
    }
}

?>
