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
package org.eclipse.mylyn.docs.epub.wikitext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Item;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder.Stylesheet;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.util.XmlStreamWriter;

/**
 * This type can be used to create an EBUB directly from WikiText markup.
 * 
 * @author Torkild U. Resheim
 * @since 1.0
 */
public class MarkupToEPUB {

	private String bookTitle;

	private MarkupLanguage markupLanguage;

	public MarkupLanguage getMarkupLanguage() {
		return markupLanguage;
	}

	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markupLanguage = markupLanguage;
	}


	/**
	 * Parses the markup file and populates the EPUB data model — but does not
	 * assemble the resulting EPUB file. This is left to the consumer which may
	 * also do further manipulation of the data model.
	 * 
	 * @param markup
	 *            the WikiText markup file
	 * @return the EPUB instance
	 * @throws Exception
	 */
	public EPUB parse(File markup) throws Exception {
		EPUB epub = EPUB.getVersion2Instance();
		parse(epub, markup);
		return epub;

	}

	public void parse(EPUB epub, File markupFile) throws IOException, FileNotFoundException {
		if (markupLanguage == null) {
			throw new IllegalStateException("must set markupLanguage"); //$NON-NLS-1$
		}
		// Create a temporary working folder
		File workingFolder = File.createTempFile("wikitext_", null);
		if (workingFolder.delete() && workingFolder.mkdirs()) {
			File htmlFile = new File(workingFolder.getAbsolutePath() + File.separator + "markup.html");
			FileWriter out = new FileWriter(htmlFile);
			HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out) {
				@Override
				protected XmlStreamWriter createXmlStreamWriter(Writer out) {
					return super.createFormattingXmlStreamWriter(out);
				}
			};

			List<Item> stylesheets = epub.getItemsByMIMEType(EPUB.MIMETYPE_CSS);
			for (Item item : stylesheets) {
				File file = new File(item.getFile());
				Stylesheet css = new Stylesheet(file);
				builder.addCssStylesheet(css);
				System.out.println(item.getFile());
			}

			builder.setXhtmlStrict(true);

			MarkupParser markupParser = new MarkupParser();

			markupParser.setBuilder(builder);
			markupParser.setMarkupLanguage(markupLanguage);
			markupParser.parse(new FileReader(markupFile));
			// Convert the generated HTML to EPUB
			epub.setGenerateToc(true);
			epub.setIncludeReferencedResources(true);
			Item item = epub.addItem(htmlFile);
			item.setSourcePath(markupFile.getAbsolutePath());
		}
		workingFolder.deleteOnExit();
	}

	public void assemble(File markupFile, File epubFile) throws Exception {
		EPUB epub = parse(markupFile);
		epub.pack(epubFile);
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}

}
