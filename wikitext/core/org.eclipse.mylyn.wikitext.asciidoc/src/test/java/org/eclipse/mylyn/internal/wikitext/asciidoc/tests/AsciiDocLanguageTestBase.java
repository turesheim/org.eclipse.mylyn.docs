/*******************************************************************************
 * Copyright (c) 2015, 2016 Max Rydahl Andersen and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Max Rydahl Andersen - copied from markdown to get base for asciidoc, Bug 474084
 *******************************************************************************/

package org.eclipse.mylyn.internal.wikitext.asciidoc.tests;

import java.io.StringWriter;

import org.eclipse.mylyn.wikitext.asciidoc.AsciiDocLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguageConfiguration;
import org.junit.Before;

/**
 * Test base for asciidoc language tests. provides base set-up functionalities, like parsing markup to html
 *
 * @author Max Rydahl Andersen
 */
public abstract class AsciiDocLanguageTestBase {

	private MarkupParser parser;

	@Before
	public void setUp() throws Exception {
		parser = createParserWithConfiguration(null);
	}

	public String parseToHtml(String markup) {
		return parseAsciiDocToHtml(markup, parser);
	}

	protected static MarkupParser createParserWithConfiguration(MarkupLanguageConfiguration configuration) {
		MarkupLanguage markupLanguage = new AsciiDocLanguage();
		if (configuration != null) {
			markupLanguage.configure(configuration);
		}
		return new MarkupParser(markupLanguage);
	}

	protected static String parseAsciiDocToHtml(String markup, MarkupParser parser) {
		StringWriter out = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out);
		builder.setEmitAsDocument(false);
		parser.setBuilder(builder);
		parser.parse(markup);
		return out.toString();
	}

}
