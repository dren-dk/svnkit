#!/bin/sh

usage() {
    echo -e "Usage:"
    echo -e "build.sh [options] [command [command arguments]]"
    echo -e ""
    echo -e "Arguments:"
    echo -e "\t-h --help\tprint this message and exit"
}

DIRECTORY=$(printf "$PWD")
IMAGE_NAME=$(basename "$DIRECTORY")

JAVA_PACKAGE="openjdk-8-jdk-headless"
MAVEN_VERSION="3.6.1"
COMMAND=""
USER_NAME="docker"
DOCKER_HOME="/home/$USER_NAME"
HOST_HOME="$HOME"

while [ "$1" != "" ]; do
    PARAM=`echo $1`
    case ${PARAM} in
        -h | --help)
            usage
            exit
            ;;
        --image-name)
            IMAGE_NAME=`echo $2`
            shift
            ;;
        --java)
            JAVA_PACKAGE=`echo $2`
            shift
            ;;
        -C | --directory)
            DIRECTORY=`echo $2`
            shift
            ;;
        *)
            break
            ;;
    esac
    shift
done

IMAGE_TAG=$(printf "%s_maven-%s" "$JAVA_PACKAGE" "$MAVEN_VERSION")
DOCKER_ARGS="--rm"
if [ -t 1 ] ; then
    DOCKER_ARGS="--interactive --tty --rm"
fi

USER_UID=$(id -u)
USER_GID=$(id -g)

echo "Host home: $HOST_HOME"
echo "Host project directory: $DIRECTORY"
echo "Image: $IMAGE_NAME:$IMAGE_TAG"
echo "Container home: $DOCKER_HOME"
echo "Container args: $DOCKER_ARGS"
echo "Container user: $USER_UID:$USER_GID"
echo "Command: $*"

docker build ${DOCKER_BUILD_OPTS} -t "$IMAGE_NAME:$IMAGE_TAG" \
    --build-arg USER_UID="$USER_UID" \
    --build-arg USER_GID="$USER_GID" \
    --build-arg USER_NAME="$USER_NAME" \
    --build-arg GROUP_NAME="$USER_NAME" \
    --build-arg PROJECT_HOME="$DOCKER_HOME/project" \
    --build-arg USER_HOME="$DOCKER_HOME" \
    --build-arg JAVA_PACKAGE="$JAVA_PACKAGE" \
    "$DIRECTORY"

docker run  ${DOCKER_RUN_OPTS} \
    -v "$DIRECTORY:$DOCKER_HOME/project" \
    -v "$HOST_HOME/.m2:$DOCKER_HOME/.m2" \
    -v "$HOST_HOME/.gradle:$DOCKER_HOME/.gradle" \
    -v "$HOST_HOME/.mavenrc:$DOCKER_HOME/.mavenrc" \
    -v "$HOST_HOME/.gnupg:$DOCKER_HOME/.gnupg" \
    -v "$HOST_HOME/ca:$DOCKER_HOME/ca" \
    ${DOCKER_ARGS} \
    "$IMAGE_NAME:$IMAGE_TAG" \
    $*
