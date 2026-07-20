#!/usr/bin/env bash

project_env="../.env"
financial_env="../../financial/setup/.env"

if [ -f "$project_env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$project_env"
  set +a
elif [ -f "$financial_env" ]; then
  set -a
  # Reuse the ignored financial demo credentials without copying the secret.
  # shellcheck disable=SC1091
  source "$financial_env"
  set +a
  export DB_USERNAME="${DB_USERNAME:-${DB_USER:-FINANCIAL}}"
  export TNS_ADMIN="${TNS_ADMIN:-${WALLET_DIR:-}}"
  # The financial deployment URL contains its container-only wallet path.
  # Let the Java configuration build a local URL from TNS_ADMIN instead.
  unset DB_URL
fi
