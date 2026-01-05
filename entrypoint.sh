#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "--- 🛠️ DEPLOYMENT STARTED ---"

# 1. Compile Java
echo "Compiling Java..."
javac -d java_core/bin java_core/src/*.java

# 2. Start the Java Server in BACKGROUND (&)
echo "Starting Java Neural Engine (TensorLess)..."
java -cp java_core/bin PredictorServer &

# 3. Wait for Java to wake up
echo "Waiting for Java to initialize..."
sleep 3

echo "Applying Database Migrations..."
python manage.py migrate

# 4. Start Django (Gunicorn) in FOREGROUND
# Render provides the $PORT variable automatically
echo "Starting Django Web Server..."
gunicorn NumberRecognitionApp.wsgi:application --bind 0.0.0.0:$PORT