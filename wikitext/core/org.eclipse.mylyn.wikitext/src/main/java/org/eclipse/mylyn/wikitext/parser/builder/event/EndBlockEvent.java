/*******************************************************************************
 * Copyright (c) 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.parser.builder.event;

import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;

import com.google.common.base.Objects;

/**
 * An {@link DocumentBuilderEvent} corresponding to {@link DocumentBuilder#endBlock()}.
 *
 * @author david.green
 * @since 3.0
 */
public class EndBlockEvent extends DocumentBuilderEvent {
	@Override
	public void invoke(DocumentBuilder builder) {
		builder.endBlock();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(EndBlockEvent.class);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EndBlockEvent)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "endBlock()"; //$NON-NLS-1$
	}
}
