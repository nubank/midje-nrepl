# Change Log

All notable changes to this project will be documented in this file. This change
log follows the conventions of [keepachangelog.com](http://keepachangelog.com/)
and this project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- [#12](https://github.com/nubank/midje-nrepl/pull/12): add two new options to
  `wrap-test` middleware, `:ns-exclusions` and `:ns-inclusions`. Those options
  allow for clients to send lists of regexes to filter namespaces to be excluded
  or included from/to the test execution.
  - [#13](https://github.com/nubank/midje-nrepl/pull/13): allow for clients to
    collect profiling statistics about tests. By default, only the total time
    taken by the test suite is returned. Clients can obtain more detailed
    information by sending the parameter `profile?` in the request.

### Fixed
- [#14](https://github.com/nubank/midje-nrepl/pull/14): compute failures
  properly within [Selvage](https://github.com/nubank/selvage) flows where facts
  are retried.

## [1.1.0] - 2018-12-19

### Added
- Allow for nREPL clients to run all tests in a subset of the known test paths
  of the project, by sending the parameter `test-paths` in the message.
- Allow for nREPL clients to retrieve the list of project's test paths and
  namespaces declared within those paths. Those operations are handled by the
  new `wrap-test-info` middleware.

## [1.0.1] - 2018-12-05

### Added
- Support for running tests on [CircleCI](https://circleci.com/).

### Fixed
- Return proper descriptions for failing tabular facts.

## [1.0.0] - 2018-11-12

### Added

- Initial version with a set of features for running Midje tests, getting report
  information and formatting tabular facts within a NREPL session.

[Unreleased]: https://github.com/nubank/midje-nrepl/compare/1.1.0...HEAD
[1.1.0]: https://github.com/nubank/midje-nrepl/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/nubank/midje-nrepl/compare/1.0.0...1.0.1
