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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.opf.Item;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
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
	 * @param file
	 *            the resulting EPUB file
	 * @return the EPUB instance
	 * @throws Exception
	 */
	public EPUB parse(File markup, File file) throws Exception {
		if (markupLanguage == null) {
			throw new IllegalStateException("must set markupLanguage"); //$NON-NLS-1$
		}
		EPUB epub = EPUB.getVersion2Instance();
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
			builder.setXhtmlStrict(true);
			MarkupParser markupParser = new MarkupParser();

			markupParser.setBuilder(builder);
			markupParser.setMarkupLanguage(markupLanguage);
			markupParser.parse(new FileReader(markup));
			// Convert the generated HTML to EPUB
			epub.setGenerateToc(true);
			epub.setIncludeReferencedResources(true);
			Item item = epub.addItem(htmlFile);
			item.setSourcePath(markup.getAbsolutePath());
			epub.setFile(file);
		}
		workingFolder.deleteOnExit();
		return epub;

	}

	public void parseAndAssemble(File markup, File file) throws Exception {
		EPUB epub = parse(markup, file);
		epub.assemble();
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}

}
