arm:
	cd ../../ ; mvn clean install -Dcheckstyle.skip; cd oidf-services; mvn clean compile jib:dockerBuild@local -Dcheckstyle.skip -Djib.from.platforms=linux/arm64

amd:
	cd ../../ ; mvn clean install -Dcheckstyle.skip; cd oidf-services; mvn clean compile jib:dockerBuild@local -Dcheckstyle.skip -Djib.from.platforms=linux/amd64