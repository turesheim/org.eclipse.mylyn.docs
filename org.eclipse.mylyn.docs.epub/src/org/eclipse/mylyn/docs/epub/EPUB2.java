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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.mylyn.docs.epub.internal.EPUBFileUtil;
import org.eclipse.mylyn.docs.epub.internal.EPUBXMLHelperImp;
import org.eclipse.mylyn.docs.epub.internal.TOCGenerator;
import org.eclipse.mylyn.docs.epub.ncx.DocTitle;
import org.eclipse.mylyn.docs.epub.ncx.Head;
import org.eclipse.mylyn.docs.epub.ncx.Meta;
import org.eclipse.mylyn.docs.epub.ncx.NCXFactory;
import org.eclipse.mylyn.docs.epub.ncx.NCXPackage;
import org.eclipse.mylyn.docs.epub.ncx.NavMap;
import org.eclipse.mylyn.docs.epub.ncx.Ncx;
import org.eclipse.mylyn.docs.epub.ncx.Text;
import org.eclipse.mylyn.docs.epub.ncx.util.NCXResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.ncx.util.NCXResourceImpl;
import org.eclipse.mylyn.docs.epub.opf.Guide;
import org.eclipse.mylyn.docs.epub.opf.Item;
import org.eclipse.mylyn.docs.epub.opf.Itemref;
import org.eclipse.mylyn.docs.epub.opf.Manifest;
import org.eclipse.mylyn.docs.epub.opf.Metadata;
import org.eclipse.mylyn.docs.epub.opf.OPFFactory;
import org.eclipse.mylyn.docs.epub.opf.Spine;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This type represents one EPUB revision 2.0.1 formatted publication. It
 * maintains a data structure representing the entire publication and API for
 * building it.
 * <p>
 * Please note that this API is provisional and should not yet be used to build
 * applications.
 * </p>
 * 
 * @author Torkild U. Resheim
 */
class EPUB2 extends EPUB {

	protected final Ncx ncxTOC;

	/**
	 * This mechanism will traverse the spine of the publication (which is
	 * representing the reading order) and parse each file for information that
	 * can be used to assemble a table of contents.
	 * 
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@Override
	protected void generateTableOfContents() throws Exception {
		NavMap navMap = NCXFactory.eINSTANCE.createNavMap();
		ncxTOC.setNavMap(navMap);
		ncxTOC.setVersion("2005-1");
		// Create the required head element
		Head head = NCXFactory.eINSTANCE.createHead();
		ncxTOC.setHead(head);
		Meta meta = NCXFactory.eINSTANCE.createMeta();
		meta.setName("dtb:uid");
		meta.setContent(getIdentifier().getMixed().getValue(0).toString());
		head.getMeta().add(meta);
		DocTitle docTitle = NCXFactory.eINSTANCE.createDocTitle();
		Text text = NCXFactory.eINSTANCE.createText();
		FeatureMapUtil.addText(text.getMixed(), "Table of contents");
		docTitle.setText(text);
		ncxTOC.setDocTitle(docTitle);
		int playOrder = 0;
		// Iterate over the spine
		EList<Itemref> spineItems = getSpine().getSpineItems();
		EList<Item> manifestItems = opfPackage.getManifest().getItems();
		for (Itemref itemref : spineItems) {
			Item referencedItem = null;
			String id = itemref.getIdref();
			// Find the manifest item that is referenced
			for (Item item : manifestItems) {
				if (item.getId().equals(id)) {
					referencedItem = item;
					break;
				}
			}
			if (referencedItem != null && !referencedItem.isNoToc()) {
				System.out.println("Generating table of contents from "
						+ referencedItem.getFile());
				FileReader fr = new FileReader(referencedItem.getFile());
				playOrder = TOCGenerator.parse(new InputSource(fr),
						referencedItem.getHref(), ncxTOC, playOrder);
			}
		}
	}

	/**
	 * Writes the table of contents file in the specified folder
	 * 
	 * @param oepbsFolder
	 *            the folder to create the file in.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Override
	protected void writeTableOfContents(File oepbsFolder) throws Exception {
		File ncxFile = new File(oepbsFolder.getAbsolutePath() + File.separator
				+ "toc.ncx");
		// A path to a table of contents have not been specified, so we will
		// use data from our model to generate the file.
		if (tocFile == null) {
			ResourceSet resourceSet = new ResourceSetImpl();
			// Register the packages to make it available during loading.
			resourceSet.getPackageRegistry().put(NCXPackage.eNS_URI,
					NCXPackage.eINSTANCE);
			URI fileURI = URI.createFileURI(ncxFile.getAbsolutePath());
			Resource resource = resourceSet.createResource(fileURI);
			// We've been asked to generate a table of contents using pages
			// contained in the spine.
			if (opfPackage.isGenerateTableOfContents()) {
				generateTableOfContents();
			}
			resource.getContents().add(ncxTOC);
			Map<String, Object> options = new HashMap<String, Object>();
			// NCX requires that we encode using UTF-8
			options.put(XMLResource.OPTION_ENCODING, XML_ENCODING);
			options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
			resource.save(options);
		} else {
			EPUBFileUtil.copy(tocFile, ncxFile);
		}
		// As we now have written the table of contents we must make sure it is
		// in the manifest and referenced in the spine. We also want it to be
		// the first element in the manifest.
		Item item = addItem(opfPackage.getSpine().getToc(), null, ncxFile,
				null, "application/x-dtbncx+xml", false, false);
		opfPackage.getManifest().getItems().move(0, item);
	}

	/**
	 * Creates a new EPUB.
	 */
	public EPUB2() {
		super();
		opfPackage.setVersion("2.0");
		ncxTOC = NCXFactory.eINSTANCE.createNcx();
		Metadata opfMetadata = OPFFactory.eINSTANCE.createMetadata();
		opfPackage.setMetadata(opfMetadata);
		Guide opfGuide = OPFFactory.eINSTANCE.createGuide();
		opfPackage.setGuide(opfGuide);
		Manifest opfManifest = OPFFactory.eINSTANCE.createManifest();
		opfPackage.setManifest(opfManifest);
		// Create the spine and set a reference to the table of contents
		// item which will be added to the manifest on a later stage.
		Spine opfSpine = OPFFactory.eINSTANCE.createSpine();
		opfSpine.setToc(TABLE_OF_CONTENTS_ID);
		opfPackage.setSpine(opfSpine);

		registerOPFResourceFactory();
		registerNCXResourceFactory();
		opfPackage.setGenerateTableOfContents(true);
	}

	/**
	 * Registers a new resource factory for NCX data structures. This is
	 * normally done through Eclipse extension points but we also need to be
	 * able to create this factory without the Eclipse runtime.
	 */
	private void registerNCXResourceFactory() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
				TABLE_OF_CONTENTS_ID, new NCXResourceFactoryImpl() {
					@Override
					public Resource createResource(URI uri) {
						NCXResourceImpl xmiResource = new NCXResourceImpl(uri) {

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

}
