FROM    php:apache
COPY    backend-php/ /var/www/html/
COPY    frontend/ /var/www/html/
COPY    docker/start.sh .

RUN     apt-get update && \
        apt-get install -y memcached libmemcached-dev zlib1g-dev libldap2-dev && \
        pecl install memcached redis && \
        docker-php-ext-enable memcached && \
        docker-php-ext-enable redis && \
        docker-php-ext-configure ldap --with-libdir=lib/*-linux-gnu*/ && \
        docker-php-ext-install ldap

EXPOSE  80/tcp
VOLUME  /etc/hauk

STOPSIGNAL SIGINT
RUN     chmod +x ./start.sh
CMD     ["./start.sh"]
