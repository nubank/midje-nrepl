#!/usr/bin/env bash
set -eou pipefail

# Extracts the current version from the project.clj.
version=$(sed -n '1,1s/^.*"\([^"]*\)".*$/\1/p' project.clj)

lein install

cd dev-resources/octocat

# Conj's midje-nrepl into the plugins vector and starts the REPL.
set -o xtrace
lein update-in :plugins conj "[midje-nrepl \"${version}\"]" -- \
     repl :headless
