package org.eclipse.mylyn.docs.epub.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.eclipse.mylyn.docs.epub.EPUB2;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.eclipse.mylyn.docs.epub.opf.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.epubcheck.api.EpubCheck;

public class TestAPI {
	private EPUB2 epub;
	private File workingFolder;
	private File epubFile;

	private File getFile(String path) throws URISyntaxException {
		return new File(path);
	}

	private boolean fileExists(String filename) {
		String path = workingFolder.getAbsolutePath() + File.separator
				+ "OEBPS" + File.separator + filename;
		File file = new File(path);
		return file.exists();
	}

	void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}

		if (f.exists() && !f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}

	@Before
	public void setUp() throws Exception {
		workingFolder = getFile("test/api/work");
		delete(workingFolder);
		epubFile = getFile("test/api/alice-in-wonderland.epub");
		epubFile.delete();
		epub = new EPUB2();
		epub.setFile(epubFile.getAbsolutePath());
	}

	@After
	public void tearDown() {

	}

	private Element readOPF() throws ParserConfigurationException,
			SAXException, IOException {
		File fXmlFile = new File(workingFolder.getAbsolutePath()
				+ File.separator + "OEBPS" + File.separator + "content.opf");
		Assert.assertEquals(true, fXmlFile.exists());
		System.out.println(fXmlFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		return doc.getDocumentElement();
	}

	/**
	 * Checks the contents of the OPF when nothing has been added to the
	 * publication.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSerializationEmpty() throws Exception {
		// epub.addReference("title.xhtml", "Cover Page", Type.COVER);
		epub.assemble(workingFolder);
		Element doc = readOPF();
		Assert.assertEquals("opf:package", doc.getNodeName());
		NodeList nl = doc.getChildNodes();
		// Text nodes in between
		Assert.assertEquals("opf:metadata", nl.item(1).getNodeName());
		Assert.assertEquals("opf:manifest", nl.item(3).getNodeName());
		Assert.assertEquals("opf:spine", nl.item(5).getNodeName());
		Assert.assertEquals("opf:guide", nl.item(7).getNodeName());

		// Table of contents
		Node toc = nl.item(3).getFirstChild().getNextSibling();
		Assert.assertEquals("opf:item", toc.getNodeName());
		Assert.assertEquals("ncx", toc.getAttributes().getNamedItem("id")
				.getNodeValue());
		Assert.assertEquals("toc.ncx", toc.getAttributes().getNamedItem("href")
				.getNodeValue());
		Assert.assertEquals("application/x-dtbncx+xml", toc.getAttributes()
				.getNamedItem("media-type")
				.getNodeValue());
		Assert.assertEquals(true, fileExists("toc.ncx"));
		Node spine = nl.item(5);
		Assert.assertEquals("ncx", spine.getAttributes().getNamedItem("toc")
				.getNodeValue());
		doc = null;
	}

	/**
	 * See if all reference types are serialised properly.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSerializationReferences() throws Exception {
		Type[] types = Type.values();
		for (Type type : types) {
			epub.addReference(type.getLiteral() + ".xhtml", type.getName(),
					type);
		}
		epub.assemble(workingFolder);
		Element doc = readOPF();
		Node guide = doc.getElementsByTagName("opf:guide").item(0);
		Node ref = guide.getFirstChild(); // Discard first TEXT node
		for (Type type : types) {
			ref = ref.getNextSibling();
			// The should be exactly three attributes
			Assert.assertEquals(
					"Wrong number of attributes in '" + type.getLiteral() + "'",
					3,
					ref.getAttributes().getLength());
			Assert.assertEquals(type.getName(), ref.getAttributes()
					.getNamedItem("title").getNodeValue());
			Assert.assertEquals(type.getLiteral() + ".xhtml", ref
					.getAttributes().getNamedItem("href").getNodeValue());
			Assert.assertEquals(type.getLiteral(), ref.getAttributes()
					.getNamedItem("type").getNodeValue());
			ref = ref.getNextSibling();
			doc = null;
		}

	}



	// @Test
	public void testEPUB() throws Exception {
		epub.setIdentifierId("uuid");
		epub.addLanguage(null, "en");
		epub.addDate(null, "1916", "original-publication");
		epub.addTitle(null, null, "Alice in Wonderland");
		epub.addSubject(null, null, "Young Readers");
		epub.addSubject(null, null, "Fantasy");
		epub.setSource(null, null, "Project Gutenberg");
		epub.addCreator(null, null, "Lewis Carroll", Role.AUTHOR,
				"Carroll, Lewis");
		epub.addItem(getFile("alice-in-wonderland/chapter-001.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-002.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-003.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-004.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-005.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-006.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-007.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-008.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-009.xhtml"));
		epub.addItem(getFile("alice-in-wonderland/chapter-010.xhtml"));
		epub.addReference("title.xhtml", "Cover Page", Type.COVER);
		epub.addIdentifier("uuid", Scheme.UUID, UUID.randomUUID().toString());
		epub.setGenerateToc(true);
		epub.assemble(workingFolder);
		EpubCheck checker = new EpubCheck(epubFile);
		Assert.assertTrue(checker.validate());
	}

}
