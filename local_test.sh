export WORKSPACE=/home/patrick/Workspace/thumbnail-api
export BRANCH=EA-4001_multiple_pods_experimental
cp $WORKSPACE/src/main/resources/thumbnail.user.properties $WORKSPACE/k8s/base/

PROJECT_NAME="pv-poc"
DOCKER_IMAGE_NAME="europeana/thumbnail-api"
DEPLOYMENT_SUFFIX=test

K8S_DEPLOYMENT_NAME=${PROJECT_NAME}-statefulset-${DEPLOYMENT_SUFFIX}

K8S_DEPLOYMENT_NAME=${PROJECT_NAME}-statefulset-${DEPLOYMENT_SUFFIX}
# Namespace to deploy resources to: eg. test, acceptance, production
export K8S_HOSTNAME=pvpoc.test.eanadev.org
export K8S_SECRETNAME=${K8S_HOSTNAME}
export K8S_SERVER_ALIASES=
# Note that the newline and 4 spaces before nginx.ingress are intentional and should not be changed
export K8S_INGRESS_ANNOTATIONS="cert-manager.io/issuer: letsencrypt-production
    nginx.ingress.kubernetes.io/server-alias: ${K8S_SERVER_ALIASES}"

KUSTOMIZE_BASE=$WORKSPACE/k8s/base
KUSTOMIZE_OVERLAY=$WORKSPACE/k8s/overlays/cloud

# Exit on first error
set -e


# Additional properties required for APM (won't be used if COLLECT_LOGS is false)
export ELASTIC_APM_SERVERS=https://apm.eanadev.org:8200
export ELASTIC_APP_PACKAGES=eu.europeana
export APP_NAME=${PROJECT_NAME}-${DEPLOYMENT_SUFFIX}

echo "Substituting variables..."

envsubst < $KUSTOMIZE_OVERLAY/deployment_patch.yaml.template > $KUSTOMIZE_OVERLAY/deployment_patch.yaml
envsubst < $KUSTOMIZE_OVERLAY/ingress.yaml.template > $KUSTOMIZE_OVERLAY/ingress.yaml
#envsubst < $KUSTOMIZE_OVERLAY/hpa.yaml.template > $KUSTOMIZE_OVERLAY/hpa.yaml

echo "Running kustomize..."

# run "kustomize edit" commands from overlay directory until https://github.com/kubernetes-sigs/kustomize/issues/2803 is resolved
cd $KUSTOMIZE_OVERLAY
echo "Switched to folder $KUSTOMIZE_OVERLAY"

kustomize edit set image ${DOCKER_IMAGE_NAME}:${BRANCH}
echo "Docker image = ${DOCKER_IMAGE_NAME}:${BRANCH}"

# Append deployment environment to all resources (eg -test)
kustomize edit set namesuffix -- -$DEPLOYMENT_SUFFIX
echo "Set namesuffix = $DEPLOYMENT_SUFFIX"

# Add label to all resources created during this deployment (used for cleanup above)
kustomize edit set label app:${APP_NAME}
echo "Set appname = ${APP_NAME}"

# Prints out the resources that will be applied
kustomize build $KUSTOMIZE_OVERLAY
echo "Build overlay =  $KUSTOMIZE_OVERLAY"

#kubectl apply -k $KUSTOMIZE_OVERLAY -n test