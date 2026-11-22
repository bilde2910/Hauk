FROM    php:8.5-apache
COPY --from=ghcr.io/php/pie:bin /pie /usr/bin/pie
COPY    backend-php/ /var/www/html/
COPY    frontend/ /var/www/html/
COPY    docker/start.sh .

RUN     apt-get update && \
        apt-get install -y memcached libmemcached-dev zlib1g-dev libldap2-dev libtool libssl-dev unzip && \
        pie install php-memcached/php-memcached && \
        docker-php-ext-enable memcached && \
        docker-php-ext-configure ldap --with-libdir=lib/*-linux-gnu*/ && \
        docker-php-ext-install ldap

EXPOSE  80/tcp
VOLUME  /etc/hauk

STOPSIGNAL SIGINT
RUN     chmod +x ./start.sh
CMD     ["./start.sh"]
