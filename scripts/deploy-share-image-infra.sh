#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <EC2_INSTANCE_ROLE_NAME>" >&2
  exit 2
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
TEMPLATE_FILE="${REPOSITORY_DIR}/infra/share-image-dev.yaml"
STACK_NAME="${STACK_NAME:-brifo-dev-share-image}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
INSTANCE_ROLE_NAME="$1"

PARAMETER_OVERRIDES=(
  "DeploymentInstanceRoleName=${INSTANCE_ROLE_NAME}"
  "ParameterPrefix=/brifo/dev"
)

if [[ -n "${SHARE_IMAGE_BUCKET_NAME:-}" ]]; then
  PARAMETER_OVERRIDES+=("ShareImageBucketName=${SHARE_IMAGE_BUCKET_NAME}")
fi

aws sts get-caller-identity --region "${AWS_REGION}" >/dev/null

aws cloudformation deploy \
  --region "${AWS_REGION}" \
  --stack-name "${STACK_NAME}" \
  --template-file "${TEMPLATE_FILE}" \
  --capabilities CAPABILITY_NAMED_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides "${PARAMETER_OVERRIDES[@]}"

aws cloudformation describe-stacks \
  --region "${AWS_REGION}" \
  --stack-name "${STACK_NAME}" \
  --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' \
  --output table
