FROM ubuntu:20.04
ENV dirty=1
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get dist-upgrade -y
RUN apt-get install -y apache2 libapache2-mod-perl2 libdbd-pgsql libcgi-pm-perl libdbi-perl cron libfile-slurp-perl libcrypt-eksblowfish-perl \
                       libmime-tools-perl libtext-vcard-perl libxml-writer-perl libdbd-pg-perl libchart-perl postgresql-client \
		       libcrypt-rijndael-perl libyaml-perl
#RUN apt-get install -y 

RUN useradd -m -s /bin/bash hal
RUN a2enmod rewrite

COPY --chown=hal:hal . /home/hal/hal
COPY config/docker.conf /etc/apache2/sites-enabled/000-default.conf
RUN chmod a+r -R /home/hal/hal && chmod a+x `find  /home/hal/hal -type d`;

CMD /home/hal/hal/config/docker-run
