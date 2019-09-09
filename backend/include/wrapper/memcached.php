<?php

// This class is a wrapper class for the `memcached` extension in PHP. Hauk
// maintains compatibility with both `memcache` and `memcached`, and therefore
// needs an abstraction layer between itself and the extensions to ensure a
// unified interface for both.

class MemWrapper {
    private $memcache = null;

    function __construct($host, $port) {
        $host = CONFIG["memcached_host"];
        if (substr($host, 0, 7) == "unix://") $host = substr($host, 7);
        $this->memcache = new Memcached();
        $this->memcache->addServer($host, CONFIG["memcached_port"])
                   or die ("Server could not connect to memcached!\n");
    }

    function get($key) {
        $data = $this->memcache->get($key);
        if ($data === false) return false;
        return json_decode($data, true);
    }

    function set($key, $data, $expire) {
        $this->memcache->set($key, json_encode($data), $expire);
    }

    function replace($key, $data) {
        $this->memcache->replace($key, json_encode($data), $expire);
    }

    function delete($key) {
        $this->memcache->delete($key);
    }
}

?>
