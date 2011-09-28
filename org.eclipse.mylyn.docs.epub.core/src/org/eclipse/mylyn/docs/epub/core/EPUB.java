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
package org.eclipse.mylyn.docs.epub.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.ocf.Container;
import org.eclipse.mylyn.docs.epub.ocf.OCFFactory;
import org.eclipse.mylyn.docs.epub.ocf.OCFPackage;
import org.eclipse.mylyn.docs.epub.ocf.RootFile;
import org.eclipse.mylyn.docs.epub.ocf.RootFiles;
import org.eclipse.mylyn.docs.epub.ocf.util.OCFResourceImpl;
import org.eclipse.mylyn.internal.docs.epub.core.EPUBFileUtil;

/**
 * Represents one EPUB file. One or more publications can be added and will be a
 * part of the distribution when packed. See the <a
 * href="http://idpf.org/epub/20/spec/OPS_2.0.1_draft.htm#Section1.2">OPS
 * specification</a> for definitions of words and terms.
 * 
 * @author Torkild U. Resheim
 * @see http://idpf.org/epub/20/spec/OPS_2.0.1_draft.html
 * @see http://idpf.org/epub/20/spec/OPF_2.0.1_draft.htm
 */
public class EPUB {

	/** OEBPS (OPS+OPF) mimetype */
	private static final String MIMETYPE_OEBPS = "application/oebps-package+xml";

	/** Suffix for OCF files */
	private static final String OCF_FILE_SUFFIX = "xml";

	/** The encoding to use for the OCF */
	private static final String OCF_FILE_ENCODING = "UTF-8";

	private Container ocfContainer;

	/**
	 * Creates a new <b>empty</b> instance of an EPUB. Use
	 * {@link #add(OPSPublication)} and {@link #pack(File)} to add publications
	 * and ready the EPUB for distribution.
	 */
	public EPUB() {
		ocfContainer = OCFFactory.eINSTANCE.createContainer();
		RootFiles rootFiles = OCFFactory.eINSTANCE.createRootFiles();
		ocfContainer.setRootfiles(rootFiles);
		ocfContainer.setVersion("2.0");
		registerOCFResourceFactory();
	}

	/**
	 * Returns the container instance of the EPUB.
	 * 
	 * @return the container instance
	 */
	public Container getContainer() {
		return ocfContainer;
	}

	/**
	 * Adds a new OPS publication to the EPUB.
	 * 
	 * @param oebps
	 *            the publication to add.
	 */
	public void add(OPSPublication oebps) {
		RootFiles rootFiles = ocfContainer.getRootfiles();
		int count = rootFiles.getRootfiles().size();
		String rootFileName = count > 0 ? "OEBPS_" + count : "OEBPS";
		rootFileName += "/content.opf";
		RootFile rootFile = OCFFactory.eINSTANCE.createRootFile();
		rootFile.setFullPath(rootFileName);
		rootFile.setMediaType(MIMETYPE_OEBPS);
		rootFile.setPublication(oebps);
		rootFiles.getRootfiles().add(rootFile);
	}

	/**
	 * Adds a new publication to the EPUB
	 * 
	 * @param file
	 *            the publication to add
	 * @param type
	 *            the MIME type of the publication
	 */
	public void add(File file, String type) {
		String name = type.substring(type.lastIndexOf('/') + 1, type.length()).toUpperCase();
		RootFiles rootFiles = ocfContainer.getRootfiles();
		int count = rootFiles.getRootfiles().size();
		String rootFileName = count > 0 ? name + "_" + count : name;
		rootFileName += File.separator + file.getName();
		RootFile rootFile = OCFFactory.eINSTANCE.createRootFile();
		rootFile.setFullPath(rootFileName);
		rootFile.setMediaType(type);
		rootFile.setPublication(file);
		rootFiles.getRootfiles().add(rootFile);
	}

	/**
	 * Returns a list of all <i>OPS publications</i> contained within the EPUB.
	 * 
	 * @return a list of all OPS publications
	 */
	public List<OPSPublication> getOPSPublications() {
		ArrayList<OPSPublication> publications = new ArrayList<OPSPublication>();
		EList<RootFile> rootFiles = ocfContainer.getRootfiles().getRootfiles();
		for (RootFile rootFile : rootFiles) {
			if (rootFile.getMediaType().equals(MIMETYPE_OEBPS)) {
				publications.add((OPSPublication) rootFile.getPublication());
			}
		}
		return publications;
	}

	/**
	 * Assembles the EPUB file using a temporary working folder. The folder will
	 * be deleted as soon as the assembly has completed.
	 * 
	 * @param epubFile
	 *            the target EPUB file
	 * 
	 * @throws Exception
	 */
	public File pack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			pack(epubFile, workingFolder);
		}
		deleteFolder(workingFolder);
		return workingFolder;
	}

	/**
	 * Assembles the EPUB file using the specified working folder.
	 * 
	 * @param epubFile
	 *            the target EPUB file
	 * @param workingFolder
	 *            the working folder
	 * @throws Exception
	 */
	public void pack(File epubFile, File workingFolder) throws Exception {
		if (ocfContainer.getRootfiles().getRootfiles().isEmpty()) {
			throw new IllegalArgumentException("EPUB does not contain any publications");
		}
		workingFolder.mkdirs();
		if (workingFolder.isDirectory() || workingFolder.mkdirs()) {
			writeOCF(workingFolder);
			EList<RootFile> publications = ocfContainer.getRootfiles().getRootfiles();
			for (RootFile rootFile : publications) {
				Object publication = rootFile.getPublication();
				File root = new File(workingFolder.getAbsolutePath() + File.separator + rootFile.getFullPath());
				if (publication instanceof OPSPublication) {
					((OPSPublication) publication).pack(root);
				} else {
					if (rootFile.getPublication() instanceof File) {
						EPUBFileUtil.copy((File) rootFile.getPublication(), root);
					} else {
						throw new IllegalArgumentException("Unknown publication type");
					}
				}
			}
			EPUBFileUtil.zip(epubFile, workingFolder);
		} else {
			throw new IOException("Could not create working folder in " + workingFolder.getAbsolutePath());
		}
	}

	/**
	 * Reads the <i>Open Container Format</i> formatted list of the contents of
	 * this EPUB.
	 * 
	 * @param workingFolder
	 *            the folder where the EPUB was unpacked
	 * @throws IOException
	 * @see {@link #unpack(File)}
	 * @see {@link #unpack(File, File)}
	 */
	private void readOCF(File workingFolder) throws IOException {
		// These file names are listed in the OCF specification and must not be
		// changed.
		File metaFolder = new File(workingFolder.getAbsolutePath() + File.separator + "META-INF");
		File containerFile = new File(metaFolder.getAbsolutePath() + File.separator + "container.xml");
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(containerFile.getAbsolutePath());
		Resource resource = resourceSet.createResource(fileURI);
		resource.load(null);
		ocfContainer = (Container) resource.getContents().get(0);
	}

	/**
	 * Registers a new resource factory for OCF data structures. This is
	 * normally done through Eclipse extension points but we also need to be
	 * able to create this factory without the Eclipse runtime.
	 */
	private void registerOCFResourceFactory() {
		// Register package so that it is available even without the Eclipse
		// runtime
		@SuppressWarnings("unused")
		OCFPackage packageInstance = OCFPackage.eINSTANCE;

		// Register the file suffix
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(OCF_FILE_SUFFIX,
				new XMLResourceFactoryImpl() {

					@Override
					public Resource createResource(URI uri) {
						OCFResourceImpl xmiResource = new OCFResourceImpl(uri);
						Map<Object, Object> loadOptions = xmiResource.getDefaultLoadOptions();
						Map<Object, Object> saveOptions = xmiResource.getDefaultSaveOptions();
						// We use extended metadata
						saveOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
						loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
						// Required in order to correctly read in attributes
						loadOptions.put(XMLResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
						// Treat "href" attributes as features
						loadOptions.put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);
						// UTF-8 encoding is required per specification
						saveOptions.put(XMLResource.OPTION_ENCODING, OCF_FILE_ENCODING);
						return xmiResource;
					}

				});
	}

	/**
	 * Unpacks the EPUB file to a temporary location and populates the data
	 * model with the content.
	 * 
	 * @param epubFile
	 *            the EPUB file to unpack
	 * @return the location when the EPUB is unpacked
	 * @throws Exception
	 */
	public File unpack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		workingFolder.deleteOnExit();
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			unpack(epubFile, workingFolder);
		}
		return workingFolder;
	}

	/**
	 * Unpacks the given EPUB file into the specified destination and populates
	 * the data model with the content.
	 * 
	 * @param epubFile
	 *            the EPUB file to unpack
	 * @param destination
	 *            the destination folder
	 * @throws Exception
	 */
	public void unpack(File epubFile, File destination) throws Exception {
		EPUBFileUtil.unzip(epubFile, destination);
		readOCF(destination);
		EList<RootFile> rootFiles = ocfContainer.getRootfiles().getRootfiles();
		for (RootFile rootFile : rootFiles) {
			if (rootFile.getMediaType().equals(MIMETYPE_OEBPS)) {
				// XXX: Handle this better when adding support for EPUB 3
				OPSPublication ops = OPSPublication.getVersion2Instance();
				File root = new File(destination.getAbsolutePath() + File.separator + rootFile.getFullPath());
				ops.unpack(root);
				rootFile.setPublication(ops);
			}
		}
	}

	/**
	 * Creates a new folder named META-INF and writes the required
	 * <b>container.xml</b> in that folder.
	 * 
	 * @param workingFolder
	 *            the root folder
	 */
	private void writeOCF(File workingFolder) throws IOException {
		File metaFolder = new File(workingFolder.getAbsolutePath() + File.separator + "META-INF");
		if (metaFolder.mkdir()) {
			File containerFile = new File(metaFolder.getAbsolutePath() + File.separator + "container.xml");
			ResourceSet resourceSet = new ResourceSetImpl();
			// Register the packages to make it available during loading.
			URI fileURI = URI.createFileURI(containerFile.getAbsolutePath());
			Resource resource = resourceSet.createResource(fileURI);
			resource.getContents().add(ocfContainer);
			resource.save(null);
		}
	}

	/**
	 * Delete the folder recursively.
	 * 
	 * @param folder
	 *            the folder to delete
	 * @return
	 */
	private void deleteFolder(File folder) {
		if (folder.isDirectory()) {
			String[] children = folder.list();
			for (int i = 0; i < children.length; i++) {
				deleteFolder(new File(folder, children[i]));
			}
		}
		folder.delete();
	}
}
