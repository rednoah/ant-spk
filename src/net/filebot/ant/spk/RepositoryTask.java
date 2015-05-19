package net.filebot.ant.spk;

import static java.util.Collections.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import static net.filebot.ant.spk.PackageTask.*;
import net.filebot.ant.spk.PackageTask.Info;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;

public class RepositoryTask extends Task {

	public static class SPK {

		File file;
		URL link;

		public void setFile(File file) {
			this.file = file;
		}

		public void setLink(URL link) {
			this.link = link;
		}

		Map<String, Object> infoList = new LinkedHashMap<String, Object>();

		public void addConfiguredInfo(Info info) {
			infoList.put(info.name, info.value);
		}

		List<String> thumbnail = new ArrayList<String>();
		List<String> snapshot = new ArrayList<String>();

		public void addConfiguredThumbnail(URLResource link) {
			thumbnail.add(link.getURL().toString());
		}

		public void addConfiguredSnapshot(URLResource link) {
			snapshot.add(link.getURL().toString());
		}

	}

	File index;
	Union keyrings = new Union();
	List<SPK> spks = new ArrayList<SPK>();

	public void setFile(File file) {
		this.index = file;
	}

	public void addConfiguredKeyRing(FileSet key) {
		keyrings.add(key);
	}

	public void addConfiguredKeyRing(ResourceCollection key) {
		keyrings.add(key);
	}

	public void addConfiguredSPK(SPK spk) {
		if (spk.file == null || (spk.link == null && !spk.file.exists()))
			throw new BuildException("Required attributes: file or link");

		spks.add(spk);
	}

	@Override
	public void execute() throws BuildException {
		if (index == null) {
			throw new BuildException("Required attributes: file");
		}

		try {
			JsonObjectBuilder jsonRoot = Json.createObjectBuilder();

			JsonArrayBuilder jsonKeyrings = Json.createArrayBuilder();
			getKeyRings().forEach(jsonKeyrings::add);
			jsonRoot.add("keyrings", jsonKeyrings);

			JsonArrayBuilder jsonPackages = Json.createArrayBuilder();
			getPackages().forEach((p) -> {
				JsonObjectBuilder jsonPackage = Json.createObjectBuilder();
				p.forEach((k, v) -> {
					if (v instanceof Boolean) {
						jsonPackage.add(k, (Boolean) v); // Boolean
					} else if (v instanceof Number) {
						jsonPackage.add(k, ((Number) v).longValue()); // Integer
					} else if (v instanceof String[]) {
						JsonArrayBuilder array = Json.createArrayBuilder();
						for (String s : (String[]) v) {
							array.add(s);
						}
						jsonPackage.add(k, array);
					} else {
						jsonPackage.add(k, v.toString()); // String
					}
				});
				jsonPackages.add(jsonPackage);
			});
			jsonRoot.add("packages", jsonPackages);

			log("Write Package Source: " + index);
			try (JsonWriter writer = Json.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true)).createWriter(new FileOutputStream(index))) {
				writer.writeObject(jsonRoot.build());
			}
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	public List<String> getKeyRings() throws IOException {
		List<String> keys = new ArrayList<String>();
		for (Resource resource : keyrings) {
			log("Include keyring: " + resource.getName());
			String key = FileUtils.readFully(new InputStreamReader(resource.getInputStream(), StandardCharsets.US_ASCII));
			if (key != null) {
				keys.add(key);
			}
		}

		return keys;
	}

	public List<Map<String, Object>> getPackages() throws IOException {
		List<Map<String, Object>> packages = new ArrayList<Map<String, Object>>();

		for (SPK spk : spks) {
			log("Include SPK: " + spk.file.getName());

			boolean useLink = spk.link != null && new URLResource(spk.link).isExists();
			if (!useLink && !spk.file.exists()) {
				throw new BuildException("Required resources: file or link");
			}

			// make sure file is cached locally
			if (useLink) {
				Get get = new Get();
				get.bindToOwner(this);
				get.setUseTimestamp(true);
				get.setSrc(spk.link);
				get.setDest(spk.file);
				get.execute();
			} else {
				log("Using " + spk.file);
			}

			// import SPK INFO
			Map<String, Object> info = new LinkedHashMap<String, Object>();

			TarFileSet tar = new TarFileSet();
			tar.setProject(getProject());
			tar.setSrc(spk.file);
			tar.setIncludes(INFO);
			for (Resource resource : tar) {
				if (INFO.equals(resource.getName())) {
					String text = FileUtils.readFully(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
					for (String line : text.split("\\R")) {
						String[] s = line.split("=", 2);
						if (s.length == 2) {
							if (s[1].startsWith("\"") && s[1].endsWith("\"")) {
								s[1] = s[1].substring(1, s[1].length() - 1);
							}
							importSpkInfo(info, s[0], s[1]);
						}
					}
				}
			}
			log(String.format("Imported %d fields from SPK: %s", info.size(), info.keySet()));

			// add thumbnails and snapshots
			if (spk.thumbnail.size() > 0) {
				info.put("thumbnail", spk.thumbnail.toArray(new String[0]));
			}
			if (spk.snapshot.size() > 0) {
				info.put("snapshot", spk.snapshot.toArray(new String[0]));
			}

			// add user-defined fields
			info.putAll(spk.infoList);

			// automatically generate file size and checksum fields
			if (spk.link != null) {
				info.put("link", spk.link);
			}
			info.put("md5", md5(spk.file));
			info.put("size", spk.file.length());

			packages.add(info);
		}

		return packages;
	}

	public void importSpkInfo(Map<String, Object> info, String key, String value) {
		switch (key) {
		case "package":
		case "version":
		case "maintainer":
		case "maintainer_url":
		case "distributor":
		case "distributor_url":
			info.put(key, value);
			break;
		case "displayname":
			info.put("dname", value);
			break;
		case "description":
			info.put("desc", value);
			break;
		case "install_dep_packages":
			info.put("deppkgs", value);
			break;
		case "install_dep_services":
			info.put("depsers", value);
			break;
		case "startable":
			info.put("start", Project.toBoolean(value));
			info.put("qstart", Project.toBoolean(value));
			break;
		case "silent_install":
			info.put("qinst", Project.toBoolean(value));
			break;
		case "silent_upgrade":
			info.put("qupgrade", Project.toBoolean(value));
			break;
		case "thirdparty":
			info.put(key, Project.toBoolean(value));
			break;
		}
	}

}
