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
package org.eclipse.mylyn.docs.epub.tests;

import java.io.File;

import junit.framework.Assert;

import org.apache.tools.ant.BuildFileTest;

import com.adobe.epubcheck.api.EpubCheck;

/**
 * Tests for the <b>markup-to-epub</b> ANT task.
 * 
 * @author Torkild U. Resheim
 */
public class TestAntTask extends BuildFileTest {

	private static final String EPUB_FILE_PATH = "test/test.epub";

	public TestAntTask(String s) {
		super(s);
	}

	private File getFile() {
		return new File(getProjectDir().getAbsolutePath() + File.separator
				+ EPUB_FILE_PATH);
	}

	private void assertEpub() {
		EpubCheck checker = new EpubCheck(getFile());
		Assert.assertTrue(checker.validate());
	}

	@Override
	public void setUp() {
		configureProject("ant-test.xml");
		File epub = getFile();
		if (epub.exists()) {
			epub.delete();
		}
	}

	/**
	 * Creates a an empty EPUB file and tests whether it's structure can be
	 * validated.
	 */
	public void testEmpty() {
		executeTarget("test.empty");
		assertEpub();
	}

	/**
	 * Creates a an empty EPUB file and tests whether it's structure can be
	 * validated.
	 */
	public void testItem() {
		executeTarget("test.item");
		assertEpub();
	}
}
