package org.eclipse.mylyn.docs.epub.tests;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.eclipse.mylyn.docs.epub.EPUB2;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.adobe.epubcheck.api.EpubCheck;

public class TestEPUB {

	private static final String EPUB_FILE_PATH = "/Users/torkild/Temp/test.epub";
	private final static String PARAGRAPH = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus consectetur diam quis lacus venenatis et elementum est scelerisque. Fusce tincidunt semper lacinia. Nulla aliquet, libero et iaculis volutpat, diam massa dapibus nisi, id eleifend lectus sapien id lacus. Aliquam volutpat malesuada tortor, ac fringilla justo luctus eu. Donec dolor orci, auctor sit amet faucibus nec, blandit ut magna. Suspendisse volutpat aliquet nunc id dictum. Nam lectus libero, blandit in condimentum vitae, ultrices quis massa. Donec mi lorem, elementum id imperdiet sit amet, auctor et diam. Sed gravida adipiscing tortor, eget mollis arcu commodo et. Curabitur dapibus lorem sit amet lectus faucibus lobortis.";

	@Before
	public void setUp() throws IOException, SAXException,
			ParserConfigurationException {
		File file = new File(EPUB_FILE_PATH);
		file.delete();
		EPUB2 epub = new EPUB2();
		epub.setFile(EPUB_FILE_PATH);
		epub.setIdentifierId("uuid");
		epub.addLanguage("en");
		epub.addTitle("New Committer Handbook", null);
		epub.addCreator("Eclipse Committers and Contributors", Role.AUTHOR,
				null, null);
		epub.addDescription(PARAGRAPH, null);
		epub.addPublisher("Eclipse.Org", null);
		epub.addSubject("My first subject", null);
		epub.addSubject("My second subject", null);
		epub.addItem(new File("testdata/style.css"), null, "text/css", false);
		epub.addItem(new File("test/src/New_Committer_Handbook.html"), null, null,
				true);
		epub.addItem(new File("test/src/Eclipse_Quality.html"), null, null,
				true);
		epub.addIdentifier("uuid", Scheme.UUID, UUID.randomUUID().toString());
		epub.setGenerateToc(true);
		epub.assemble();
	}

	@After
	public void tearDown() {

	}

	@Test
	public void testEPUB() {
		EpubCheck checker = new EpubCheck(new File(EPUB_FILE_PATH));
		Assert.assertTrue(checker.validate());
	}

}
