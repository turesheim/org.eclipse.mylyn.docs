/*******************************************************************************
 * Copyright (c) 2013, 2014 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.html.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import org.eclipse.mylyn.wikitext.parser.Attributes;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder.SpanType;

class SubstitutionSpanStrategy implements SpanStrategy {

	private final SpanType type;

	protected SubstitutionSpanStrategy(SpanType type) {
		this.type = checkNotNull(type);
	}

	@Override
	public void beginSpan(DocumentBuilder builder, SpanType unsupportedType, Attributes attributes) {
		builder.beginSpan(type, attributes);
	}

	@Override
	public void endSpan(DocumentBuilder builder) {
		builder.endSpan();
	}

	SpanType getType() {
		return type;
	}
}
