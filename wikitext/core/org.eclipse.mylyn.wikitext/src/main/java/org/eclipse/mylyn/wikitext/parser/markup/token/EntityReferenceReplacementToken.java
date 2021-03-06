/*******************************************************************************
 * Copyright (c) 2007, 2011 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.parser.markup.token;

import java.util.regex.Pattern;

/**
 * @author David Green
 * @since 3.0
 */
public class EntityReferenceReplacementToken extends PatternEntityReferenceReplacementToken {

	public EntityReferenceReplacementToken(String token, String replacement) {
		super("(" + Pattern.quote(token) + ")", replacement); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
