FROM debian:jessie

ARG USER_NAME=docker
ARG GROUP_NAME=$USER_NAME
ARG USER_HOME=/home/$USER_NAME
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG JAVA_VERSION=8

RUN \
  echo "deb http://httpredir.debian.org/debian jessie main contrib" > /etc/apt/sources.list && \
  echo "deb http://httpredir.debian.org/debian jessie-updates main contrib" >> /etc/apt/sources.list && \
  echo "deb http://security.debian.org jessie/updates main contrib" >> /etc/apt/sources.list

RUN \
  apt-get update

RUN \
    echo "===> add webupd8 repository..."  && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list  && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list  && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886  && \
    apt-get update && \
    \
    \
    echo "===> install Java"  && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections  && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections  && \
    DEBIAN_FRONTEND=noninteractive  apt-get install -y --force-yes oracle-java$JAVA_VERSION-installer oracle-java$JAVA_VERSION-set-default  && \
    \
    \
    echo "===> clean up..."  && \
    rm -rf /var/cache/oracle-jdk$JAVA_VERSION-installer

RUN \
   apt-get install -y \
   build-essential \
   cmake \
   mingw32 \
   gcc-multilib \
   libc6-i386 \
   libc6-dev-i386

RUN \
   apt-get install -y \
   subversion \
   python \
   apache2

RUN groupadd -g $USER_GID $GROUP_NAME
RUN useradd -u $USER_UID -g $USER_GID -m -d $USER_HOME -s /bin/bash $USER_NAME
USER $USER_NAME

ENV JAVA_HOME /usr/lib/jvm/java-$JAVA_VERSION-oracle

WORKDIR $USER_HOME/project
