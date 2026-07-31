#!/usr/bin/env bash

# Development environment deployment only.
# This script is baked into the dev AMI and must not be used for production deployments.

set -Eeuo pipefail

AWS_REGION="ap-northeast-2"
ECR_REPOSITORY="brifo-ecr"
PARAMETER_PREFIX="/brifo/dev"

CONTAINER_NAME="brifo-server"
HOST_PORT="8080"
CONTAINER_PORT="8080"
HEALTH_CHECK_TIMEOUT_SECONDS="60"
DB_CA_CERT_HOST_PATH="/home/ec2-user/ssl/global-bundle.pem"
DB_CA_CERT_CONTAINER_PATH="/app/ssl/global-bundle.pem"

IMAGE_TAG="${1:?Usage: deploy.sh <image-tag>}"
HEALTH_URL="http://127.0.0.1:${HOST_PORT}/actuator/health"

if [[ ! "${IMAGE_TAG}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid image tag: expected a 40-character Git SHA." >&2
  exit 1
fi

get_required_parameter() {
  aws ssm get-parameter \
    --name "${PARAMETER_PREFIX}/$1" \
    --with-decryption \
    --query "Parameter.Value" \
    --output text \
    --region "${AWS_REGION}"
}

run_container() (
  local image="$1"

  DB_URL="$(get_required_parameter "DB_URL")" || return 1
  DB_PASSWORD="$(get_required_parameter "DB_PASSWORD")" || return 1
  JWT_SECRET_BASE64="$(get_required_parameter "JWT_SECRET_BASE64")" || return 1
  KAKAO_CLIENT_SECRET="$(get_required_parameter "KAKAO_CLIENT_SECRET")" || return 1
  NAVER_CLIENT_SECRET="$(get_required_parameter "NAVER_CLIENT_SECRET")" || return 1

  export \
    DB_URL \
    DB_PASSWORD \
    JWT_SECRET_BASE64 \
    KAKAO_CLIENT_SECRET \
    NAVER_CLIENT_SECRET

  docker stop --time 10 "${CONTAINER_NAME}" >/dev/null 2>&1 || true
  docker rm "${CONTAINER_NAME}" >/dev/null 2>&1 || true

  docker run --detach \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    --publish "${HOST_PORT}:${CONTAINER_PORT}" \
    --volume "${DB_CA_CERT_HOST_PATH}:${DB_CA_CERT_CONTAINER_PATH}:ro" \
    --env SPRING_PROFILES_ACTIVE=dev \
    --env DB_URL \
    --env DB_USERNAME="${DB_USERNAME}" \
    --env DB_PASSWORD \
    --env JWT_SECRET_BASE64 \
    --env KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
    --env KAKAO_CLIENT_SECRET \
    --env KAKAO_REDIRECT_URIS="${KAKAO_REDIRECT_URIS}" \
    --env NAVER_CLIENT_ID="${NAVER_CLIENT_ID}" \
    --env NAVER_CLIENT_SECRET \
    --env NAVER_REDIRECT_URIS="${NAVER_REDIRECT_URIS}" \
    --env CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS}" \
    "${image}"
)

wait_for_health() {
  local started_at="${SECONDS}"

  while (( SECONDS - started_at < HEALTH_CHECK_TIMEOUT_SECONDS )); do
    if curl \
      --fail \
      --silent \
      --connect-timeout 2 \
      --max-time 3 \
      "${HEALTH_URL}" >/dev/null; then
      return 0
    fi

    if [[ "$(docker inspect --format "{{.State.Running}}" "${CONTAINER_NAME}" 2>/dev/null || true)" != "true" ]]; then
      return 1
    fi

    sleep 2
  done

  return 1
}

for command in aws docker curl; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is not installed: ${command}" >&2
    exit 1
  fi
done

if [[ ! -r "${DB_CA_CERT_HOST_PATH}" ]]; then
  echo "PostgreSQL CA certificate is not readable: ${DB_CA_CERT_HOST_PATH}" >&2
  exit 1
fi

AWS_ACCOUNT_ID="$(
  aws sts get-caller-identity \
    --query "Account" \
    --output text \
    --region "${AWS_REGION}"
)"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
ECR_LOGGED_IN=false

cleanup() {
  unset \
    DB_USERNAME \
    KAKAO_CLIENT_ID \
    KAKAO_REDIRECT_URIS \
    NAVER_CLIENT_ID \
    NAVER_REDIRECT_URIS \
    CORS_ALLOWED_ORIGINS

  if [[ "${ECR_LOGGED_IN}" == "true" ]]; then
    docker logout "${ECR_REGISTRY}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

DB_USERNAME="$(get_required_parameter "DB_USERNAME")"
KAKAO_CLIENT_ID="$(get_required_parameter "KAKAO_CLIENT_ID")"
KAKAO_REDIRECT_URIS="$(get_required_parameter "KAKAO_REDIRECT_URIS")"
NAVER_CLIENT_ID="$(get_required_parameter "NAVER_CLIENT_ID")"
NAVER_REDIRECT_URIS="$(get_required_parameter "NAVER_REDIRECT_URIS")"
CORS_ALLOWED_ORIGINS="$(get_required_parameter "CORS_ALLOWED_ORIGINS")"

aws ecr get-login-password --region "${AWS_REGION}" |
  docker login \
    --username AWS \
    --password-stdin "${ECR_REGISTRY}"
ECR_LOGGED_IN=true

docker pull "${IMAGE}"

if ! run_container "${IMAGE}" >/dev/null; then
  echo "Failed to start the new container." >&2
  exit 1
fi

if ! wait_for_health; then
  echo "Deployment failed: the new container is unhealthy." >&2
  exit 1
fi

echo "Deployment completed for image tag: ${IMAGE_TAG}"
