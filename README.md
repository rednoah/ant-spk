ant-spk
============

Ant Task for creating SPK packages for Synology NAS.

Introduction
------------
I've found the Synology SDK tools for creating and signing SPK packages overly difficult to use and and terrible to automate. So here's an Apache Ant task to handle build automation of SPK packages in an easy to maintain and completely platform-independent manner.

__Ant SPK Task__
* Much more easy to use than whats in the official Synology SDK docs & tools
* Automatically create and sign your SPK packages in your automated Ant build
* Completely platform-agnostic so it works on Windows, Linux and Mac (and everything else that runs Java 8)
* Supports passphrase protected GPG keychains

Have a quick look at the [Synology DSM  3rd Party Apps Developer Guide](http://usdl.synology.com/download/ds/userguide/Synology_DiskStation_Manager_3rd_Party_Apps_Developer_Guide.pdf) and read the **Package Structure** section to learn more on how SPK packages work.

Example
-------
```xml
<project name="Ant SPK Task" basedir="." default="spk" xmlns:syno="antlib:net.filebot.ant.spk">

	<target name="spk">

		<syno:spk destdir="dist" name="helloworld" version="0.1" arch="noarch">
			<info name="displayname" value="Hello World" />
			<info name="description" value="Hello World package built with ant-spk" />

			<info name="maintainer" value="ant-spk" />
			<info name="maintainer_url" value="https://github.com/rednoah/ant-spk" />

			<info name="dsmappname" value="org.example.HelloWorld" />
			<info name="dsmuidir" value="dsm" />

			<icon size="72" file="app/dsm/images/icon_72.png" />
			<icon size="256" file="app/dsm/images/icon_256.png" />

			<wizard dir="spk/wizard" />
			<scripts dir="spk/scripts" filemode="755" />

			<package dir="app" includes="**/*.sh" filemode="755" />
			<package dir="app" excludes="**/*.sh" />

			<codesign keyid="D545C93D" pubring="gpg/pubring.gpg" secring="gpg/secring.gpg" password="" />
		</syno:spk>

	</target>

</project>
```

Downloads
---------
The latest binaries are in the [release section](https://github.com/rednoah/ant-spk-task/releases).

Dependencies
------------
**ant-spk** uses **Apache Ivy** for dependency management. Call `ant example` to fetch all dependencies and build the example project.

If you just want to download the jars you can find them [here](https://github.com/filebot/filebot-node/tree/master/lib).

Real World Examples
-------------
**ant-spk** is used to automatically build **.spk** packages for [FileBot](http://www.filebot.net/) so check out the `filebot` [build.xml](http://sourceforge.net/p/filebot/code/HEAD/tree/trunk/build.xml) or `filebot-node` [build.xml](https://github.com/filebot/filebot-node/blob/master/build.xml) for a more comprehensive examples. ;)
