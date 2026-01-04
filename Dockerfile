# 1. Use Python 3.12 as the base
FROM python:3.12-slim

# 2. Install Java (OpenJDK 17) and system tools
RUN apt-get update && \
    apt-get install -y default-jre default-jdk && \
    apt-get clean

# 3. Set work directory
WORKDIR /app

# 4. Install Python Dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copy your project code
COPY . .

# 6. Create the directory for compiled Java classes
RUN mkdir -p java_core/bin

# 7. Make the entrypoint script executable
RUN chmod +x entrypoint.sh

# 8. Collect Static Files (CSS)
RUN python manage.py collectstatic --noinput

# 9. Run the app!
CMD ["./entrypoint.sh"]