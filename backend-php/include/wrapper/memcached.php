<?php

// This class is a wrapper class for the `memcached` extension in PHP. Hauk
// maintains compatibility with both `memcache` and `memcached`, and therefore
// needs an abstraction layer between itself and the extensions to ensure a
// unified interface for both.

class MemWrapper {
    private $memcache = null;

    function __construct($host, $port) {
        // If the hostname starts with "unix://", this is a UNIX socket.
        // The `memcached` extension only needs the full pathname and not the
        // unix scheme, so we strip the scheme from the path.
        $host = getConfig("memcached_host");
        if (substr($host, 0, 7) == "unix://") $host = substr($host, 7);

        $this->memcache = new Memcached();
        if (getConfig("memcached_binary")) {
            $this->memcache->setOption(Memcached::OPT_BINARY_PROTOCOL, true);
            if (getConfig("memcached_use_sasl")) {
                $this->memcache->setSaslAuthData(getConfig("memcached_sasl_user"), getConfig("memcached_sasl_pass"));
            }
        }
        $this->memcache->addServer($host, getConfig("memcached_port"))
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
