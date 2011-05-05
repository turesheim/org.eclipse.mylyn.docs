/*******************************************************************************
 * Copyright (c) 2011 Torkild U. Resheim.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Torkild U. Resheim - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.docs.epub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

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
import org.eclipse.mylyn.docs.epub.opf.Item;
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

	/**
	 * Creates a new instance of a EPUB document using the specified file. If
	 * the file already exists one can use {@link #disassemble()} to take it
	 * apart and read it's information. Otherwise one can use
	 * {@link #assemble()} to create a new EPUB file after first populating it
	 * with data.
	 * 
	 * @param file
	 *            the EPUB file
	 */
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

	public void disassemble() {

	}

	/**
	 * Creates the final EPUB file.
	 */
	public void assemble() throws IOException {
		File workingFolder = File.createTempFile("epub_", null);
		System.out.println("Assembling EPUB file in " + workingFolder);
		if (workingFolder.delete() && workingFolder.mkdir()) {
			writeMimetype(workingFolder);
			writeContainer(workingFolder);
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

	private void writeContainer(File workingFolder) {
		File metaFolder = new File(workingFolder.getAbsolutePath()
				+ File.separator + "META-INF");
		if (metaFolder.mkdir()) {
			File containerFile = new File(metaFolder.getAbsolutePath()
					+ File.separator + "container.xml");
			if (!containerFile.exists()) {
				try {
					FileWriter fw = new FileWriter(containerFile);
					fw.append("<?xml version=\"1.0\"?>\n");
					fw.append("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n");
					fw.append("  <rootfiles>\n");
					fw.append("    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n");
					fw.append("  </rootfiles>\n");
					fw.append("</container>\n");
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void compress(File root, ZipOutputStream zos) {
		// TODO Auto-generated method stub
	}

	private void compress(File oepbsFolder) throws ZipException, IOException {
		// TODO Auto-generated method stub
	}

	private void copyContent(File oepbsFolder) {
		// TODO Auto-generated method stub

	}

	private void writeTOC(File oepbsFolder) {
		// TODO Auto-generated method stub

	}

	/**
	 * Creates a new file containing the EPUB MIME-type in the working folder.
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
	 * @param lang
	 *            the language code or <code>null</code>
	 * @return the new title
	 */
	public Title addTitle(String title, String lang) {
		Title dc = DCFactory.eINSTANCE.createTitle();
		FeatureMapUtil.addText(dc.getMixed(), title);
		opfMetadata.getTitles().add(dc);
		if (lang != null) {
			dc.setLang(lang);
		}
		return dc;
	}

	/**
	 * Specifies a new creator for the publication.
	 * 
	 * @param name
	 *            name of the creator
	 * @param role
	 *            the role or <code>null</code>
	 * @param fileAs
	 *            name to file the creator under
	 * @param lang
	 *            the language code or <code>null</code>
	 * @return the new creator
	 */
	public Creator addCreator(String name, Role role, String fileAs, String lang) {
		Creator dc = DCFactory.eINSTANCE.createCreator();
		FeatureMapUtil.addText(dc.getMixed(), name);
		opfMetadata.getCreators().add(dc);
		if (role != null) {
			dc.setRole(role);
		}
		if (fileAs != null) {
			dc.setFileAs(fileAs);
		}
		if (lang != null) {
			dc.setLang(lang);
		}
		return dc;
	}

	/**
	 * Adds a new subject to the publication.
	 * 
	 * @param subject
	 *            the subject
	 * @param lang
	 *            the language code or <code>null</code>
	 */
	public Subject addSubject(String subject, String lang) {
		Subject dc = DCFactory.eINSTANCE.createSubject();
		FeatureMapUtil.addText(dc.getMixed(), subject);
		opfMetadata.getSubjects().add(dc);
		if (lang != null) {
			dc.setLang(lang);
		}
		return dc;
	}

	/**
	 * Adds a new description to the publication.
	 * 
	 * @param description
	 *            the description text
	 * @param lang
	 *            the language code or <code>null</code>
	 * @return the new description
	 */
	public Description addDescription(String description, String lang) {
		Description dc = DCFactory.eINSTANCE.createDescription();
		FeatureMapUtil.addText(dc.getMixed(), description);
		opfMetadata.setDescription(dc);
		if (lang != null) {
			dc.setLang(lang);
		}
		return dc;
	}

	/**
	 * Adds a new publisher to the publication.
	 * 
	 * @param publisher
	 *            name of the publisher
	 * @param lang
	 *            the language code or <code>null</code>
	 * @return the new publisher
	 */
	public Publisher addPublisher(String publisher, String lang) {
		Publisher dc = DCFactory.eINSTANCE.createPublisher();
		FeatureMapUtil.addText(dc.getMixed(), publisher);
		if (lang != null) {
			dc.setLang(lang);
		}
		opfMetadata.setPublisher(dc);
		return dc;
	}

	/**
	 * Adds a new item to the manifest. If an identifier is not specified it
	 * will automatically be assigned.
	 * 
	 * @param file
	 *            the file to add
	 * @param id
	 *            identifier or <code>null</code>
	 * @param type
	 *            MIME file type
	 * @return the new item
	 */
	public Item addItem(File file, String id, String type) {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("\"file\" must exist.");
		}
		if (type == null) {
			throw new IllegalArgumentException("MIME-type must be specified");
		}
		Item item = OPFFactory.eINSTANCE.createItem();
		item.setMedia_type(type);
		opfManifest.getItems().add(item);
		if (id == null) {
			id = "item-" + opfManifest.getItems().size();
			item.setId(id);
		}
		return item;
	}
}
