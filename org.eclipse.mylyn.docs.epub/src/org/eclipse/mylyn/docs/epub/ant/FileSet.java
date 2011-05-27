package org.eclipse.mylyn.docs.epub.ant;

/**
 * @ant.type name="fileset" category="epub"
 */
public class FileSet extends org.apache.tools.ant.types.FileSet {
	String dest;

	public void setDest(String dest) {
		this.dest = dest;
	}
}
