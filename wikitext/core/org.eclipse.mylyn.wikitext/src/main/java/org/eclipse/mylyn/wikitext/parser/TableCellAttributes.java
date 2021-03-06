/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.parser;

import org.eclipse.mylyn.wikitext.parser.DocumentBuilder.BlockType;

/**
 * Attributes that may used when creating blocks of type {@link BlockType#TABLE_CELL_HEADER} or
 * {@link BlockType#TABLE_CELL_NORMAL}.
 *
 * @author David Green
 * @since 3.0
 */
public class TableCellAttributes extends Attributes {
	private String bgcolor;

	private String align;

	private String valign;

	private String rowspan;

	private String colspan;

	public String getBgcolor() {
		return bgcolor;
	}

	public void setBgcolor(String bgcolor) {
		this.bgcolor = bgcolor;
	}

	/**
	 * Horizontal alignment attribute.
	 *
	 * @param valign
	 *            typical value is "left", "right", "center"
	 */
	public String getAlign() {
		return align;
	}

	/**
	 * Horizontal alignment attribute.
	 *
	 * @param valign
	 *            typical value is "left", "right", "center"
	 */
	public void setAlign(String align) {
		this.align = align;
	}

	/**
	 * Vertical alignment attribute.
	 *
	 * @param valign
	 *            typical value is "top", "middle", "bottom"
	 */
	public String getValign() {
		return valign;
	}

	/**
	 * Vertical alignment attribute.
	 *
	 * @param valign
	 *            typical value is "top", "middle", "bottom"
	 */
	public void setValign(String valign) {
		this.valign = valign;
	}

	public String getRowspan() {
		return rowspan;
	}

	public void setRowspan(String rowspan) {
		this.rowspan = rowspan;
	}

	public String getColspan() {
		return colspan;
	}

	public void setColspan(String colspan) {
		this.colspan = colspan;
	}

}
