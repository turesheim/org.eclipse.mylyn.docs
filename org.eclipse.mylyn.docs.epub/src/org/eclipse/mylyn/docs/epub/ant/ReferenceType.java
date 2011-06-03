package org.eclipse.mylyn.docs.epub.ant;

/**
 * @ant.type name="reference" category="epub"
 */
public class ReferenceType {

	String href;
	String title;
	String type;

	/**
	 * @ant.required
	 */
	public void setHref(String href) {
		this.href = href;
	}

	/**
	 * @ant.required
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @ant.required
	 */
	public void setType(String type) {
		this.type = type;
	}

}
