# Plain JDK (17+), no build tool needed.
JAVAC ?= javac
SOURCES := $(shell find src -name '*.java')

build:
	$(JAVAC) -d out $(SOURCES)

test: build
	java -cp out csp.Tests

bench: build
	java -cp out csp.bench.Benchmark 60

figures:
	python3 scripts/plot.py

.PHONY: build test bench figures
