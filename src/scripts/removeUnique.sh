#!/bin/bash

# Input log file
LOG_FILE="output (copy).log"

# Temporary file to store the result
TEMP_FILE="temp_log_file.log"

# Find and keep only repeated lines along with their count
awk '{ count[$0]++ } END { for (line in count) if (count[line] > 1) print count[line], line }' "$LOG_FILE" > "$TEMP_FILE"

# Replace original log file with the filtered log file
mv "$TEMP_FILE" "$LOG_FILE"

echo "Unique lines removed. Repeated lines are saved in $LOG_FILE."
