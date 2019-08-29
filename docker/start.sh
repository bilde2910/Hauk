#!/bin/sh
memcached -u memcache &
apachectl -D FOREGROUND
