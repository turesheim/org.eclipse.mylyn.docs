/*******************************************************************************
 * Copyright (c) 2011 Torkild U. Resheim.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Torkild U. Resheim - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.docs.epub.ant;

import java.io.File;

/**
 * @ant.type name="item" category="epub"
 */
public class Item {
	File file;
	String page;
	String id;
	String type;
	String dest;

	/**
	 * @ant.not-required
	 */
	public void setNoToc(boolean toc) {
		this.noToc = toc;
	}

	boolean noToc = false;

	/**
	 * @ant.not-required
	 */
	public void setDest(String dest) {
		this.dest = dest;
	}

	/** Default is to add the item to the spine */
	boolean spine = true;

	/**
	 * A file on the local file system.
	 * 
	 * @param file
	 * @ant.required
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * A page on the wiki.
	 * 
	 * @param page
	 */
	public void setPage(String page) {
		this.page = page;
	}

	/**
	 * @ant.not-required
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @ant.not-required
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @ant.not-required
	 */
	public void setSpine(boolean spine) {
		this.spine = spine;
	}
}
