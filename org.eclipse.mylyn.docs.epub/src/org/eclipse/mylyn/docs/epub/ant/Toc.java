package org.eclipse.mylyn.docs.epub.ant;

import java.io.File;

/**
 * Represents a table of contents element in the {@link HtmlToEpubTask}. One
 * should specify either a path to a NCX file or whether or not to generate the
 * NCX.
 * 
 * @author Torkild U. Resheim
 * @ant.type name="toc" category="epub"
 */
public class Toc {

	File file;
	boolean generate;

	/**
	 * @ant.not-required
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @ant.not-required
	 */
	public void setGenerate(boolean generate) {
		this.generate = generate;
	}
}
