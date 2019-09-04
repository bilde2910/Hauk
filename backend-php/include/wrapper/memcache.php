<?php

// This class is a wrapper class for the `memcache` extension in PHP. Hauk
// maintains compatibility with both `memcache` and `memcached`, and therefore
// needs an abstraction layer between itself and the extensions to ensure a
// unified interface for both.

class MemWrapper {
    private $memcache = null;

    function __construct($host, $port) {
        $this->memcache = new Memcache();
        $this->memcache->connect(CONFIG["memcached_host"], CONFIG["memcached_port"])
                   or die ("Server could not connect to memcached!\n");
    }

    function get($key) {
        $data = $this->memcache->get($key);
        if ($data === false) return false;
        return json_decode($data, true);
    }

    function set($key, $data, $duration) {
        $this->memcache->set($key, json_encode($data), 0, time() + $duration);
    }

    function replace($key, $data) {
        $this->memcache->replace($key, json_encode($data));
    }

    function delete($key) {
        $this->memcache->delete($key);
    }
}

?>
