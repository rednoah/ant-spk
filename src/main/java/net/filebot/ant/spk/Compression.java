package net.filebot.ant.spk;

import org.apache.tools.ant.taskdefs.Tar.TarCompressionMethod;

/**
 * In DSM 5.2 or older, package.tgz must be tgz format. In DSM 6.0 or newer, package.tgz can be tgz or xz format, but the file name must be package.tgz.
 */
public enum Compression {

	none, gzip, xz;

	public TarCompressionMethod getTarCompressionMethod() {
		TarCompressionMethod compression = new TarCompressionMethod();
		compression.setValue(name());
		return compression;
	}

}
