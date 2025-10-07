#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


#k8s-deploy 'inventory-springboot-deployment.yaml'

kubectl delete deployment inventory  -n financial

kubectl apply -f inventory-springboot-deployment.yaml -n financial

