#!/bin/bash

export TAG=0.1.$(date +%s)
echo TAG = $TAG

export IMAGE_NAME=kafka-sql-inventory
export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=$DOCKER_REGISTRY
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}



./build.sh
./deploy.sh
echo logpod kafka-sql-inventory
sleep 5
logpod kafka-sql-inventory
