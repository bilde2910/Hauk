<?php

// This class is a wrapper class for the `memcache` extension in PHP. Hauk
// maintains compatibility with both `memcache` and `memcached`, and therefore
// needs an abstraction layer between itself and the extensions to ensure a
// unified interface for both.

class MemWrapper {
    private $memcache = null;

    function __construct($host, $port) {
        $this->memcache = new Memcache();
        $this->memcache->connect(getConfig("memcached_host"), getConfig("memcached_port"))
                   or die ("Server could not connect to memcached!\n");
    }

    function get($key) {
        $data = $this->memcache->get($key);
        if ($data === false) return false;
        return json_decode($data, true);
    }

    function set($key, $data, $expire) {
        $this->memcache->set($key, json_encode($data), 0, $expire);
    }

    function replace($key, $data, $expire) {
        $this->memcache->replace($key, json_encode($data), 0, $expire);
    }

    function delete($key) {
        $this->memcache->delete($key);
    }
}

?>
