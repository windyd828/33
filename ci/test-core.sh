#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/core-tests
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d build/core-tests \
  app/src/main/java/com/example/headsetblocker/KeyRules.java \
  tests/KeyRulesTest.java
java -cp build/core-tests com.example.headsetblocker.KeyRulesTest
