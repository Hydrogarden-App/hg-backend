
docker build -t hg-backend:latest .

docker run -v hg-logs:/app/logs hg-backend:latest
