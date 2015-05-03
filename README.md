ant-spk
============

Ant Task for creating SPK packages for Synology NAS.

Introduction
------------
First have a quick look at the [Synology DSM  3rd Party Apps Developer Guide](http://usdl.synology.com/download/ds/userguide/Synology_DiskStation_Manager_3rd_Party_Apps_Developer_Guide.pdf) and read the **Package Structure** section.

Example
-------
```xml
<taskdef name="spk" classname="net.filebot.ant.spk.SpkTask" classpath="lib/ant-spk.jar" />

<spk destdir="dist" name="Hello World" version="0.1" arch="noarch">
	<info name="description" value="Hello World package made with ant-spk" />
	<info name="maintainer" value="rednoah" />
	<package dir="helloworld/app"  />
	<scripts dir="helloworld/spk/scripts" filemode="755" />
</spk>
```

Downloads
---------
The latest binaries are in the [release section](https://github.com/rednoah/ant-spk-task/releases).


Real World Examples
-------------
**ant-spk** is used to automatically build **.spk** packages for [FileBot](http://www.filebot.net/) so check out the `filebot` [build.xml](http://sourceforge.net/p/filebot/code/HEAD/tree/trunk/build.xml) or `filebot-node` [build.xml](https://github.com/filebot/filebot-node/blob/master/build.xml) for a more comprehensive examples. ;)
