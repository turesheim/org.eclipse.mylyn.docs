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
package org.eclipse.mylyn.docs.epub.core.wikitext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.eclipse.mylyn.docs.epub.core.EPUB;
import org.eclipse.mylyn.docs.epub.core.OPSPublication;
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

	private MarkupLanguage markupLanguage;

	/**
	 * Delete the folder recursively.
	 * 
	 * @param folder
	 *            the folder to delete
	 * @return <code>true</code> if the folder was deleted
	 */
	private boolean deleteFolder(File folder) {
		if (folder.isDirectory()) {
			String[] children = folder.list();
			for (int i = 0; i < children.length; i++) {
				boolean ok = deleteFolder(new File(folder, children[i]));
				if (!ok) {
					return false;
				}
			}
		}
		return folder.delete();
	}

	/**
	 * Parses the markup file and populates the OPS dataâ but does not assemble
	 * the resulting EPUB file. This is left to the consumer which may also do
	 * further manipulation of the data.
	 * 
	 * @param markup
	 *            the WikiText markup file
	 * 
	 * @return the EPUB instance
	 * @throws Exception
	 */
	public OPSPublication generate(File markup) throws Exception {
		OPSPublication epub = OPSPublication.getVersion2Instance();
		parse(epub, markup);
		return epub;

	}

	/**
	 * Converts the markup code to HTML then places it with the EPUB and
	 * assembles the publication.
	 * 
	 * @param markupFile
	 *            the markup file
	 * @param epubFile
	 *            the new EPUB file
	 * @return the EPUB file
	 * @throws Exception
	 */
	public EPUB generate(File markupFile, File epubFile) throws Exception {
		OPSPublication ops = generate(markupFile);
		EPUB epub = new EPUB();
		epub.add(ops);
		epub.pack(epubFile);
		return epub;
	}

	/**
	 * Parses the markup file and populates the OPS data.
	 * 
	 * @param epub
	 *            the new EPUB file
	 * @param the
	 *            WikiText markup file
	 */
	public void parse(OPSPublication epub, File markupFile) throws IOException, FileNotFoundException {
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

			List<Item> stylesheets = epub.getItemsByMIMEType(OPSPublication.MIMETYPE_CSS);
			for (Item item : stylesheets) {
				File file = new File(item.getFile());
				Stylesheet css = new Stylesheet(file);
				builder.addCssStylesheet(css);
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
		deleteFolder(workingFolder);
	}

	/**
	 * Sets the markup language to use when generating HTML from markup.
	 * 
	 * @param markupLanguage
	 *            the markup language
	 */
	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markupLanguage = markupLanguage;
	}

}
