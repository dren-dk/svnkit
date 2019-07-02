FROM debian:stretch-slim

ARG JAVA_PACKAGE=openjdk-8-jdk-headless

RUN \
  apt-get update \
  && mkdir -p /usr/share/man/man1 \
  && apt-get install -y --no-install-recommends \
     wget \
     gnupg1 \
     $JAVA_PACKAGE \
  && apt-get clean all \
  && rm -rf /var/lib/apt/lists/* \
  && /bin/sh -c 'echo "export JAVA_HOME=$(readlink -f /usr/bin/javac | sed 's:/bin/javac::')"' >> /etc/profile

RUN \
  apt-get update \
  && apt-get install -y --no-install-recommends \
     build-essential \
     cmake \
     mingw-w64 \
     gcc-multilib \
     libc6-i386 \
     libc6-dev-i386 \
     subversion \
     python \
     apache2 \
  && apt-get clean all \
  && rm -rf /var/lib/apt/lists/*

ARG USER_NAME=docker
ARG GROUP_NAME=$USER_NAME
ARG USER_HOME=/home/$USER_NAME
ARG PROJECT_HOME=$USER_HOME/project
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN \
    groupadd -g $USER_GID $GROUP_NAME \
    && useradd -u $USER_UID -g $USER_GID -m -d $USER_HOME -s /bin/bash $USER_NAME

USER $USER_NAME
WORKDIR $PROJECT_HOME
