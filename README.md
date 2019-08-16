# Thumbnail API
### Europeana Thumbnail API 

This project retrieves images from our object storage provider. No API key is required. 

### Implementation details ###

This Thumbnail API implements the functionality described in Thumbnail API Specification.

## Build
``mvn clean install`` (add ``-DskipTests``) to skip the unit tests during build

### REQUIREMENTS
The application needs Java8 jre, Spring-Boot and Maven v3.3.x or above

### FUNCTIONALITY
*This project retrieves images from our object storage provider. No API key is required. 

*We use 2 storages : Amazon S3 and IBM Cloud storage. 

*When a request for a thumbnail comes in the API’s ThumbnailController will always check IBM Cloud 
storage first and if a thumbnail is not found there it will go on to check if it’s in the old Amazon S3 storage.


### PROPERTIES
Application name, port number, storage connection settings etc. are 
all managed in the thumbnail.properties file.
Note that any sensitive data (e.g. passwords) are omitted from this file; they can be overridden in a local 
thumbnail.user.properties file in src/main/resources.

### KNOWN ISSUES

