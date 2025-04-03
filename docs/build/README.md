# Build

To build the project image the following can be run in the services directory

## Verify build

The following checks should be run before merging.
```
mvn clean checkstyle:check javadoc:javadoc verify
```


## AMD
```
mvn clean compile jib:dockerBuild@local -Djib.from.platforms=linux/amd64 
```

## ARM
```
mvn clean compile jib:dockerBuild@local -Djib.from.platforms=linux/arm64
```