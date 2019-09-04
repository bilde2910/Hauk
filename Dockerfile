FROM    php:apache
COPY    backend-php/ /var/www/html/
COPY    docker/start.sh .

RUN     apt-get update && \
        apt-get install -y memcached libmemcached-dev zlib1g-dev && \
        pecl install memcached && \
        docker-php-ext-enable memcached

EXPOSE  80/tcp
VOLUME  /etc/hauk

CMD     ["/bin/sh", "start.sh"]
