FROM    php:8.3-apache

# Install dependencies and PHP extensions
RUN     apt-get update && \
        apt-get install -y --no-install-recommends \
            memcached \
            libmemcached-dev \
            zlib1g-dev \
            libldap2-dev \
            libssl-dev && \
        pecl install memcached-3.2.0 && \
        docker-php-ext-enable memcached && \
        docker-php-ext-configure ldap --with-libdir=lib/x86_64-linux-gnu/ && \
        docker-php-ext-install ldap && \
        apt-get clean && \
        rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Copy application files
COPY    backend-php/ /var/www/html/
COPY    frontend/ /var/www/html/
COPY    docker/start.sh .

# Set secure permissions
RUN     chown -R www-data:www-data /var/www/html && \
        find /var/www/html -type d -exec chmod 755 {} \; && \
        find /var/www/html -type f -exec chmod 644 {} \;

EXPOSE  80/tcp
VOLUME  /etc/hauk

STOPSIGNAL SIGINT
RUN     chmod +x ./start.sh
CMD     ["./start.sh"]
