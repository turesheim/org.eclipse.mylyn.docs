package org.eclipse.mylyn.docs.epub.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestEPUB {

	private final static String PARAGRAPH = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus consectetur diam quis lacus venenatis et elementum est scelerisque. Fusce tincidunt semper lacinia. Nulla aliquet, libero et iaculis volutpat, diam massa dapibus nisi, id eleifend lectus sapien id lacus. Aliquam volutpat malesuada tortor, ac fringilla justo luctus eu. Donec dolor orci, auctor sit amet faucibus nec, blandit ut magna. Suspendisse volutpat aliquet nunc id dictum. Nam lectus libero, blandit in condimentum vitae, ultrices quis massa. Donec mi lorem, elementum id imperdiet sit amet, auctor et diam. Sed gravida adipiscing tortor, eget mollis arcu commodo et. Curabitur dapibus lorem sit amet lectus faucibus lobortis.";

	@Before
	public void setUp() throws IOException {
		EPUB epub = new EPUB("/tmp/epub/test.epub");
		epub.addTitle("My Title", null);
		// epub.addTitle("My Second Title");
		epub.addCreator("My Name", Role.AUTHOR, null, null);
		epub.addDescription(PARAGRAPH, null);
		epub.addPublisher("Eclipse.Org", null);
		epub.addSubject("My first subject", null);
		epub.addSubject("My second subject", null);
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

	// @Test
	// public void testEPUB() {
	// EpubCheck checker = new EpubCheck(opfFile);
	// Assert.assertTrue(checker.validate());
	// }

}
