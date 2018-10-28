#!/usr/bin/env bash
set -eou pipefail

cur_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

function print_and_exec() {
    echo "$ $@" 1>&2; "$@"
}

midje_nrepl_version=$(sed -n '1,1s/^.*"\([^"]*\)".*$/\1/p' project.clj)

cider_nrepl_version=$(grep cider/cider-nrepl project.clj | sed -n '1,1s/.*cider\/cider-nrepl[^"]*"\([^"]*\)".*/\1/p')

refactor_nrepl_version=$(grep refactor-nrepl project.clj | sed -n '1,1s/.*refactor-nrepl[^"]*"\([^"]*\)".*/\1/p')

print_and_exec lein install

cd dev-resources/octocat

# Conj's cider-nrepl, refactor-nrepl and midje-nrepl into the plugins vector and starts the REPL.
print_and_exec lein update-in :plugins conj "[cider/cider-nrepl \"${cider_nrepl_version}\"]" -- \
               update-in :plugins conj "[refactor-nrepl \"${refactor_nrepl_version}\"]" -- \
               update-in :plugins conj "[nubank/midje-nrepl \"${midje_nrepl_version}\"]" -- \
               repl :headless & echo $! > ${cur_dir}/.pid
