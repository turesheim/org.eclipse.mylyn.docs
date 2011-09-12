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
package org.eclipse.mylyn.docs.epub;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.internal.EPUBFileUtil;
import org.eclipse.mylyn.docs.epub.ocf.Container;
import org.eclipse.mylyn.docs.epub.ocf.OCFFactory;
import org.eclipse.mylyn.docs.epub.ocf.OCFPackage;
import org.eclipse.mylyn.docs.epub.ocf.RootFile;
import org.eclipse.mylyn.docs.epub.ocf.RootFiles;
import org.eclipse.mylyn.docs.epub.ocf.util.OCFResourceImpl;

public class EPUB {

	public static final String MIMETYPE_OEBPS = "application/oebps-package+xml";

	Container ocfContainer;

	public EPUB() {
		ocfContainer = OCFFactory.eINSTANCE.createContainer();
		RootFiles rootFiles = OCFFactory.eINSTANCE.createRootFiles();
		ocfContainer.setRootfiles(rootFiles);
		registerOCFResourceFactory();
	}

	public void pack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		workingFolder.deleteOnExit();
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			pack(epubFile, workingFolder);
		}
	}

	public void pack(File epubFile, File workingFolder) throws Exception {
		// Note that order is important here. Some methods may insert data into
		// the EPUB structure. Hence the OPF must be written last.
		if (workingFolder.isDirectory() || workingFolder.mkdirs()) {
			writeOCF(workingFolder);
			EList<RootFile> publications = ocfContainer.getRootfiles().getRootfiles();
			for (RootFile rootFile : publications) {
				Object publication = rootFile.getPublication();
				if (publication instanceof OPSPublication) {
					String folder = new File(rootFile.getFullPath()).getParent();
					File rootFolder = new File(workingFolder.getAbsolutePath() + File.separator + folder);
					((OPSPublication) publication).pack(rootFolder);
				}
			}
			EPUBFileUtil.zip(epubFile, workingFolder);
		} else {
			throw new IOException("Could not create working folder in " + workingFolder.getAbsolutePath());
		}
	}

	public void add(OPSPublication epub) {
		RootFiles rootFiles = ocfContainer.getRootfiles();
		int count = rootFiles.getRootfiles().size();
		String rootFileName = count > 0 ? "OEBPS_" + count : "OEBPS";
		rootFileName += "/content.opf";
		RootFile rootFile = OCFFactory.eINSTANCE.createRootFile();
		rootFile.setFullPath(rootFileName);
		rootFile.setMediaType(MIMETYPE_OEBPS);
		rootFile.setPublication(epub);
		rootFiles.getRootfiles().add(rootFile);
	}

	public File unpack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		workingFolder.deleteOnExit();
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			unpack(epubFile, workingFolder);
		}
		return workingFolder;
	}

	/**
	 * Returns the first OPS publication if any. XXX: Must be handled better
	 * when adding more root files.
	 * 
	 * @return
	 */
	public OPSPublication getOPSPublication() {
		RootFile rootFile = ocfContainer.getRootfiles().getRootfiles().get(0);
		return (OPSPublication) rootFile.getPublication();
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

	private static final String OCF_FILE_SUFFIX = "xml";

	/** The encoding to use in XML files */
	protected static final String XML_ENCODING = "UTF-8";

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
				System.out.println("URI" + uri);
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
				saveOptions.put(XMLResource.OPTION_ENCODING, XML_ENCODING);
				return xmiResource;
			}

		});
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

	private void readOCF(File workingFolder) throws IOException {
		File metaFolder = new File(workingFolder.getAbsolutePath() + File.separator + "META-INF");
		File containerFile = new File(metaFolder.getAbsolutePath() + File.separator + "container.xml");
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(containerFile.getAbsolutePath());
		Resource resource = resourceSet.createResource(fileURI);
		resource.load(null);
		ocfContainer = (Container) resource.getContents().get(0);
	}
}
