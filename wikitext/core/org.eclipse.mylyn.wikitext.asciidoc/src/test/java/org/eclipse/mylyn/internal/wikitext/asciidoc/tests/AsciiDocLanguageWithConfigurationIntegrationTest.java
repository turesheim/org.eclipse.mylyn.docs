/*******************************************************************************
 * Copyright (c) 2017 Jeremie Bresson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeremie Bresson - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.wikitext.asciidoc.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylyn.wikitext.asciidoc.AsciiDocMarkupLanguageConfiguration;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguageConfiguration;
import org.junit.Test;

public class AsciiDocLanguageWithConfigurationIntegrationTest extends AsciiDocLanguageTestBase {

	@Test
	public void parseWithEmptyConfiguration() {
		AsciiDocMarkupLanguageConfiguration configuration = new AsciiDocMarkupLanguageConfiguration();

		MarkupParser parser = createParserWithConfiguration(configuration);

		String html = parseAsciiDocToHtml(AsciiDocLanguageAttributeTest.MARKUP_FOR_DEFAULT, parser);
		AsciiDocLanguageAttributeTest.ensureDefaultValues(html);
	}

	@Test
	public void parseWithCommonMarkupLanguageConfiguration() {
		MarkupLanguageConfiguration configuration = new MarkupLanguageConfiguration();

		MarkupParser parser = createParserWithConfiguration(configuration);

		String html = parseAsciiDocToHtml(AsciiDocLanguageAttributeTest.MARKUP_FOR_DEFAULT, parser);
		AsciiDocLanguageAttributeTest.ensureDefaultValues(html);
	}

	@Test
	public void parseWithImagesdirConfiguration() {
		AsciiDocMarkupLanguageConfiguration configuration = new AsciiDocMarkupLanguageConfiguration();
		Map<String, String> initialAttributes = new HashMap<>();
		initialAttributes.put("imagesdir", "IMGS");
		configuration.setInitialAttributes(initialAttributes);

		MarkupParser parser = createParserWithConfiguration(configuration);

		String markup = "See this the {imagesdir} folder";
		String html = parseAsciiDocToHtml(markup, parser);
		assertEquals("<p>See this the IMGS folder</p>\n", html);
	}
}
