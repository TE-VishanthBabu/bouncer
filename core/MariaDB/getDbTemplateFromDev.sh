#!/usr/bin/#!/usr/bin/env bash
ssh azure@4.227.239.92 "mysqldump -uvagrant -pvagrant --all-databases > siaft-db-template.sql.new"
scp azure@4.227.239.92:/home/azure/siaft-db-template.sql.new .
