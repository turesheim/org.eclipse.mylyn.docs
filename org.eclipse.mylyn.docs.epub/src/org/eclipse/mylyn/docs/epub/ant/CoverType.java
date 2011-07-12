package org.eclipse.mylyn.docs.epub.ant;

/**
 * @ant.type name="cover" category="epub"
 */
public class CoverType {

	String image;

	String value;

	/**
	 * @ant.required
	 */
	public void addText(String value) {
		this.value = value;
	}

	/**
	 * @ant.required
	 */
	public void setImage(String image) {
		this.image = image;
	}

}
