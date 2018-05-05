#!/usr/bin/env bash
set -eou pipefail

cur_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

source ${cur_dir}/setup-integration.sh &
sleep 3 &&
    lein integration
