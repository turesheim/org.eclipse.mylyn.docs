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

public class Item {
	File file;
	String page;
	String id;
	String type;
	/** Default is to add the item to the spine */
	boolean spine = true;
	boolean fetch = true;

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
	}

	/**
	 * A file on the local file system.
	 * 
	 * @param file
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

	public void setId(String id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setSpine(boolean spine) {
		this.spine = spine;
	}
}
