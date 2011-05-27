package org.eclipse.mylyn.docs.epub.ant;

/**
 * @ant.type name="language" category="epub"
 */
public class Language {
	String code;

	/**
	 * @ant.required
	 */
	public void setCode(String code) {
		this.code = code;
	}
}
