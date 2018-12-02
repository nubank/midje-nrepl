# midje-nrepl

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Clojars Project](https://img.shields.io/clojars/v/nubank/midje-nrepl.svg)](https://clojars.org/nubank/midje-nrepl)
[![Build status](https://circleci.com/gh/nubank/midje-nrepl.svg?style=svg)](https://circleci.com/gh/nubank/midje-nrepl)

nREPL middleware to interact with Midje

The goal of midje-nrepl is to provide a better support for interacting with
[Midje][midje] from Clojure tools such as [Cider][cider]. It offers a set of
features for running Midje tests, getting report information and formatting
facilities to be used within a [nREPL][nrepl] session.

## Usage

### With Cider and Emidje

If you are using [Cider][cider] and [Emidje][emidje] within Emacs, just call
`cider-jack-in` and the appropriate `midje-nrepl`'s version will be injected in
the `REPL` automatically.

If you are connecting to an already running `REPL` process, `midje-nrepl` should
be added explicitly. Add the following, either in your project's `project.clj`,
or in the `:user` profile found at `~/.lein/profiles.clj`:

```clojure
:plugins [[cider/cider-nrepl "0.18.0"]
[nubank/midje-nrepl "1.0.0"]]
```

Notice that currently only `Leiningen` is supported.

## Available features

* Inhibit Midje facts from being run on certain nREPL operations such as `eval`,
  `load-file`, `refresh`, `refresh-all` and `warm-ast-cache` (when
  [refactor-nrepl][refactor-nrepl]) is available on the project's
  classpath. This is useful specially on large projects with slow and heavy
  tests, where those run inadvertently as a side effect of the aforementioned
  operations.
* Provide a set of operations for dealing with Midje tests: to run a given fact,
  a set of facts in a given namespace, all facts defined in the project, allow
  re-running non-passing tests, etc.
* Support auto-formatting tabular facts.

## Changelog

An extensive changelog is available [here](CHANGELOG.md).

## Testing

Type `make test` to run unit tests (those under the `test` directory). Type
`make test-integration` to run integration tests (those under the `integration`
directory). They are useful for exercising a real communication with a nREPL
server running within a fake project. If you are working interactively on a REPL
and want to run integration tests, type `make setup-integration` to set-up the
needed stuff. Type `make test-all` to run both kinds of tests.

## License
Copyright © 2018 Nubank

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License.  You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

[cider]: https://github.com/clojure-emacs/cider
[emidje]: https://github.com/nubank/emidje
[midje]: https://github.com/marick/Midje
[nrepl]: https://github.com/nrepl/nrepl
[refactor-nrepl]: https://github.com/clojure-emacs/refactor-nrepl
