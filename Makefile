octocat = dev-resources/octocat
midje_nrepl_version = $(shell sed -n '1,1s/^.*"\([^"]*\)".*$$/\1/p' project.clj)
cider_nrepl_version = $(shell grep cider/cider-nrepl project.clj | sed -n '1,1s/.*cider\/cider-nrepl[^"]*"\([^"]*\)".*/\1/p')
refactor_nrepl_version = $(shell grep refactor-nrepl project.clj | sed -n '1,1s/.*refactor-nrepl[^"]*"\([^"]*\)".*/\1/p')

.PHONY: test

test:
	@echo "Running unit tests..."
	@lein midje midje-nrepl.*

setup-integration:
	@echo "Setting up needed stuff to run integration tests..."
	lein install
	cd $(octocat) && \
	lein update-in :plugins conj "[cider/cider-nrepl \"$(cider_nrepl_version)\"]" -- \
		update-in :plugins conj "[refactor-nrepl \"$(refactor_nrepl_version)\"]" -- \
update-in :plugins conj "[nubank/midje-nrepl \"$(midje_nrepl_version)\"]" -- \
		repl :headless & \
		echo $$! > $(octocat)/.nrepl-pid

teardown-integration:
	@echo "Cleaning up integration tests..."
	@cd $(octocat); \
	pid=$$(cat .nrepl-pid); \
	pkill --parent $$pid && \
	rm .nrepl-pid .nrepl-port
	@echo "Done"

test-integration: setup-integration
	@echo "Running integration tests..."
	@lein midje integration.*
	@make teardown-integration

test-all: test test-integration

release: test-all
	@echo "Releasing nubank/midje-nrepl..."
	lein release :patch
	@echo "Done"

clean:
	@lein clean
