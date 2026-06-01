#!/usr/bin/env bash
set -e
mvn -pl oidf-services spring-boot:run -Dspring-boot.run.profiles=demo
