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
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

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
import org.eclipse.mylyn.docs.epub.dc.DCType;
import org.eclipse.mylyn.docs.epub.dc.Date;
import org.eclipse.mylyn.docs.epub.dc.Description;
import org.eclipse.mylyn.docs.epub.dc.Format;
import org.eclipse.mylyn.docs.epub.dc.Identifier;
import org.eclipse.mylyn.docs.epub.dc.Language;
import org.eclipse.mylyn.docs.epub.dc.LocalizedDCType;
import org.eclipse.mylyn.docs.epub.dc.Publisher;
import org.eclipse.mylyn.docs.epub.dc.Relation;
import org.eclipse.mylyn.docs.epub.dc.Rights;
import org.eclipse.mylyn.docs.epub.dc.Source;
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
public final class EPUB2 {

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
	 * @param href
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
		generateToc = true;
	}

	/**
	 * Adds data to the publication that we always want to be present.
	 * <ul>
	 * <li>The creation date.</li>
	 * <li><i>Eclipse committers and contributors</i> as contributor redactor
	 * role.</li>
	 * <li>A unique identifier if none has been specified.</li>
	 * </ul>
	 */
	private void addCompulsoryData() {
		addDate(null, new java.util.Date(System.currentTimeMillis()),
				"creation");
		addContributor(null, null, "Eclipse Committers and Contributors",
				Role.REDACTOR, null);
		if (getIdentifier() == null) {
			addIdentifier("uuid", Scheme.UUID, UUID.randomUUID().toString());
			setIdentifierId("uuid");
		}
	}

	/**
	 * Specifies a new contributor for the publication.
	 * 
	 * @param id
	 *            an identifier or <code>null</code>
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
	public Contributor addContributor(String id, Locale lang, String name,
			Role role, String fileAs) {
		Contributor dc = DCFactory.eINSTANCE.createContributor();
		setDcLocalized(dc, id, lang, name);
		if (role != null) {
			dc.setRole(role);
		}
		if (fileAs != null) {
			dc.setFileAs(fileAs);
		}
		opfMetadata.getContributors().add(dc);
		return dc;
	}

	/**
	 * Specifies a new creator for the publication.
	 * 
	 * 
	 * @param id
	 *            a unique identifier or <code>null</code>
	 * @param lang
	 *            the language code or <code>null</code>
	 * @param name
	 *            name of the creator
	 * @param role
	 *            the role or <code>null</code>
	 * @param fileAs
	 *            name to file the creator under or <code>null</code>
	 * @return the new creator
	 */
	public Creator addCreator(String id, Locale lang, String name, Role role,
			String fileAs) {
		Creator dc = DCFactory.eINSTANCE.createCreator();
		setDcLocalized(dc, id, lang, name);
		if (role != null) {
			dc.setRole(role);
		}
		if (fileAs != null) {
			dc.setFileAs(fileAs);
		}
		opfMetadata.getCreators().add(dc);
		return dc;
	}

	public Date addDate(String id, java.util.Date date, String event) {
		Date dc = DCFactory.eINSTANCE.createDate();
		setDcCommon(dc, id, toString(date));
		if (event != null) {
			dc.setEvent(event);
		}
		opfMetadata.getDates().add(dc);
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
	public Date addDate(String id, String date, String event) {
		Date dc = DCFactory.eINSTANCE.createDate();
		setDcCommon(dc, id, date);
		if (event != null) {
			dc.setEvent(event);
		}
		opfMetadata.getDates().add(dc);
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
	public Description addDescription(String id, Locale lang, String description) {
		Description dc = DCFactory.eINSTANCE.createDescription();
		setDcLocalized(dc, id, lang, description);
		opfMetadata.setDescription(dc);
		return dc;
	}

	/**
	 * Sets the &quot;Dublin Core Format&quot; of the publication.
	 * <p>
	 * This property is optional.
	 * </p>
	 * 
	 * @param value
	 *            the format to add
	 * @return the new format
	 */
	public Format addFormat(String id, String value) {
		Format dc = DCFactory.eINSTANCE.createFormat();
		setDcCommon(dc, id, value);
		opfMetadata.setFormat(dc);
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
	 * Adds a new item to the manifest using default values for properties not
	 * specified. Same as
	 * <code>addItem(null, null, file, null, null, true, true);</code>.
	 * 
	 * @param file
	 * @return
	 */
	public Item addItem(File file) {
		return addItem(null, null, file, null, null, true, true);
	}

	/**
	 * Adds a new item to the manifest. If an identifier is not specified it
	 * will automatically be assigned.
	 * 
	 * <p>
	 * The <i>spine</i> defines the reading order, so the order items are added
	 * and whether or not <i>spine</i> is <code>true</code> does matter. Unless
	 * a table of contents file has been specified it will be generated. All
	 * files that have been added to the spine will be examined unless the
	 * <i>noToc</i> attribute has been set to <code>true</code>.
	 * </p>
	 * 
	 * @param file
	 *            the file to add
	 * @param dest
	 *            the destination sub-folder or <code>null</code>
	 * @param id
	 *            identifier or <code>null</code>
	 * @param type
	 *            MIME file type
	 * @param spine
	 *            whether or not to add the item to the spine
	 * @param noToc
	 *            whether or not to include in TOC when automatically generated
	 * @return the new item
	 */
	public Item addItem(String id, Locale lang, File file, String dest,
			String type, boolean spine, boolean noToc) {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("\"file\" "
					+ file.getAbsolutePath() + " must exist.");
		}
		Item item = OPFFactory.eINSTANCE.createItem();
		if (type == null) {
			if (!spine) {
				type = getMimeType(file);
				if (type == null) {
					throw new IllegalArgumentException(
							"Could not automatically determine MIME type for file "
									+ file
									+ ". Please specify the correct value");
				}
			} else {
				type = DEFAULT_MIMETYPE;
			}
		}
		if (id == null) {
			String prefix = "";
			if (!type.equals(DEFAULT_MIMETYPE)) {
				prefix = (type.indexOf('/')) == -1 ? type : type.substring(0,
						type.indexOf('/')) + "-";
			}
			id = prefix
					+ file.getName().substring(0,
							file.getName().lastIndexOf('.'));
		}
		item.setId(id);
		if (dest == null) {
			item.setHref(file.getName());
		} else {
			item.setHref(dest + '/' + file.getName());
		}
		item.setNoToc(noToc);
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
	public Language addLanguage(String id, String lang) {
		Language dc = DCFactory.eINSTANCE.createLanguage();
		setDcCommon(dc, id, lang);
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
	public Publisher addPublisher(String id, Locale lang, String publisher) {
		Publisher dc = DCFactory.eINSTANCE.createPublisher();
		setDcLocalized(dc, id, lang, publisher);
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

	public Relation addRelation(String id, Locale lang, String value) {
		Relation dc = DCFactory.eINSTANCE.createRelation();
		setDcLocalized(dc, id, lang, value);
		opfMetadata.setRelation(dc);
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
	public Subject addSubject(String id, Locale lang, String subject) {
		Subject dc = DCFactory.eINSTANCE.createSubject();
		setDcLocalized(dc, id, lang, subject);
		opfMetadata.getSubjects().add(dc);
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
	public Title addTitle(String id, Locale lang, String title) {
		Title dc = DCFactory.eINSTANCE.createTitle();
		setDcLocalized(dc, id, lang, title);
		opfMetadata.getTitles().add(dc);
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
	public org.eclipse.mylyn.docs.epub.dc.Type addType(String id, String value) {
		org.eclipse.mylyn.docs.epub.dc.Type dc = DCFactory.eINSTANCE
				.createType();
		setDcCommon(dc, id, value);
		opfMetadata.getTypes().add(dc);
		return dc;
	}

	/**
	 * Creates the final EPUB file.
	 * 
	 * @throws Exception
	 */
	public void assemble() throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			assemble(workingFolder);
		}
		workingFolder.deleteOnExit();
	}

	public void assemble(File workingFolder) throws Exception {
		System.out.println("Assembling EPUB file in " + workingFolder);
		addCompulsoryData();
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
		validate();
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
					+ File.separator + item.getHref());
			EPUBFileUtil.copy(source, destination);
		}
	}

	/**
	 * This mechanism will traverse the spine of the publication (which is
	 * representing the reading order) and parse each file for information that
	 * can be used to assemble a table of contents.
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
		int playOrder = 0;
		// Iterate over the spine
		EList<Itemref> items = opfSpine.getSpineItems();
		EList<Item> manifestItems = opfManifest.getItems();
		for (Itemref itemref : items) {
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

	private void setDcCommon(DCType dc, String id, String value) {
		FeatureMapUtil.addText(dc.getMixed(), value);
		if (id != null) {
			dc.setId(id);
		}
	}

	private void setDcLocalized(LocalizedDCType dc, String id, Locale lang,
			String value) {
		setDcCommon(dc, id, value);
		if (lang != null) {
			dc.setLang(lang.toString());
		}
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

	/**
	 * Sets the &quot;Dublin Core Rights&quot; of the publication.
	 * <p>
	 * This property is optional.
	 * </p>
	 * 
	 * @param text
	 *            the rights text
	 * @return the new rights element
	 */
	public Rights setRights(String id, Locale lang, String value) {
		Rights dc = DCFactory.eINSTANCE.createRights();
		setDcLocalized(dc, id, lang, value);
		opfMetadata.setRights(dc);
		return dc;
	}

	/**
	 * Sets the &quot;Dublin Core Source&quot; of the publication.
	 * <p>
	 * This property is optional.
	 * </p>
	 * 
	 * @param text
	 *            the source text
	 * @return the new source element
	 */
	public Source setSource(String id, Locale lang, String value) {
		Source dc = DCFactory.eINSTANCE.createSource();
		setDcLocalized(dc, id, lang, value);
		opfMetadata.setSource(dc);
		return dc;
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
		Item item = addItem(opfSpine.getToc(), null, ncxFile, null,
				"application/x-dtbncx+xml", false, false);
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
