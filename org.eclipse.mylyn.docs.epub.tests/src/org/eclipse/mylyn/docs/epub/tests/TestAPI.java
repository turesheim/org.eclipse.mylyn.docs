package org.eclipse.mylyn.docs.epub.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.mylyn.docs.epub.EPUB2;
import org.eclipse.mylyn.docs.epub.opf.Role;
import org.eclipse.mylyn.docs.epub.opf.Scheme;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class TestAPI {
	private File getFile(String path) throws URISyntaxException {
		return new File(path);

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
	public void setUp() throws IOException, SAXException,
			ParserConfigurationException, URISyntaxException {
		File workingFolder = getFile("test/api/work");
		delete(workingFolder);
		File file = getFile("test/api/alice-in-wonderland.epub");
		file.delete();
		EPUB2 epub = new EPUB2();
		epub.setFile(file.getAbsolutePath());
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
		epub.addIdentifier("uuid", Scheme.UUID, UUID.randomUUID().toString());
		epub.setGenerateToc(true);
		epub.assemble(workingFolder);
	}

	@After
	public void tearDown() {

	}

	@Test
	public void testEPUB() {
		// EpubCheck checker = new EpubCheck(new File(EPUB_FILE_PATH));
		// Assert.assertTrue(checker.validate());
	}

}
