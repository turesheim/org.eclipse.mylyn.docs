/*******************************************************************************
 * Copyright (c) 2015 David Green.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.commonmark.internal.inlines;

import org.eclipse.mylyn.wikitext.commonmark.internal.Line;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;

public class HtmlTag extends Inline {

	private final String content;

	public HtmlTag(Line line, int offset, String content) {
		super(line, offset, content.length());
		this.content = content;
	}

	@Override
	public void emit(DocumentBuilder builder) {
		builder.charactersUnescaped(content);
	}

}
