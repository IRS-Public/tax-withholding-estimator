#!/bin/bash
# Sorts keys in the order of the template files
#
# If we change the order of content, keys in the yaml files won't change but
# the order will change in the generated english file. These changes won't be
# reflected in the spanish yaml file. This script helps ensure we can keep them
# in the same order. We can use this script to reorganize the spanish yaml file
# to match the generated english yaml file.
#
# Dependencies: yq
# Usage: ./reorder.sh flow_en.yaml flow_es.yaml
TEMPLATE_FILE=$1
TARGET_FILE=$2

# Exporting so yq can access the file via strenv()
export TEMPLATE_FILE
yq -i 'load(strenv(TEMPLATE_FILE)) *+ .' "$TARGET_FILE"
