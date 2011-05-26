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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.mylyn.docs.epub.dc.Contributor;
import org.eclipse.mylyn.docs.epub.dc.Creator;
import org.eclipse.mylyn.docs.epub.dc.DCFactory;
import org.eclipse.mylyn.docs.epub.dc.Date;
import org.eclipse.mylyn.docs.epub.dc.Description;
import org.eclipse.mylyn.docs.epub.dc.Format;
import org.eclipse.mylyn.docs.epub.dc.Identifier;
import org.eclipse.mylyn.docs.epub.dc.Language;
import org.eclipse.mylyn.docs.epub.dc.Publisher;
import org.eclipse.mylyn.docs.epub.dc.Subject;
import org.eclipse.mylyn.docs.epub.dc.Title;
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
import org.eclipse.mylyn.docs.epub.opf.OPFPackage;
import org.eclipse.mylyn.docs.epub.opf.Package;
import org.eclipse.mylyn.docs.epub.opf.Reference;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.eclipse.mylyn.docs.epub.opf.Spine;
import org.eclipse.mylyn.docs.epub.opf.Type;
import org.eclipse.mylyn.docs.epub.opf.util.OPFResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.opf.util.OPFResourceImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This particular type represents one EPUB revision 2.0.1 formatted
 * publication. It maintains a data structure representing the entire
 * publication and API for building it.
 * 
 * @author Torkild U. Resheim
 */
public class EPUB2 {

	private static final String DEFAULT_MIMETYPE = "application/xhtml+xml";

	/** Identifier of the table of contents file */
	private static final String TABLE_OF_CONTENTS_ID = "ncx";

	/** Whether or not a table of contents should be automatically generated */
	private boolean generateToc;
	final Ncx ncxTOC;
	private final Guide opfGuide;
	private final Manifest opfManifest;
	private final Metadata opfMetadata;
	private final Package opfPackage;

	private final Spine opfSpine;

	private String path;

	private File tocFile;

	/**
	 * Creates a new EPUB file using the specified path.
	 * 
	 * @param file
	 *            the EPUB file
	 */
	public EPUB2() {
		// Start with the root of the OPF structure
		opfPackage = OPFFactory.eINSTANCE.createPackage();
		opfPackage.setVersion("2.0");
		ncxTOC = NCXFactory.eINSTANCE.createNcx();
		opfMetadata = OPFFactory.eINSTANCE.createMetadata();
		opfPackage.setMetadata(opfMetadata);
		opfGuide = OPFFactory.eINSTANCE.createGuide();
		opfPackage.setGuide(opfGuide);
		opfManifest = OPFFactory.eINSTANCE.createManifest();
		opfPackage.setManifest(opfManifest);
		// Create the spine and set a reference to the table of contents item
		// which will be added to the manifest on a later stage.
		opfSpine = OPFFactory.eINSTANCE.createSpine();
		opfSpine.setToc(TABLE_OF_CONTENTS_ID);
		opfPackage.setSpine(opfSpine);
		registerOPFResourceFactory();
		registerNCXResourceFactory();

		generateToc = false;
	}

	/**
	 * Adds data to the publication that we always want to be present.
	 * <ul>
	 * <li>The creation date.</li>
	 * <li><i>Eclipse committers and contributors</i> as contributor redactor
	 * role.</li>
	 * </ul>
	 */
	private void addCompulsoryData() {
		addDate(new java.util.Date(System.currentTimeMillis()), "creation");
		addContributor("Eclipse Committers and Contributors", Role.REDACTOR,
				null, null);
	}

	/**
	 * Specifies a new contributor for the publication.
	 * 
	 * @param name
	 *            name of the creator
	 * @param role
	 *            the role or <code>null</code>
	 * @param fileAs
	 *            name to file the creator under or <code>null</code>
	 * @param lang
	 *            the language code or <code>null</code>
	 * @return the new creator
	 */
	public Contributor addContributor(String name, Role role, String fileAs,
			String lang) {
		Contributor dc = DCFactory.eINSTANCE.createContributor();
		FeatureMapUtil.addText(dc.getMixed(), name);
		opfMetadata.getContributors().add(dc);
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
	 * Specifies a new creator for the publication.
	 * 
	 * @param name
	 *            name of the creator
	 * @param role
	 *            the role or <code>null</code>
	 * @param fileAs
	 *            name to file the creator under or <code>null</code>
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

	public Date addDate(java.util.Date date, String event) {
		Date dc = DCFactory.eINSTANCE.createDate();
		FeatureMapUtil.addText(dc.getMixed(), toString(date));
		opfMetadata.getDates().add(dc);
		if (event != null) {
			dc.setEvent(event);
		}
		return dc;
	}

	/**
	 * Date of publication, in the format defined by "Date and Time Formats" at
	 * http://www.w3.org/TR/NOTE-datetime and by ISO 8601 on which it is based.
	 * In particular, dates without times are represented in the form
	 * YYYY[-MM[-DD]]: a required 4-digit year, an optional 2-digit month, and
	 * if the month is given, an optional 2-digit day of month. The date element
	 * has one optional OPF event attribute. The set of values for event are not
	 * defined by this specification; possible values may include: creation,
	 * publication, and modification.
	 * 
	 * @param date
	 *            the date string
	 * @param event
	 *            an option event description
	 * @return the new date
	 */
	public Date addDate(String date, String event) {
		Date dc = DCFactory.eINSTANCE.createDate();
		FeatureMapUtil.addText(dc.getMixed(), date);
		opfMetadata.getDates().add(dc);
		if (event != null) {
			dc.setEvent(event);
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
	 * Adds a new identifier to the publication.
	 * 
	 * @param id
	 *            the identifier id
	 * @param scheme
	 *            the scheme used for representing the identifier
	 * @param value
	 *            the identifier value
	 * @return the new identifier
	 */
	public Identifier addIdentifier(String id, Scheme scheme, String value) {
		Identifier dc = DCFactory.eINSTANCE.createIdentifier();
		dc.setId(id);
		dc.setScheme(scheme);
		FeatureMapUtil.addText(dc.getMixed(), value);
		opfMetadata.getIdentifiers().add(dc);
		return dc;
	}

	/**
	 * Adds a new item to the manifest. If an identifier is not specified it
	 * will automatically be assigned.
	 * 
	 * <p>
	 * The
	 * <q>spine</q> defines the reading order, so the order items are added and
	 * whether or not <i>spine</i> is <code>true</code> does matter.
	 * </p>
	 * 
	 * @param file
	 *            the file to add
	 * @param id
	 *            identifier or <code>null</code>
	 * @param type
	 *            MIME file type
	 * @param spine
	 *            whether or not to add the item to the spine
	 * @return the new item
	 */
	public Item addItem(File file, String id, String type, boolean spine) {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("\"file\" "
					+ file.getAbsolutePath() + " must exist.");
		}
		Item item = OPFFactory.eINSTANCE.createItem();
		if (type == null && !spine) {
			type = getMimeType(file);
			if (type == null) {
				throw new IllegalArgumentException(
						"Could not automatically determine MIME type for file "
								+ file + ". Please specify the correct value");
			}
		} else {
			type = DEFAULT_MIMETYPE;
		}
		if (id == null) {
			id = file.getName().substring(0, file.getName().lastIndexOf('.'));
		}
		item.setId(id);
		item.setHref(file.getName());
		item.setMedia_type(type);
		item.setFile(file.getAbsolutePath());
		opfManifest.getItems().add(item);
		if (spine) {
			Itemref ref = OPFFactory.eINSTANCE.createItemref();
			ref.setIdref(id);
			opfSpine.getSpineItems().add(ref);
		}
		return item;
	}

	/**
	 * Adds a new language specification to the publication
	 * 
	 * @param lang
	 *            the RFC-3066 format of the language code
	 * @return the language instance
	 */
	public Language addLanguage(String lang) {
		Language dc = DCFactory.eINSTANCE.createLanguage();
		FeatureMapUtil.addText(dc.getMixed(), lang);
		opfMetadata.getLanguages().add(dc);
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
	 * The structural components of the books are listed in reference elements
	 * contained within the guide element. These components could refer to the
	 * table of contents, list of illustrations, foreword, bibliography, and
	 * many other standard parts of the book. Reading systems are not required
	 * to use the guide element but it is a good idea to use it.
	 * 
	 * @param href
	 *            the item referenced
	 * @param title
	 *            title of the reference
	 * @param type
	 *            type of the reference
	 * @return the reference
	 */
	public Reference addReference(String href, String title, Type type) {
		Reference reference = OPFFactory.eINSTANCE.createReference();
		reference.setHref(href);
		reference.setTitle(title);
		reference.setType(type);
		opfGuide.getGuideItems().add(reference);
		return reference;
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
	 * Adds a new &quot;Dublin Core Type&quot; to the publication.
	 * <p>
	 * This property is optional.
	 * </p>
	 * 
	 * @param type
	 *            the type to add
	 * @return the new type
	 */
	public org.eclipse.mylyn.docs.epub.dc.Type addType(String type) {
		org.eclipse.mylyn.docs.epub.dc.Type dc = DCFactory.eINSTANCE
				.createType();
		FeatureMapUtil.addText(dc.getMixed(), type);
		opfMetadata.getTypes().add(dc);
		return dc;
	}

	/**
	 * Sets the &quot;Dublin Core Format&quot; of the publication.
	 * <p>
	 * This property is optional.
	 * </p>
	 * 
	 * @param type
	 *            the format to add
	 * @return the new format
	 */
	public Format addFormat(String format) {
		Format dc = DCFactory.eINSTANCE
				.createFormat();
		FeatureMapUtil.addText(dc.getMixed(), format);
		opfMetadata.setFormat(dc);
		return dc;
	}

	/**
	 * Creates the final EPUB file.
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void assemble() throws IOException, SAXException,
			ParserConfigurationException {
		File workingFolder = File.createTempFile("epub_", null);
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			assemble(workingFolder);
		}
		workingFolder.deleteOnExit();
	}

	public void assemble(File workingFolder) throws IOException, SAXException,
			ParserConfigurationException {
		System.out.println("Assembling EPUB file in " + workingFolder);
		addCompulsoryData();
		validate();
		// Note that order is important here. Some methods may insert data into
		// the EPUB structure. Hence the OPF must be written last.
		if (workingFolder.isDirectory() || workingFolder.mkdirs()) {
			writeContainer(workingFolder);
			File oepbsFolder = new File(workingFolder.getAbsolutePath()
					+ File.separator + "OEBPS");
			if (oepbsFolder.mkdir()) {
				copyContent(oepbsFolder);
				writeNCX(oepbsFolder);
				writeOPF(oepbsFolder);
			} else {
				throw new IOException("Could not create OEBPS folder in "
						+ oepbsFolder.getAbsolutePath());
			}
			EPUBFileUtil.zip(new File(path), workingFolder);
		} else {
			throw new IOException("Could not create working folder in "
					+ workingFolder.getAbsolutePath());
		}
	}

	/**
	 * Copies all items part of the publication into the OEPBS folder.
	 * 
	 * @param oepbsFolder
	 *            the folder to copy into.
	 * @throws IOException
	 */
	private void copyContent(File oepbsFolder) throws IOException {
		EList<Item> items = opfManifest.getItems();
		for (Item item : items) {
			File source = new File(item.getFile());
			File destination = new File(oepbsFolder.getAbsolutePath()
					+ File.separator + source.getName());
			item.setHref(source.getName());
			EPUBFileUtil.copy(source, destination);
		}
	}

	/**
	 * This mechanism will traverse the spine of the publication (which is
	 * representing the reading order) and parse each file for headers that can
	 * be used to assemble a table of contents.
	 * 
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private void generateNCX() throws SAXException, IOException,
			ParserConfigurationException {
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

		EList<Itemref> items = opfSpine.getSpineItems();
		for (Itemref itemref : items) {
			Item referencedItem = null;
			String id = itemref.getIdref();
			EList<Item> manifestItems = opfManifest.getItems();
			for (Item item : manifestItems) {
				if (item.getId().equals(id)) {
					referencedItem = item;
					break;
				}
			}
			if (referencedItem != null) {
				FileReader fr = new FileReader(referencedItem.getFile());
				TOCGenerator.parse(new InputSource(fr),
						referencedItem.getHref(), ncxTOC);
			}
		}
	}

	/**
	 * Returns the main identifier of the publication or <code>null</code> if it
	 * could not be determined.
	 * 
	 * @return the main identifier or <code>null</code>
	 */
	private Identifier getIdentifier() {
		EList<Identifier> identifiers = opfMetadata.getIdentifiers();
		for (Identifier identifier : identifiers) {
			if (identifier.getId().equals(opfPackage.getUniqueIdentifier())) {
				return identifier;
			}
		}
		return null;
	}

	/**
	 * Attempts to figure out the MIME-type for the file.
	 * 
	 * @param file
	 *            the file to determine MIME-type for
	 * @return the MIME-type or <code>null</code>
	 */
	private String getMimeType(File file) {
		String mimeType = URLConnection
				.guessContentTypeFromName(file.getName());
		if (mimeType == null) {
			try {
				InputStream is = new BufferedInputStream(new FileInputStream(
						file));
				mimeType = URLConnection.guessContentTypeFromStream(is);
				is.close();
				// TODO: Improve upon this
				if (mimeType == null) {
					if (file.getName().endsWith(".otf")) {
						return "font/opentype";
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mimeType;
	}

	public File getTocPath() {
		return tocFile;
	}

	public boolean isGenerateToc() {
		return generateToc;
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

	/**
	 * Registers a new resource factory for OPF data structures. This is
	 * normally done through Eclipse extension points but we also need to be
	 * able to create this factory without the Eclipse runtime.
	 */
	private void registerOPFResourceFactory() {
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

	public void setFile(String file) {
		this.path = file;
	}

	public void setGenerateToc(boolean generateToc) {
		this.generateToc = generateToc;
	}

	public void setIdentifierId(String identifier_id) {
		opfPackage.setUniqueIdentifier(identifier_id);
	}

	public void setTocFile(File tocFile) {
		this.tocFile = tocFile;
	}

	/**
	 * Represents a {@link java.util.Date} instance in a format defined by
	 * "Date and Time Formats" at http://www.w3.org/TR/NOTE-datetime and by ISO
	 * 8601 on which it is based.
	 * 
	 * @param date
	 *            the date to represent
	 * @return the formatted string
	 */
	public String toString(java.util.Date date) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		df.setTimeZone(tz);
		return df.format(date);
	}

	/**
	 * Validates the OPF structure.
	 * 
	 * @return <code>true</code> if the model is valid, <code>false</code>
	 *         otherwise.
	 */
	private boolean validate() {
		EValidator.Registry.INSTANCE.put(OPFPackage.eINSTANCE,
				new EcoreValidator());
		BasicDiagnostic diagnostics = new BasicDiagnostic();
		boolean valid = true;
		for (EObject eo : opfPackage.eContents()) {
			Map<Object, Object> context = new HashMap<Object, Object>();
			valid &= Diagnostician.INSTANCE.validate(eo, diagnostics, context);
		}
		if (!valid) {
			List<Diagnostic> problems = diagnostics.getChildren();
			for (Diagnostic diagnostic : problems) {
				System.err.println(diagnostic.getMessage());
			}
		}
		return valid;
	}

	/**
	 * Creates a new folder named META-INF and writes the required
	 * <b>container.xml</b> in that folder.
	 * 
	 * @param workingFolder
	 *            the root folder
	 */
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

	/**
	 * Writes the table of contents file in the specified folder
	 * 
	 * @param oepbsFolder
	 *            the folder to create the file in.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void writeNCX(File oepbsFolder) throws IOException, SAXException,
			ParserConfigurationException {
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
			if (generateToc) {
				generateNCX();
			}
			resource.getContents().add(ncxTOC);
			Map<String, Object> options = new HashMap<String, Object>();
			// NCX requires that we encode using UTF-8
			options.put(XMLResource.OPTION_ENCODING, "UTF-8");
			options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
			resource.save(options);
		} else {
			EPUBFileUtil.copy(tocFile, ncxFile);
		}
		// As we now have written the table of contents we must make sure it is
		// in the manifest and referenced in the spine. We also want it to be
		// the first element in the manifest.
		Item item = addItem(ncxFile, opfSpine.getToc(),
				"application/x-dtbncx+xml", false);
		opfPackage.getManifest().getItems().move(0, item);
	}

	/**
	 * Writes the <b>content.opf</b> file.
	 * 
	 * @param oebpsFolder
	 *            the folder where to write the file.
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
}
