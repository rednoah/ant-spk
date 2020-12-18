package net.filebot.ant.spk;

import java.io.File;

public class Icon {

	Integer size;
	File file;

	public void setSize(Integer size) {
		this.size = size;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getPackageName() {
		if (size == null || size.equals(64) || size.equals(72)) {
			return "PACKAGE_ICON.PNG";
		} else {
			return "PACKAGE_ICON_" + size + ".PNG";
		}
	}

}
