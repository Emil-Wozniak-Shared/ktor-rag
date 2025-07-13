# Application EJ Dev!
This project shows how to use AI RAG in your work environment.

- this example includes XWiki (Confluence like app) to obtain your work documentations.

## Building & Running

Fill [application.yaml](src/main/resources/application.yaml)

The application requires several additional application which you can start using docker.

```bash
cd ./docker
docker compose up
```

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

### XWiki
Open `localhost:9090` and add you pages or you can upload samples
```bash
bash ./assets/xwiki-upload.sh
```

## How to use it

### Add a document:
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "AI Overview",
    "content": "Artificial Intelligence is transforming how we work and live..."
  }' | jq
```

### Load documents from xwiki:
```bash
curl -X POST http://localhost:8080/api/documents/xwiki
```

### Search documents:
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Security/Privacy Implications",
    "limit": 5
  }' | jq
```

### Get all documents:
```bash
curl http://localhost:8080/api/documents | jq
```

### Get RAG response:
```bash
curl -X POST http://localhost:8080/api/rag \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the best practices to create good architecture?",
    "limit": 10
  }' | jq
```