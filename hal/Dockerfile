FROM ubuntu:18.04

RUN apt-get update
RUN apt-get dist-upgrade -y
RUN apt-get install -y apache2 libapache2-mod-perl2 libdbd-pgsql libcgi-pm-perl
RUN apt-get install -y libdbi-perl cron

RUN useradd -m -s /bin/bash hal
RUN a2enmod rewrite

COPY . /home/hal/hal
COPY config/docker.conf /etc/apache2/sites-enabled/000-default.conf


CMD /home/hal/hal/config/docker-run