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
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.mylyn.docs.epub.dc.Creator;
import org.eclipse.mylyn.docs.epub.dc.DCFactory;
import org.eclipse.mylyn.docs.epub.dc.Description;
import org.eclipse.mylyn.docs.epub.dc.Publisher;
import org.eclipse.mylyn.docs.epub.dc.Subject;
import org.eclipse.mylyn.docs.epub.dc.Title;
import org.eclipse.mylyn.docs.epub.opf.Guide;
import org.eclipse.mylyn.docs.epub.opf.Manifest;
import org.eclipse.mylyn.docs.epub.opf.Metadata;
import org.eclipse.mylyn.docs.epub.opf.OPFFactory;
import org.eclipse.mylyn.docs.epub.opf.OPFPackage;
import org.eclipse.mylyn.docs.epub.opf.Package;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Spine;
import org.eclipse.mylyn.docs.epub.opf.util.OPFResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.opf.util.OPFResourceImpl;

/**
 * <q>EPUB</q> is a standard from the International Digital Publishing Forum. It
 * is an arrangement of several other standards (mainly: XHTML, CSS, XML, NCX,
 * DCMI). There are three parts, addressing: content, package metadata, and
 * archive (OPS, OPF, and OCF). It is powerful, straightforward, and
 * non-proprietary.
 * <p>
 * This particular type represents one EPUB-formatted publication. It maintains
 * a data structure representing the entire publication and API for building it.
 * </p>
 * 
 * @author Torkild U. Resheim
 * 
 */
public class EPUB {

	private final Metadata opfMetadata;
	private final Guide opfGuide;
	private final Manifest opfManifest;
	private final Package opfPackage;
	private final Spine opfSpine;

	private final File epubFile;

	public EPUB(File file) {
		epubFile = file;
		// Start with the root of the OPF structure
		opfPackage = OPFFactory.eINSTANCE.createPackage();
		// Add required features
		opfMetadata = OPFFactory.eINSTANCE.createMetadata();
		opfPackage.setMetadata(opfMetadata);
		opfGuide = OPFFactory.eINSTANCE.createGuide();
		opfPackage.setGuide(opfGuide);
		opfManifest = OPFFactory.eINSTANCE.createManifest();
		opfPackage.setManifest(opfManifest);
		opfSpine = OPFFactory.eINSTANCE.createSpine();
		opfPackage.setSpine(opfSpine);
		registerResourceFactory();
	}

	/**
	 * Creates the final EPUB file.
	 */
	public void assemble() throws IOException {
		File workingFolder = File.createTempFile("epub_", null);
		if (workingFolder.delete() && workingFolder.mkdir()) {
			writeMimetype(workingFolder);
			File oepbsFolder = new File(workingFolder.getAbsolutePath()
					+ File.separator + "OEBPS");
			if (oepbsFolder.mkdir()) {
				writeOPF(oepbsFolder);
				writeTOC(oepbsFolder);
				copyContent(oepbsFolder);
				compress(oepbsFolder);
			} else {
				throw new IOException("Could not create OEBPS folder");
			}
		} else {
			throw new IOException("Could not create working folder");
		}

	}

	private void compress(File oepbsFolder) {
		// TODO Auto-generated method stub

	}

	private void copyContent(File oepbsFolder) {
		// TODO Auto-generated method stub

	}

	private void writeTOC(File oepbsFolder) {
		// TODO Auto-generated method stub

	}

	/**
	 * Creates a new file containing the EPUB mime-type in the working folder.
	 * If such a file already exists, it will not be changed.
	 * 
	 * @param workingFolder
	 *            the folder to create the file in
	 */
	private void writeMimetype(File workingFolder) {
		File mimeFile = new File(workingFolder.getAbsolutePath()
				+ File.separator + "mimetype");
		if (!mimeFile.exists()) {
			try {
				FileWriter fw = new FileWriter(mimeFile);
				fw.append("application/epub+zip");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param opfFile
	 * @throws IOException
	 */
	private void writeOPF(File oebpsFolder) throws IOException {
		File opfFile = new File(oebpsFolder.getAbsolutePath() + File.separator
				+ "content.opf");
		ResourceSet resourceSet = new ResourceSetImpl();
		// Register the packages to make it available during loading.
		resourceSet.getPackageRegistry().put(OPFPackage.eNS_URI,
				OPFPackage.eINSTANCE);
		URI fileURI = URI.createFileURI(opfFile.getAbsolutePath());
		Resource resource = resourceSet.createResource(fileURI);
		resource.getContents().add(opfPackage);

		Map<String, Object> options = new HashMap<String, Object>();
		// OPF requires that we encode using UTF-8
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
		resource.save(options);
	}

	/**
	 * Registers a new resource factory for OPF data structures. This is
	 * normally done through Eclipse extension points but we also need to be
	 * able to create this factory without the Eclipse runtime.
	 */
	private void registerResourceFactory() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
				"opf", new OPFResourceFactoryImpl() {
					@Override
					public Resource createResource(URI uri) {
						OPFResourceImpl xmiResource = new OPFResourceImpl(uri) {

							@Override
							protected XMLHelper createXMLHelper() {
								EPUBXMLHelperImp xmlHelper = new EPUBXMLHelperImp();
								return xmlHelper;
							}

						};
						return xmiResource;
					}

				});
	}

	/**
	 * Specifies a new title for the publication. There must be at least one.
	 * 
	 * @param title
	 *            the new title
	 */
	public Title addTitle(String title) {
		Title dc = DCFactory.eINSTANCE.createTitle();
		FeatureMapUtil.addText(dc.getMixed(), title);
		opfMetadata.getTitles().add(dc);
		return dc;
	}

	/**
	 * Specifies a new creator for the publication.
	 * 
	 * @param name
	 * @param role
	 */
	public Creator addCreator(String name, Role role) {
		Creator dc = DCFactory.eINSTANCE.createCreator();
		dc.setRole(role);
		FeatureMapUtil.addText(dc.getMixed(), name);
		opfMetadata.getCreators().add(dc);
		return dc;
	}

	/**
	 * Specifies a new creator for the publication.
	 * 
	 * @param name
	 * @param role
	 */
	public Creator addCreator(String name, Role role, String fileAs) {
		Creator dcCreator = addCreator(name, role);
		return dcCreator;
	}

	/**
	 * Adds a new subject for the publication.
	 * 
	 * @param subject
	 *            the subject
	 */
	public Subject addSubject(String subject) {
		Subject dc = DCFactory.eINSTANCE.createSubject();
		FeatureMapUtil.addText(dc.getMixed(), subject);
		opfMetadata.getSubjects().add(dc);
		return dc;
	}

	public Description addDescription(String description) {
		Description dc = DCFactory.eINSTANCE.createDescription();
		FeatureMapUtil.addText(dc.getMixed(), description);
		opfMetadata.setDescription(dc);
		return dc;
	}

	public Publisher addPublisher(String publisher) {
		Publisher dc = DCFactory.eINSTANCE.createPublisher();
		FeatureMapUtil.addText(dc.getMixed(), publisher);
		opfMetadata.setPublisher(dc);
		return dc;
	}
}
