# Thumbnail API
This project retrieves images from our object storage provider(s). No API key is required. 

## Requirements
The application needs Java17 and Maven v3.8.x or above

## Functionality

  * We use 3 storages : IBM Cloud S3 storage, Amazon S3 (deprecated) and IIIF server (deprecated)
  * When a request for a thumbnail comes in, the API’s ThumbnailController will always check IBM Cloud 
storage first and if a thumbnail is not found there it will go on to check if it’s in the old Amazon S3 storage.
If it can be found there either, it will check the IIIF server (for v2 IIIF image urls).
  * The upload functionality uses an (unmodified) version of the [Scrimage](https://github.com/sksamuel/scrimage)
library for scaling images and converting them to webp. This software has an [Apache 2.0 license](https://github.com/sksamuel/scrimage/blob/master/LICENSE)
    

## Build
``mvn clean install`` (add ``-DskipTests``) to skip the unit tests during build

## Deployment
1. Generate a Docker image using the project's [Dockerfile](Dockerfile)

2. Configure the application by generating a `thumbnail.user.properties` file and placing this in the 
[k8s](k8s) folder. After deployment this file will override the settings specified in the `thumbnail.properties` file
located in the [src/main/resources](src/main/resources) folder. The .gitignore file makes sure the .user.properties file
is never committed.

3. Configure the deployment by setting the proper environment variables specified in the configuration template files
in the [k8s](k8s) folder

4. Deploy to Kubernetes infrastructure

