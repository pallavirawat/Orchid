#!/usr/bin/env bash
set -e

./gradlew assemble :OrchidCore:orchidBuild -Penv=prod
