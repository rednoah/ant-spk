package net.filebot.ant.spk;

import static java.nio.charset.StandardCharsets.*;
import static net.filebot.ant.spk.Info.*;
import static net.filebot.ant.spk.util.Digest.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Tar.TarFileSet;
import org.apache.tools.ant.taskdefs.Tar.TarLongFileMode;
import org.apache.tools.ant.types.FileSet;

public class PackageTask extends Task {

	public static final String INFO = "INFO";
	public static final String SYNO_SIGNATURE = "syno_signature.asc";

	File destDir;

	Map<String, String> infoList = new LinkedHashMap<String, String>();

	List<TarFileSet> packageFiles = new ArrayList<TarFileSet>();
	List<TarFileSet> spkFiles = new ArrayList<TarFileSet>();

	Compression compression = Compression.gzip; // use GZIP by default, XZ requires DSM 6 or higher

	CodeSignTask codesign;

	public void setDestdir(File value) {
		destDir = value;
	}

	public void setCompression(Compression value) {
		compression = value;
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
		files.setFullpath(icon.getPackageName());
		files.setFile(icon.file);
		spkFiles.add(files);
	}

	public void setLicense(File file) {
		TarFileSet files = new TarFileSet();
		files.setFullpath("LICENSE");
		files.setFile(file);
		spkFiles.add(files);
	}

	public void addConfiguredCodeSign(CodeSignTask codesign) {
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

		// spk must be an uncompressed tar
		tar(spkFile, Compression.none, spkFiles);

		// make sure staging folder is clean for next time
		clean(spkStaging);
	}

	private void prepareSignature(File tempDirectory) {
		if (codesign != null) {
			// select files that need to be signed
			spkFiles.forEach((fs) -> {
				fs.setProject(getProject());
				codesign.addConfiguredCat(fs);
			});

			// create signature file
			File signatureFile = new File(tempDirectory, SYNO_SIGNATURE);
			codesign.setToken(signatureFile);

			codesign.bindToOwner(this);
			codesign.execute();

			// add signature file to output package
			TarFileSet syno_signature = new TarFileSet();
			syno_signature.setFullpath(signatureFile.getName());
			syno_signature.setFile(signatureFile);
			spkFiles.add(syno_signature);
		}
	}

	private void preparePackage(File tempDirectory) {
		File packageFile = new File(tempDirectory, "package.tgz");
		tar(packageFile, compression, packageFiles);

		infoList.put("checksum", md5(packageFile));

		TarFileSet package_tgz = new TarFileSet();
		package_tgz.setFullpath(packageFile.getName());
		package_tgz.setFile(packageFile);
		spkFiles.add(package_tgz);
	}

	private void prepareInfo(File tempDirectory) {
		StringBuilder infoText = new StringBuilder();
		infoList.forEach((k, v) -> {
			infoText.append(k).append('=').append('"').append(v).append('"').append('\n');
		});

		File infoFile = new File(tempDirectory, INFO);
		log("Generating INFO: " + infoFile);
		try {
			Files.write(infoFile.toPath(), infoText.toString().getBytes(UTF_8));
		} catch (Exception e) {
			throw new BuildException("Failed to write INFO", e);
		}

		TarFileSet info = new TarFileSet();
		info.setFullpath(infoFile.getName());
		info.setFile(infoFile);
		spkFiles.add(info);
	}

	private void tar(File destFile, Compression compression, List<TarFileSet> files) {
		Tar tar = new Tar();
		tar.setProject(getProject());
		tar.setLocation(getLocation());
		tar.setTaskName(getTaskName());
		tar.setEncoding("utf-8");

		TarLongFileMode longFileMode = new TarLongFileMode();
		longFileMode.setValue("posix");
		tar.setLongfile(longFileMode);

		tar.setCompression(compression.getTarCompressionMethod());

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
