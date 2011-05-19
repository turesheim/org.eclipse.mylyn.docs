package org.eclipse.mylyn.docs.epub.ant;

import java.io.File;

/**
 * Represents a table of contents element in the {@link HtmlToEpubTask}. One
 * should specify either a path to a NCX file or whether or not to generate the
 * NCX.
 * 
 * @author Torkild U. Resheim
 * 
 */
public class Toc {

	private File file;

	private boolean generate;

	File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public boolean isGenerate() {
		return generate;
	}

	public void setGenerate(boolean generate) {
		this.generate = generate;
	}
}
