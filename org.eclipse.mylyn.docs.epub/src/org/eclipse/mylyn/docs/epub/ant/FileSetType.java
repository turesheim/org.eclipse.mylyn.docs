package org.eclipse.mylyn.docs.epub.ant;

import java.util.Locale;

/**
 * @ant.type name="epubfileset" category="epub"
 */
public class FileSetType extends org.apache.tools.ant.types.FileSet {

	String dest;

	Locale lang;

	public FileSetType() {

	}

	public void setLocale(Locale lang) {
		this.lang = lang;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}
}
