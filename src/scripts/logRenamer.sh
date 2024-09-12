#!/bin/bash

# Define the original log file name
log_file="../logs/output.log"

# Check if the log file exists
if [ -f "$log_file" ]; then
    # Get the current date and time in the format YYYY-MM-DD_HH-MM-SS
    current_time=$(date +'%Y-%m-%d_%H-%M-%S')
    
    # Define the new log file name with the date and time appended
    new_log_file="../logs/output_$current_time.log"
    
    # Rename the log file
    mv "$log_file" "$new_log_file"
    
    echo "Log file renamed to $new_log_file"
else
    echo "Log file $log_file not found!"
fi
