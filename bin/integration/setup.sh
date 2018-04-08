#!/usr/bin/env bash
set -eou pipefail

lein install

cd dev-resources/octocat

lein repl :headless
