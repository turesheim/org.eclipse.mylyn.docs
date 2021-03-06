/*******************************************************************************
 * Copyright (c) 2007, 2010 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.textile.internal.block;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.parser.Attributes;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.parser.markup.Block;
import org.eclipse.mylyn.wikitext.textile.internal.Textile;

/**
 * Preformatted text block, matches blocks that start with <code>pre. </code>
 * 
 * @author David Green
 */
public class PreformattedBlock extends Block {

	private static final int LINE_REMAINDER_GROUP_OFFSET = Textile.ATTRIBUTES_BLOCK_GROUP_COUNT + 2;

	private static final int EXTENDED_GROUP = Textile.ATTRIBUTES_BLOCK_GROUP_COUNT + 1;

	static final Pattern startPattern = Pattern.compile("pre" + Textile.REGEX_BLOCK_ATTRIBUTES + "\\.(\\.)?\\s+(.*)"); //$NON-NLS-1$ //$NON-NLS-2$

	private boolean extended;

	private int blockLineCount = 0;

	private Matcher matcher;

	public PreformattedBlock() {
	}

	@Override
	public int processLineContent(String line, int offset) {
		if (blockLineCount == 0) {
			Attributes attributes = new Attributes();

			Textile.configureAttributes(attributes, matcher, 1, true);
			offset = matcher.start(LINE_REMAINDER_GROUP_OFFSET);
			extended = matcher.group(EXTENDED_GROUP) != null;

			builder.beginBlock(BlockType.PREFORMATTED, attributes);
		}
		if (markupLanguage.isEmptyLine(line) && !extended) {
			setClosed(true);
			return 0;
		} else if (extended && Textile.explicitBlockBegins(line, offset)) {
			setClosed(true);
			return offset;
		}
		++blockLineCount;

		final String lineText = offset > 0 ? line.substring(offset) : line;
		if (blockLineCount > 1 || lineText.trim().length() > 0) {
			builder.characters(lineText);
			builder.characters("\n"); //$NON-NLS-1$
		}

		return -1;
	}

	@Override
	public boolean canStart(String line, int lineOffset) {
		blockLineCount = 0;
		if (lineOffset == 0) {
			matcher = startPattern.matcher(line);
			return matcher.matches();
		} else {
			matcher = null;
			return false;
		}
	}

	@Override
	public void setClosed(boolean closed) {
		if (closed && !isClosed()) {
			builder.endBlock();
		}
		super.setClosed(closed);
	}

}
