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
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.eclipse.mylyn.docs.epub.opf.Type;

/**
 * Assemble a new EPUB.
 * 
 * 
 * @author Torkild U. Resheim
 * @ant.task name="epub" category="epub"
 */
public class EpubTask extends Task {

	private final EPUB epub = EPUB.getVersion2Instance();

	private final ArrayList<FileSetType> filesets;

	private TocType toc = null;

	private File workingFolder;

	public EpubTask() {
		super();
		filesets = new ArrayList<FileSetType>();
	}

	public void addConfiguredContributor(ContributorType item) {
		if (item.role == null) {
			epub.addContributor(item.id, item.lang, item.name, null, item.fileAs);
		} else {
			epub.addContributor(item.id, item.lang, item.name, Role.get(item.role), item.fileAs);
		}
	}

	public void addConfiguredCover(CoverType item) {
		epub.setCover(new File(item.image), item.value);
	}

	public void addConfiguredCoverage(CoverageType coverage) {
		epub.addCoverage(coverage.id, coverage.lang, coverage.text);
	}

	public void addConfiguredCreator(CreatorType item) {
		if (item.role == null) {
			epub.addCreator(item.id, item.lang, item.name, null, item.fileAs);
		} else {
			epub.addCreator(item.id, item.lang, item.name, Role.get(item.role), item.fileAs);
		}
	}

	public void addConfiguredDate(DateType item) {
		epub.addDate(item.id, item.date, item.event);
	}

	/**
	 * The FileSet sub-element is used to add EPUB artifacts that are not a part
	 * of the main text. This can be graphical items and styling (CSS).
	 * 
	 * @param fs
	 *            the fileset to add
	 */
	public void addConfiguredFileSet(FileSetType fs) {
		filesets.add(fs);
	}

	public void addConfiguredFormat(FormatType format) {
		epub.addFormat(format.id, format.text);
	}

	/**
	 * @ant.required
	 */
	public void addConfiguredIdentifier(IdentifierType identifier) {
		epub.addIdentifier(identifier.id, Scheme.getByName(identifier.scheme), identifier.value);
	}

	/**
	 * @ant.required
	 */
	public void addConfiguredItem(ItemType item) {
		epub.addItem(item.id, item.lang, item.file, item.dest, item.type, item.spine, item.noToc);
	}

	/**
	 * @ant.required
	 */
	public void addConfiguredLanguage(LanguageType language) {
		epub.addLanguage(language.id, language.code);
	}

	public void addConfiguredMeta(MetaType item) {
		epub.addMeta(item.name, item.content);
	}

	public void addConfiguredPublisher(PublisherType publisher) {
		epub.addPublisher(publisher.id, publisher.lang, publisher.text);
	}

	public void addConfiguredReference(ReferenceType reference) {
		Type type = Type.get(reference.type);
		if (type == null) {
			throw new BuildException("Unknown reference type " + reference.type);
		}
		epub.addReference(reference.href, reference.title, type);
	}

	public void addConfiguredRelation(RelationType relation) {
		epub.addRelation(relation.id, relation.lang, relation.text);
	}

	public void addConfiguredRights(RightsType rights) {
		epub.addRights(rights.id, rights.lang, rights.text);
	}

	public void addConfiguredSource(SourceType source) {
		epub.addSource(source.id, source.lang, source.text);
	}

	public void addConfiguredSubject(SubjectType subject) {
		epub.addSubject(subject.id, subject.lang, subject.text);
	}

	/**
	 * @ant.required
	 */
	public void addConfiguredTitle(TitleType title) {
		epub.addTitle(title.id, title.lang, title.text);
	}

	public void addConfiguredToc(TocType toc) {
		if (this.toc != null) {
			throw new BuildException("Only one table of contents (toc) declaration is allowed.");
		}
		this.toc = toc;
	}

	public void addConfiguredType(org.eclipse.mylyn.docs.epub.ant.TypeType type) {
		epub.addType(type.id, type.text);
	}

	private void addFilesets() {
		for (FileSetType fs : filesets) {
			if (fs.getProject() == null) {
				log("Deleting fileset with no project specified;" + " assuming executing project", Project.MSG_VERBOSE);
				fs = (FileSetType) fs.clone();
				fs.setProject(getProject());
			}
			final File fsDir = fs.getDir();
			if (fsDir == null) {
				throw new BuildException("File or Resource without directory or file specified");
			} else if (!fsDir.isDirectory()) {
				throw new BuildException("Directory does not exist:" + fsDir);
			}
			DirectoryScanner ds = fs.getDirectoryScanner();
			String[] includedFiles = ds.getIncludedFiles();
			for (int i = 0; i < includedFiles.length; i++) {
				String filename = includedFiles[i].replace('\\', '/');
				filename = filename.substring(filename.lastIndexOf("/") + 1);
				File base = ds.getBasedir();
				File found = new File(base, includedFiles[i]);
				epub.addItem(null, fs.lang, found, fs.dest, null, false, false);
			}

		}

	}

	@Override
	public void execute() throws BuildException {
		validate();
		addFilesets();
		if (toc != null) {
			if (toc.generate) {
				epub.setGenerateToc(true);
			} else if (toc.file != null) {
				epub.setTocFile(toc.file);
			}
		}
		try {
			if (workingFolder == null) {
				epub.assemble();
			} else {
				epub.assemble(workingFolder);
			}

		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	/**
	 * @ant.not-required Automatically add referenced resources.
	 */
	public void setAutomaticAdd(boolean automatic) {
		epub.setIncludeReferencedResources(automatic);
	}

	/**
	 * 
	 * 
	 * @param file
	 *            path to the generated EPUB file.
	 */
	public void setFile(File file) {
		epub.setFile(file);
	}

	public void setIdentifierId(String identifierId) {
		epub.setIdentifierId(identifierId);
	}

	public void setWorkingFolder(File workingFolder) {
		this.workingFolder = workingFolder;
	}

	private void validate() {
	}

}
