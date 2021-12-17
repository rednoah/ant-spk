GRADLE := docker run --rm -v "$(PWD)":/ant-spk -w /ant-spk gradle:6-jdk11 gradle

jar:
	$(GRADLE) clean build

example:
	cd "$(PWD)/examples/helloworld" && ant -lib "$(PWD)/build/libs" -lib "lib"

deploy:
	$(GRADLE) clean uploadArchives
	# open "https://oss.sonatype.org/#stagingRepositories"

eclipse:
	$(GRADLE) cleanEclipse eclipse

clean:
	git reset --hard
	git pull
	git --no-pager log -1
