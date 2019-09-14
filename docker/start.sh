#!/bin/bash

stop_all() 
{
    echo "Stopping apache"
    kill $APACHE_PID

    echo "Stopping memcached"
    kill $MEMCACHED_PID
}

trap stop_all INT TERM

memcached -u memcache &
MEMCACHED_PID=$!

apachectl -D FOREGROUND &
APACHE_PID=$!

echo "Started"
wait $APACHE_PID $MEMCACHED_PID
trap - TERM INT
wait $APACHE_PID $MEMCACHED_PID

echo "Done"