docker images | grep ejdev

# Load it into minikube
minikube image load ejdev:latest

# Verify it's loaded
minikube image ls | grep ejde