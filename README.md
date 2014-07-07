ant-spk-task
============

Ant Task for creating SPK packages for Synology NAS

Introduction
------------
First have a quick look at the [Synology DSM  3rd Party Apps Developer Guide](http://usdl.synology.com/download/ds/userguide/Synology_DiskStation_Manager_3rd_Party_Apps_Developer_Guide.pdf) and read the `Package Structure` section.

Example
-------
```xml
<taskdef name="spk" classname="net.filebot.ant.spk.SpkTask" classpath="lib/ant-spk.jar" />

<spk destdir="dist" name="Hello World" version="0.1" arch="noarch">
	<info name="description" value="Hello World package made with ant-spk" />
	<info name="maintainer" value="rednoah" />
	<package dir="examples/helloworld/app"  />
	<scripts dir="examples/helloworld/spk/scripts" filemode="755" />
</spk>
```

Downloads
---------
The latest binaries are in the [release section](https://github.com/rednoah/ant-spk-task/releases).
