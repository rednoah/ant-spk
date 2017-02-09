#/bin/sh
gradle clean uploadArchives && open "https://oss.sonatype.org/#stagingRepositories"
