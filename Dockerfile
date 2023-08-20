FROM    php:apache

RUN     apt-get update && \
        apt-get install -y memcached libmemcached-dev zlib1g-dev libldap2-dev libssl-dev && \
        pecl install memcached && \
        docker-php-ext-enable memcached && \
        docker-php-ext-configure ldap --with-libdir=lib/*-linux-gnu*/ && \
        docker-php-ext-install ldap

EXPOSE  80/tcp
VOLUME  /etc/hauk
STOPSIGNAL SIGINT

COPY    docker/start.sh .
RUN     chmod +x ./start.sh
COPY    backend-php/ /var/www/html/
COPY    frontend/ /var/www/html/

CMD     ["./start.sh"]
