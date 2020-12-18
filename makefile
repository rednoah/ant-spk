test:
	gradle clean build example

deploy:
	gradle clean uploadArchives
	# open "https://oss.sonatype.org/#stagingRepositories"

eclipse:
	gradle cleanEclipse eclipse

clean:
	git reset --hard
	git pull
	git --no-pager log -1
