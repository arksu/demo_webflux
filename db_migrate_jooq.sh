#!/bin/sh

# очистить базу и накатить миграцию заново

export FLYWAY_CLEAN_DISABLED=false
./gradlew flywayClean flywayMigrate generateJooq  