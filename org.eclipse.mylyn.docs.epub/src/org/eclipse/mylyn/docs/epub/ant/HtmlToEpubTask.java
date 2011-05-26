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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.eclipse.mylyn.docs.epub.opf.Type;
import org.xml.sax.SAXException;

public class HtmlToEpubTask extends Task {

	EPUB epub = new EPUB();
	Toc toc = null;
	
	private File workingFolder;

	public HtmlToEpubTask() {
	}

	public void addConfiguredContributor(Contributor contributor) {
		if (contributor.role == null) {
			epub.addContributor(contributor.name, null, contributor.fileAs, contributor.lang);
		} else {
			epub.addContributor(contributor.name, Role.get(contributor.role),
					contributor.fileAs, contributor.lang);
		}
	}

	public void addConfiguredCreator(Creator creator) {
		if (creator.role == null) {
			epub.addCreator(creator.name, null, creator.fileAs, creator.lang);
		} else {
			epub.addCreator(creator.name, Role.get(creator.role),
					creator.fileAs, creator.lang);
		}
	}
	public void addConfiguredDate(Date date) {
		epub.addDate(date.date, date.event);
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
	public void addConfiguredLanguage(Language language) {
		epub.addLanguage(language.code);
	}

	public void addConfiguredPublisher(Publisher publisher) {
		epub.addPublisher(publisher.text, publisher.lang);
	}

	public void addConfiguredReference(Reference reference) {
		epub.addReference(reference.href, reference.title, Type.get(reference.type));
	}

	public void addConfiguredSubject(Subject subject) {
		epub.addSubject(subject.text, subject.lang);
	}
	
	public void addConfiguredTitle(Title title) {
		epub.addTitle(title.text, title.lang);
	}

	public void addConfiguredToc(Toc toc) {
		if (this.toc!=null){
			throw new BuildException("Only one table of contents (toc) declaration is allowed.");
		}
		this.toc =toc;
	}

	public void addConfiguredType(org.eclipse.mylyn.docs.epub.ant.Type type){
		epub.addType(type.text);
	}

	@Override
	public void execute() throws BuildException {
		validate();
		if (toc.isGenerate()){
			epub.setGenerateToc(true);
		} else if (toc.getFile()!=null){
			epub.setTocFile(toc.getFile());
		}
		try {
			if (workingFolder == null) {
				epub.assemble();
			} else {
				epub.assemble(workingFolder);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
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

	private void validate(){
		// Validate the table of contents
		if (toc==null){
			throw new BuildException("A table of contents (toc) must be declared.");
		}
		if (toc.getFile()==null && !toc.isGenerate()){
			throw new BuildException("Missing 'file' or 'generate' attribute");
		}
	}

}
