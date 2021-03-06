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

package org.eclipse.mylyn.wikitext.asciidoc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;

import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguageConfiguration;

import com.google.common.collect.ImmutableMap;

/**
 * Extended configuration for the AsciiDoc markup language
 *
 * @author Jeremie Bresson
 * @since 3.0.0
 */
public class AsciiDocMarkupLanguageConfiguration extends MarkupLanguageConfiguration {

	private Map<String, String> initialAttributes = Collections.emptyMap();

	/**
	 * @since 3.0.0
	 * @return initial attributes (key, values)
	 */
	public Map<String, String> getInitialAttributes() {
		return initialAttributes;
	}

	/**
	 * @since 3.0.0
	 * @param initialAttributes
	 *            initial attributes (key, values)
	 */
	public void setInitialAttributes(Map<String, String> initialAttributes) {
		checkNotNull(initialAttributes, "initialAttributes can not be null");
		this.initialAttributes = ImmutableMap.copyOf(initialAttributes);
	}
}
