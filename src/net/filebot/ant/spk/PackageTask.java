package net.filebot.ant.spk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.openpgp.ant.OpenPgpSignerTask;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.taskdefs.Concat;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Tar.TarCompressionMethod;
import org.apache.tools.ant.taskdefs.Tar.TarFileSet;
import org.apache.tools.ant.taskdefs.Tar.TarLongFileMode;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

public class PackageTask extends Task {

	public static class Info {

		String name;
		String value;

		public void setName(String name) {
			this.name = name;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class Icon {

		public static class Size extends EnumeratedAttribute {

			@Override
			public String[] getValues() {
				return new String[] { "72", "256" };
			}

			public String getFileName() {
				switch (value) {
				case "72":
					return "PACKAGE_ICON.PNG";
				default:
					return String.format("PACKAGE_ICON_%s.PNG", value);
				}
			}
		}

		Size size;
		File file;

		public void setSize(Size size) {
			this.size = size;
		}

		public void setFile(File file) {
			this.file = file;
		}
	}

	static final String NAME = "package";
	static final String VERSION = "version";
	static final String ARCH = "arch";

	File destDir;
	Map<String, String> infoList = new LinkedHashMap<String, String>();

	List<FileSet> packageFiles = new ArrayList<FileSet>();
	List<FileSet> spkFiles = new ArrayList<FileSet>();

	CodeSign codesign;

	public void setDestdir(File value) {
		destDir = value;
	}

	public void setName(String value) {
		infoList.put(NAME, value);
	}

	public void setVersion(String value) {
		infoList.put(VERSION, value);
	}

	public void setArch(String value) {
		infoList.put(ARCH, value);
	}

	public void addConfiguredInfo(Info info) {
		infoList.put(info.name, info.value);
	}

	public void addConfiguredPackage(TarFileSet files) {
		packageFiles.add(files);
	}

	public void addConfiguredScripts(TarFileSet files) {
		files.setPrefix("scripts");
		spkFiles.add(files);
	}

	public void addConfiguredWizard(TarFileSet files) {
		files.setPrefix("WIZARD_UIFILES");
		spkFiles.add(files);
	}

	public void addConfiguredConf(TarFileSet files) {
		files.setPrefix("conf");
		spkFiles.add(files);
	}

	public void addConfiguredIcon(Icon icon) {
		TarFileSet files = new TarFileSet();
		files.setFullpath(icon.size.getFileName());
		files.setFile(icon.file);
		spkFiles.add(files);
	}

	public void setLicense(File file) {
		TarFileSet files = new TarFileSet();
		files.setFullpath("LICENSE");
		files.setFile(file);
		spkFiles.add(files);
	}

	public void addConfiguredCodeSign(CodeSign codesign) {
		this.codesign = codesign;
	}

	@Override
	public void execute() throws BuildException {
		if (destDir == null || !infoList.containsKey(NAME) || !infoList.containsKey(VERSION) || !infoList.containsKey(ARCH))
			throw new BuildException("Required attributes: destdir, name, version, arch");

		if (packageFiles.isEmpty() || spkFiles.isEmpty())
			throw new BuildException("Required elements: package, scripts");

		String spkName = String.format("%s-%s-%s", infoList.get(NAME), infoList.get(VERSION), infoList.get(ARCH));

		File spkStaging = new File(destDir, spkName);
		File spkFile = new File(destDir, spkName + ".spk");

		// make sure staging folder exists
		spkStaging.mkdirs();

		// generate info and package files and add to spk fileset
		preparePackage(spkStaging);
		prepareInfo(spkStaging);
		prepareSignature(spkStaging);

		tar(spkFile, false, spkFiles);

		// make sure staging folder is clean for next time
		clean(spkStaging);
	}

	private void prepareSignature(File tempDirectory) {
		if (codesign != null) {
			codesign.setProject(getProject());
			codesign.setToken(new File(tempDirectory, CodeSign.SYNO_SIGNATURE));
			codesign.execute();
		}
	}

	private void preparePackage(File tempDirectory) {
		File packageFile = new File(tempDirectory, "package.tgz");
		tar(packageFile, true, packageFiles);

		String resultKey = "package.tgz.md5";
		Checksum checksum = new Checksum();
		checksum.setProject(getProject());
		checksum.setTaskName(getTaskName());
		checksum.setFile(packageFile);
		checksum.setAlgorithm("MD5");
		checksum.setProperty(resultKey);
		checksum.perform();
		infoList.put("checksum", getProject().getProperty(resultKey));

		TarFileSet package_tgz = new TarFileSet();
		package_tgz.setFullpath(packageFile.getName());
		package_tgz.setFile(packageFile);
		spkFiles.add(package_tgz);
	}

	private void prepareInfo(File tempDirectory) {
		StringBuilder infoText = new StringBuilder();
		for (Entry<String, String> it : infoList.entrySet()) {
			infoText.append(it.getKey()).append('=').append('"').append(it.getValue()).append('"').append('\n');
		}

		File infoFile = new File(tempDirectory, "INFO");
		log("Generating INFO: " + infoFile);
		try {
			Files.write(infoFile.toPath(), infoText.toString().getBytes("UTF-8"));
		} catch (IOException e) {
			throw new BuildException("Failed to write INFO", e);
		}

		TarFileSet info = new TarFileSet();
		info.setFullpath(infoFile.getName());
		info.setFile(infoFile);
		spkFiles.add(info);
	}

	private void tar(File destFile, boolean gzip, List<FileSet> files) {
		Tar tar = new Tar();
		tar.setProject(getProject());
		tar.setLocation(getLocation());
		tar.setTaskName(getTaskName());

		TarLongFileMode gnuLongFileMode = new TarLongFileMode();
		gnuLongFileMode.setValue("gnu");
		tar.setLongfile(gnuLongFileMode);

		if (gzip) {
			TarCompressionMethod gzipCompression = new TarCompressionMethod();
			gzipCompression.setValue("gzip");
			tar.setCompression(gzipCompression);
		}

		tar.setDestFile(destFile);
		for (FileSet fileset : files) {
			if (fileset != null) {
				// make sure the tarfileset element is initialized with all the project information it may need
				fileset.setProject(tar.getProject());
				fileset.setLocation(tar.getLocation());
				tar.add(fileset);
			}
		}

		tar.perform();
	}

	private void clean(File tempDirectory) {
		Delete cleanupTask = new Delete();
		cleanupTask.setProject(getProject());
		cleanupTask.setTaskName(getTaskName());
		cleanupTask.setLocation(getLocation());
		cleanupTask.setDir(tempDirectory);
		cleanupTask.perform();
	}

}
