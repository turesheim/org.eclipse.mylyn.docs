package org.eclipse.mylyn.docs.epub.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import junit.framework.Assert;

import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.adobe.epubcheck.api.EpubCheck;

public class TestEPUB {

	private static final String EPUB_FILE_PATH = "/Users/torkild/Temp/test.epub";
	private final static String PARAGRAPH = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus consectetur diam quis lacus venenatis et elementum est scelerisque. Fusce tincidunt semper lacinia. Nulla aliquet, libero et iaculis volutpat, diam massa dapibus nisi, id eleifend lectus sapien id lacus. Aliquam volutpat malesuada tortor, ac fringilla justo luctus eu. Donec dolor orci, auctor sit amet faucibus nec, blandit ut magna. Suspendisse volutpat aliquet nunc id dictum. Nam lectus libero, blandit in condimentum vitae, ultrices quis massa. Donec mi lorem, elementum id imperdiet sit amet, auctor et diam. Sed gravida adipiscing tortor, eget mollis arcu commodo et. Curabitur dapibus lorem sit amet lectus faucibus lobortis.";

	@Before
	public void setUp() throws IOException {
		File file = new File(EPUB_FILE_PATH);
		file.delete();
		EPUB epub = new EPUB();
		epub.setFile(EPUB_FILE_PATH);
		epub.setIdentifierId("uuid");
		epub.addTitle("Eclipse Development Conventions and Guidelines", null);
		epub.addCreator("Eclipse Committers and Contributors", Role.AUTHOR,
				null, null);
		epub.addDescription(PARAGRAPH, null);
		epub.addPublisher("Eclipse.Org", null);
		epub.addSubject("My first subject", null);
		epub.addSubject("My second subject", null);
		epub.addItem(new File("testdata/style.css"), null, "text/css", false);
		epub.addItem(new File(
				"testdata/Development_Conventions_And_Guidelines.html"), null,
				null, true);
		epub.addItem(new File("testdata/Naming_Conventions.html"), null, null,
				true);
		epub.addItem(new File("testdata/Coding_Conventions.html"), null, null,
				true);
		epub.addItem(new File("testdata/Javadoc.html"), null, null, true);
		epub.addItem(new File("testdata/User_Interface_Guidelines.html"), null,
				null, true);
		epub.addItem(new File("testdata/Version_Numbering.html"), null, null,
				true);
		epub.addItem(new File("testdata/Plugin-versioning-fig1.jpg"), null,
				"image/jpeg", false);
		epub.addItem(new File("testdata/Cover.jpg"), "cover-image",
				"image/jpeg", false);
		epub.addItem(new File("testdata/Cover.html"), null, null, false);
		epub.addReference("Cover.html", "Cover", "cover");
		epub.addIdentifier("uuid", Scheme.UUID, UUID.randomUUID().toString());
		epub.assemble();
		// epub.saveOPF(opfFile);
	}

	@After
	public void tearDown() {

	}

	@Test
	public void testContents() throws FileNotFoundException, IOException {
		// BufferedReader reader = new BufferedReader(new FileReader(opfFile));
		// String in = null;
		// while ((in = reader.readLine()) != null) {
		// System.out.println(in);
		// }
	}

	@Test
	public void testEPUB() {
		EpubCheck checker = new EpubCheck(new File(EPUB_FILE_PATH));
		Assert.assertTrue(checker.validate());
	}

}
