# Change Log

All notable changes to this project will be documented in this file. This change
log follows the conventions of [keepachangelog.com](http://keepachangelog.com/)
and this project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Allow for nREPL clients to run all tests in a subset of the known test paths
  of the project, by sending the parameter `test-paths` in the message.

## [1.0.1] - 2018-12-05

### Added
- Support for running tests on [CircleCI](https://circleci.com/).

### Fixed
- Return proper descriptions for failing tabular facts.

## [1.0.0] - 2018-11-12

### Added

- Initial version with a set of features for running Midje tests, getting report
  information and formatting tabular facts within a NREPL session.

[Unreleased]: https://github.com/nubank/midje-nrepl/compare/1.0.1...HEAD
[1.0.1]: https://github.com/nubank/midje-nrepl/compare/1.0.0...1.0.1
