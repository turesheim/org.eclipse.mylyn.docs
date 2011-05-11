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
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;

public class HtmlToEpubTask extends Task {

	EPUB epub = new EPUB();

	private File workingFolder;

	public HtmlToEpubTask() {
	}

	/**
	 * The FileSet sub-element is used to add EPUB artifacts that are not a part
	 * of the main text. This can be graphical items and styling (CSS).
	 * 
	 * @param set
	 */
	public void addConfiguredFileSet(FileSet fs) {
		DirectoryScanner ds = fs.getDirectoryScanner(getProject()); // 3
		String[] includedFiles = ds.getIncludedFiles();
		for (int i = 0; i < includedFiles.length; i++) {
			String filename = includedFiles[i].replace('\\', '/'); // 4
			filename = filename.substring(filename.lastIndexOf("/") + 1);
			File base = ds.getBasedir(); // 5
			File found = new File(base, includedFiles[i]);
			epub.addItem(found, null, null, false);
		}
	}

	public void addConfiguredLanguage(Language language) {
		epub.addLanguage(language.code);
	}

	/**
	 * Adds a new identifier.
	 * 
	 * @param identifier
	 */
	public void addConfiguredIdentifier(Identifier identifier) {
		epub.addIdentifier(identifier.id, Scheme.getByName(identifier.scheme),
				identifier.value);
	}

	public void addConfiguredItem(Item item) {
		epub.addItem(item.file, item.id, item.type, item.spine);
	}

	public void addConfiguredSubject(Subject subject) {
		epub.addSubject(subject.text, subject.lang);
	}

	public void addConfiguredTitle(Title title) {
		epub.addTitle(title.text, title.lang);
	}

	public void addConfiguredPublisher(Publisher publisher) {
		epub.addPublisher(publisher.text, publisher.lang);
	}

	public void addConfiguredCreator(Creator creator) {
		if (creator.role != null) {
			epub.addCreator(creator.name, null, creator.fileAs, creator.lang);
		} else {
			epub.addCreator(creator.name, Role.get(creator.role),
					creator.fileAs, creator.lang);
		}
	}

	public void addConfiguredItemReference(ItemReference reference) {
		epub.addReference(reference.href, reference.title, reference.type);
	}

	@Override
	public void execute() throws BuildException {
		try {
			if (workingFolder == null) {
				epub.assemble();
			} else {
				epub.assemble(workingFolder);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 
	 * @param file
	 *            path to the generated EPUB file.
	 */
	public void setFile(String file) {
		epub.setFile(file);
	}

	public void setIdentifierId(String identifierId) {
		epub.setIdentifierId(identifierId);
	}

	public void setWorkingFolder(File workingFolder) {
		this.workingFolder = workingFolder;
	}

}
