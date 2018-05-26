#!/usr/bin/env bash
set -eou pipefail

cur_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

pid_file="${cur_dir}/.pid"

pid=$(cat $pid_file)

pkill --parent $pid

rm $pid_file
