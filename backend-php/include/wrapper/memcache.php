<?php

// This class is a wrapper class for the `memcache` extension in PHP. Hauk
// maintains compatibility with both `memcache` and `memcached`, and therefore
// needs an abstraction layer between itself and the extensions to ensure a
// unified interface for both.

class MemWrapper {
    private $memcache = null;

    function __construct() {
        $this->memcache = new Memcache();
        $this->memcache->connect(getConfig("memcached_host"), getConfig("memcached_port"))
                   or die ("Server could not connect to memcached!\n");
    }

    function get($key) {
        $data = $this->memcache->get(getConfig("memcached_prefix").$key);
        if ($data === false) return false;
        return json_decode($data, true);
    }

    function set($key, $data, $expire) {
        $this->memcache->set(getConfig("memcached_prefix").$key, json_encode($data), 0, $expire);
    }

    function delete($key) {
        $this->memcache->delete(getConfig("memcached_prefix").$key);
    }
}

?>
