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
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.mylyn.docs.epub.dc.Contributor;
import org.eclipse.mylyn.docs.epub.dc.Coverage;
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
import org.eclipse.mylyn.docs.epub.internal.ImageScanner;
import org.eclipse.mylyn.docs.epub.opf.Item;
import org.eclipse.mylyn.docs.epub.opf.Itemref;
import org.eclipse.mylyn.docs.epub.opf.OPFFactory;
import org.eclipse.mylyn.docs.epub.opf.OPFPackage;
import org.eclipse.mylyn.docs.epub.opf.Package;
import org.eclipse.mylyn.docs.epub.opf.Reference;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.eclipse.mylyn.docs.epub.opf.Spine;
import org.eclipse.mylyn.docs.epub.opf.Type;
import org.eclipse.mylyn.docs.epub.opf.util.OPFResourceImpl;
import org.xml.sax.SAXException;

/**
 * This type represents one EPUB formatted publication. It maintains a data
 * structure representing the entire publication and API for building it.
 * <p>
 * Please note that this API is provisional and should not yet be used to build
 * applications.
 * </p>
 * 
 * @author Torkild U. Resheim
 */
public abstract class EPUB {
	// Rules of engagement:
	// * Keep all data in the model, use "transient" for temporary variables
	// * Do not create files before the final assemble

	private static final String OPF_FILE_SUFFIX = "opf";

	/** MIME-type of an EPUB file */
	protected static final String PUBLICATION_MIMETYPE = "application/epub+zip";

	protected static final String CREATION_DATE_ID = "creation";

	protected static final String UUID_SCHEME = "uuid";

	/** Publication identifier for the cover image item */
	public static final String COVER_IMAGE_ID = "cover-image";

	protected static final String XHTML_MIMETYPE = "application/xhtml+xml";

	/** The encoding to use in XML files */
	protected static final String XML_ENCODING = "UTF-8";

	/**
	 * Returns an EPUB version 2.0.1 instance.
	 * 
	 * @return an EPUB instance
	 */
	public static EPUB getVersion2Instance() {
		return new EPUB2();
	}

	/** The root model element */
	protected Package opfPackage;

	protected final boolean verbose = true;

	protected EPUB() {
		opfPackage = OPFFactory.eINSTANCE.createPackage();
		registerOPFResourceFactory();
	}

	/**
	 * Adds data to the publication that we always want to be present.
	 * <ul>
	 * <li>The creation date.</li>
	 * <li><i>Eclipse committers and contributors</i> as contributor redactor
	 * role.</li>
	 * <li>A unique identifier if none has been specified.</li>
	 * <li>A empty description if none has been specified.</li>
	 * <li>Language "English" if none has been specified.</li>
	 * <li>A dummy title if none has been specified.</li>
	 * <li>The publication format if none has been specified.</li>
	 * </ul>
	 */
	private void addCompulsoryData() {
		addDate(null, new java.util.Date(System.currentTimeMillis()), CREATION_DATE_ID);
		addContributor(null, null, "Eclipse Committers and Contributors", Role.REDACTOR, null);
		if (getIdentifier() == null) {
			addIdentifier(UUID_SCHEME, Scheme.UUID, UUID.randomUUID().toString());
			setIdentifierId(UUID_SCHEME);
		}
		// Add empty subject
		if (opfPackage.getMetadata().getSubjects().isEmpty()) {
			addSubject(null, null, "");
		}
		// Add English language
		if (opfPackage.getMetadata().getLanguages().isEmpty()) {
			addLanguage(null, Locale.ENGLISH.toString());
		}
		// Add dummy title
		if (opfPackage.getMetadata().getTitles().isEmpty()) {
			addTitle(null, null, "No title specified");
		}
		// Set the publication format
		if (opfPackage.getMetadata().getFormats().isEmpty()) {
			addFormat(null, PUBLICATION_MIMETYPE);
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
	public Contributor addContributor(String id, Locale lang, String name, Role role, String fileAs) {
		Contributor dc = DCFactory.eINSTANCE.createContributor();
		setDcLocalized(dc, id, lang, name);
		if (role != null) {
			dc.setRole(role);
		}
		if (fileAs != null) {
			dc.setFileAs(fileAs);
		}
		opfPackage.getMetadata().getContributors().add(dc);
		return dc;
	}

	/**
	 * Specifies a new &quot;coverage&quot; for the publication.
	 * 
	 * @param id
	 *            an identifier or <code>null</code>
	 * @param lang
	 *            the language code or <code>null</code>
	 * @param value
	 *            value of the item
	 * @return the new coverage
	 */
	public Coverage addCoverage(String id, Locale lang, String value) {
		Coverage dc = DCFactory.eINSTANCE.createCoverage();
		setDcLocalized(dc, id, lang, value);
		opfPackage.getMetadata().getCoverages().add(dc);
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
	public Creator addCreator(String id, Locale lang, String name, Role role, String fileAs) {
		Creator dc = DCFactory.eINSTANCE.createCreator();
		setDcLocalized(dc, id, lang, name);
		if (role != null) {
			dc.setRole(role);
		}
		if (fileAs != null) {
			dc.setFileAs(fileAs);
		}
		opfPackage.getMetadata().getCreators().add(dc);
		return dc;
	}

	public Date addDate(String id, java.util.Date date, String event) {
		Date dc = DCFactory.eINSTANCE.createDate();
		setDcCommon(dc, id, toString(date));
		if (event != null) {
			dc.setEvent(event);
		}
		opfPackage.getMetadata().getDates().add(dc);
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
		opfPackage.getMetadata().getDates().add(dc);
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
		opfPackage.getMetadata().getDescriptions().add(dc);
		return dc;
	}

	/**
	 * Adds a publication format. (This property is optional.)
	 * 
	 * @param value
	 *            the format to add
	 * @return the new format
	 */
	public Format addFormat(String id, String value) {
		Format dc = DCFactory.eINSTANCE.createFormat();
		setDcCommon(dc, id, value);
		opfPackage.getMetadata().getFormats().add(dc);
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
		opfPackage.getMetadata().getIdentifiers().add(dc);
		return dc;
	}

	/**
	 * Adds a new item to the manifest using default values for properties not
	 * specified. Same as
	 * <code>addItem(null, null, file, null, null, true, false);</code>.
	 * 
	 * @param file
	 * @return
	 */
	public Item addItem(File file) {
		return addItem(null, null, file, null, null, true, false);
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
	public Item addItem(String id, Locale lang, File file, String dest, String type, boolean spine, boolean noToc) {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("\"file\" " + file.getAbsolutePath() + " must exist.");
		}
		if (file.isDirectory()) {
			throw new IllegalArgumentException("\"file\" " + file.getAbsolutePath() + " must not be a directory.");
		}
		Item item = OPFFactory.eINSTANCE.createItem();
		if (type == null) {
			if (!spine) {
				type = EPUBFileUtil.getMimeType(file);
				if (type == null) {
					throw new IllegalArgumentException("Could not automatically determine MIME type for file " + file
							+ ". Please specify the correct value");
				}
			} else {
				type = XHTML_MIMETYPE;
			}
		}
		if (id == null) {
			String prefix = "";
			if (!type.equals(XHTML_MIMETYPE)) {
				prefix = (type.indexOf('/')) == -1 ? type : type.substring(0, type.indexOf('/')) + "-";
			}
			id = prefix + file.getName().substring(0, file.getName().lastIndexOf('.'));
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
		if (verbose) {
			System.out.println("Adding " + file.getName() + " (" + item.getMedia_type() + ") to publication");
		}
		opfPackage.getManifest().getItems().add(item);
		if (spine) {
			Itemref ref = OPFFactory.eINSTANCE.createItemref();
			ref.setIdref(id);
			getSpine().getSpineItems().add(ref);
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
		opfPackage.getMetadata().getLanguages().add(dc);
		return dc;
	}

	public org.eclipse.mylyn.docs.epub.opf.Meta addMeta(String name, String content) {
		org.eclipse.mylyn.docs.epub.opf.Meta opf = OPFFactory.eINSTANCE.createMeta();
		opf.setName(name);
		opf.setContent(content);
		opfPackage.getMetadata().getMetas().add(opf);
		return opf;
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
		opfPackage.getMetadata().getPublishers().add(dc);
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
		opfPackage.getGuide().getGuideItems().add(reference);
		return reference;
	}

	public Relation addRelation(String id, Locale lang, String value) {
		Relation dc = DCFactory.eINSTANCE.createRelation();
		setDcLocalized(dc, id, lang, value);
		opfPackage.getMetadata().getRelations().add(dc);
		return dc;
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
	public Rights addRights(String id, Locale lang, String value) {
		Rights dc = DCFactory.eINSTANCE.createRights();
		setDcLocalized(dc, id, lang, value);
		opfPackage.getMetadata().getRights().add(dc);
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
	public Source addSource(String id, Locale lang, String value) {
		Source dc = DCFactory.eINSTANCE.createSource();
		setDcLocalized(dc, id, lang, value);
		opfPackage.getMetadata().getSources().add(dc);
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
		opfPackage.getMetadata().getSubjects().add(dc);
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
		opfPackage.getMetadata().getTitles().add(dc);
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
		org.eclipse.mylyn.docs.epub.dc.Type dc = DCFactory.eINSTANCE.createType();
		setDcCommon(dc, id, value);
		opfPackage.getMetadata().getTypes().add(dc);
		return dc;
	}

	/**
	 * Creates the final EPUB file.
	 * 
	 * @throws Exception
	 */
	public void pack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		workingFolder.deleteOnExit();
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			pack(epubFile, workingFolder);
		}
	}

	/**
	 * Assembles the EPUB file.
	 * 
	 * @param workingFolder
	 * @throws Exception
	 */
	public void pack(File epubFile, File workingFolder) throws Exception {
		addCompulsoryData();
		// Note that order is important here. Some methods may insert data into
		// the EPUB structure. Hence the OPF must be written last.
		if (workingFolder.isDirectory() || workingFolder.mkdirs()) {
			writeContainer(workingFolder);
			File oebpsFolder = new File(workingFolder.getAbsolutePath() + File.separator + "OEBPS");
			if (oebpsFolder.mkdir()) {
				if (opfPackage.isGenerateCoverHTML()) {
					writeCoverHTML(oebpsFolder);
				}
				if (opfPackage.isIncludeReferencedResources()) {
					includeReferencedResources();
				}
				copyContent(oebpsFolder);
				writeTableOfContents(oebpsFolder);
				writeOPF(oebpsFolder);
			} else {
				throw new IOException("Could not create OEBPS folder in " + oebpsFolder.getAbsolutePath());
			}
			EPUBFileUtil.zip(epubFile, workingFolder);
			if (verbose) {
				System.out.println("Publication packed to \"" + epubFile + "\"");
			}
		} else {
			throw new IOException("Could not create working folder in " + workingFolder.getAbsolutePath());
		}
		validateModel();
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
		File oebpsFolder = new File(destination.getAbsolutePath() + File.separator + "OEBPS");
		File opfFile = new File(oebpsFolder.getAbsolutePath() + File.separator + "content.opf");
		readOPF(opfFile);
		if (verbose) {
			System.out.println("Publication unpacked at " + destination.getAbsolutePath());
		}
	}

	public void unpack(File epubFile) throws Exception {
		File workingFolder = File.createTempFile("epub_", null);
		workingFolder.deleteOnExit();
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			unpack(epubFile, workingFolder);
		}
	}

	/**
	 * Copies all items part of the publication into the OEPBS folder unless the
	 * item in question will be generated.
	 * 
	 * @param oepbsFolder
	 *            the folder to copy into.
	 * @throws IOException
	 */
	private void copyContent(File oepbsFolder) throws IOException {
		EList<Item> items = opfPackage.getManifest().getItems();
		for (Item item : items) {
			if (!item.isGenerated()) {
				File source = new File(item.getFile());
				File destination = new File(oepbsFolder.getAbsolutePath() + File.separator + item.getHref());
				EPUBFileUtil.copy(source, destination);
			}
		}
	}

	/**
	 * Implement to handle generation of table of contents from the items added
	 * to the <i>spine</i>.
	 * 
	 * @throws Exception
	 */
	protected abstract void generateTableOfContents() throws Exception;

	/**
	 * Returns the main identifier of the publication or <code>null</code> if it
	 * could not be determined.
	 * 
	 * @return the main identifier or <code>null</code>
	 */
	protected Identifier getIdentifier() {
		EList<Identifier> identifiers = opfPackage.getMetadata().getIdentifiers();
		for (Identifier identifier : identifiers) {
			if (identifier.getId().equals(opfPackage.getUniqueIdentifier())) {
				return identifier;
			}
		}
		return null;
	}

	/**
	 * Locates and returns an item from the manifest corresponding to the given
	 * identifier.
	 * 
	 * @param id
	 *            the identifier
	 * @return the item
	 */
	public Item getItemById(String id) {
		EList<Item> items = opfPackage.getManifest().getItems();
		for (Item item : items) {
			if (item.getId().equals(id)) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Returns the publication spine.
	 * 
	 * @return the spine
	 */
	protected Spine getSpine() {
		return opfPackage.getSpine();
	}

	/**
	 * Iterates over all files in the manifest attempting to determine
	 * referenced resources such as image files and adds these to the manifest.
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void includeReferencedResources() throws ParserConfigurationException, SAXException, IOException {
		EList<Item> manifestItems = opfPackage.getManifest().getItems();
		// Compose a list of file references
		HashMap<File, List<File>> references = new HashMap<File, List<File>>();
		for (Item item : manifestItems) {
			// Only parse XHTML-files and files that are not generated
			if (item.getMedia_type().equals(XHTML_MIMETYPE) && !item.isGenerated()) {
				if (item.getSourcePath() != null) {
					File source = new File(item.getSourcePath());
					references.put(source, ImageScanner.parse(item));
				} else {
					File source = new File(item.getFile());
					references.put(source, ImageScanner.parse(item));
				}
			}
		}
		for (File root : references.keySet()) {
			List<File> images = references.get(root);
			for (File image : images) {
				File relativePath = new File(EPUBFileUtil.getRelativePath(root, image));
				addItem(null, null, image, relativePath.getParent(), null, false, false);
			}
		}

	}

	/**
	 * Registers a new resource factory for OPF data structures. This is
	 * normally done through Eclipse extension points but we also need to be
	 * able to create this factory without the Eclipse runtime.
	 */
	private void registerOPFResourceFactory() {
		// Register package so that it is available even without the Eclipse
		// runtime
		@SuppressWarnings("unused")
		OPFPackage packageInstance = OPFPackage.eINSTANCE;

		// Register the file suffix
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(OPF_FILE_SUFFIX,
				new XMLResourceFactoryImpl() {

					@Override
					public Resource createResource(URI uri) {
						OPFResourceImpl xmiResource = new OPFResourceImpl(uri) {

							@Override
							protected XMLHelper createXMLHelper() {
								EPUBXMLHelperImp xmlHelper = new EPUBXMLHelperImp();
								return xmlHelper;
							}

						};
						Map<Object, Object> loadOptions = xmiResource.getDefaultLoadOptions();
						Map<Object, Object> saveOptions = xmiResource.getDefaultSaveOptions();
						saveOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
						loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
						loadOptions.put(XMLResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
						saveOptions.put(XMLResource.OPTION_ENCODING, XML_ENCODING);
						return xmiResource;
					}

				});
	}

	/**
	 * Convenience method for adding a cover to the publication. This method
	 * will make sure the required actions are taken to provide a cover page for
	 * all reading systems.
	 * 
	 * @param image
	 *            the cover image (jpeg, png, svg or gif)
	 * @param title
	 *            title of the cover page
	 */
	public void setCover(File image, String title) {
		// Add the cover image to the manifest
		Item item = addItem(COVER_IMAGE_ID, null, image, null, null, false, true);
		item.setTitle(title);
		// Point to the cover using a meta tag
		addMeta("cover", COVER_IMAGE_ID);
		opfPackage.setGenerateCoverHTML(true);

	}

	private void setDcCommon(DCType dc, String id, String value) {
		FeatureMapUtil.addText(dc.getMixed(), value);
		if (id != null) {
			dc.setId(id);
		}
	}

	private void setDcLocalized(LocalizedDCType dc, String id, Locale lang, String value) {
		setDcCommon(dc, id, value);
		if (lang != null) {
			dc.setLang(lang.toString());
		}
	}

	public void setGenerateToc(boolean generateToc) {
		opfPackage.setGenerateTableOfContents(generateToc);
	}

	public void setIdentifierId(String identifier_id) {
		opfPackage.setUniqueIdentifier(identifier_id);
	}

	public void setIncludeReferencedResources(boolean op) {
		opfPackage.setIncludeReferencedResources(op);
	}

	public abstract void setTableOfContents(File tocFile);

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
	private boolean validateModel() {
		EValidator.Registry.INSTANCE.put(OPFPackage.eINSTANCE, new EcoreValidator());
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
		File metaFolder = new File(workingFolder.getAbsolutePath() + File.separator + "META-INF");
		if (metaFolder.mkdir()) {
			File containerFile = new File(metaFolder.getAbsolutePath() + File.separator + "container.xml");
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
	 * 
	 * @param oepbsFolder
	 * @throws IOException
	 * 
	 */
	private void writeCoverHTML(File oepbsFolder) throws IOException {
		Item coverImage = getItemById(COVER_IMAGE_ID);
		File coverFile = new File(oepbsFolder.getAbsolutePath() + File.separator + "cover-page.xhtml");
		if (!coverFile.exists()) {
			try {
				FileWriter fw = new FileWriter(coverFile);
				fw.append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n");
				fw.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
				fw.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
				fw.append("  <head>\n");
				fw.append("    <title>${title}</title>\n");
				fw.append("    <style type=\"text/css\"> img { max-width: 100%; }</style>\n");
				fw.append("  </head>\n");
				fw.append("  <body>\n");
				fw.append("    <div id=\"cover-image\">\n");
				fw.append("      <img src=\"" + coverImage.getHref() + "\" alt=\"" + coverImage.getTitle() + "\"/>\n");
				fw.append("    </div>\n");
				fw.append("  </body>\n");
				fw.append("</html>\n");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Add the cover page item
		Item coverPage = addItem(null, null, coverFile, null, XHTML_MIMETYPE, true, false);
		coverPage.setGenerated(true);
		addReference(coverPage.getHref(), coverImage.getTitle(), Type.COVER);

	}

	/**
	 * Writes the <b>content.opf</b> file.
	 * 
	 * @param oebpsFolder
	 *            the folder where to write the file.
	 * @throws IOException
	 */
	private void writeOPF(File oebpsFolder) throws IOException {
		File opfFile = new File(oebpsFolder.getAbsolutePath() + File.separator + "content.opf");
		ResourceSet resourceSet = new ResourceSetImpl();
		// Register the packages to make it available during loading.
		URI fileURI = URI.createFileURI(opfFile.getAbsolutePath());
		Resource resource = resourceSet.createResource(fileURI);
		resource.getContents().add(opfPackage);
		resource.save(null);
	}

	/**
	 * Reads the <b>content.opf</b> file.
	 * 
	 * @param opfFile
	 *            the file to read.
	 * @throws IOException
	 */
	private void readOPF(File opfFile) throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(opfFile.getAbsolutePath());
		Resource resource = resourceSet.createResource(fileURI);
		resource.load(null);
		opfPackage = (Package) resource.getContents().get(0);
	}

	/**
	 * Implement to handle writing of the table of contents. Note that this
	 * method should do nothing if the table of contents has already been
	 * specified using {@link #setTableOfContents(File)}.
	 * 
	 * @param oepbsFolder
	 *            the folder to write in
	 * @throws Exception
	 */
	protected abstract void writeTableOfContents(File oepbsFolder) throws Exception;

	public Package getOpfPackage() {
		return opfPackage;
	}
}
