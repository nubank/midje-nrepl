#!/usr/bin/env bash
set -eou pipefail

midje_nrepl_version=$(sed -n '1,1s/^.*"\([^"]*\)".*$/\1/p' project.clj)

cider_nrepl_version=$(grep cider/cider-nrepl project.clj | sed -n '1,1s/.*cider\/cider-nrepl[^"]*"\([^"]*\)".*/\1/p')

lein install

cd dev-resources/octocat

# Conj's cider-nrepl and midje-nrepl into the plugins vector and starts the REPL.
set -o xtrace
lein update-in :plugins conj "[cider/cider-nrepl \"${cider_nrepl_version}\"]" -- \
     update-in :plugins conj "[midje-nrepl \"${midje_nrepl_version}\"]" -- \
     repl :headless
