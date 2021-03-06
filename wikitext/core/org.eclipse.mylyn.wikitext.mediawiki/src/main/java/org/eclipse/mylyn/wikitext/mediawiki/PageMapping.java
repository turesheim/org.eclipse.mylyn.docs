/*******************************************************************************
 * Copyright (c) 2010, 2015 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.mediawiki;

/**
 * a means of mapping page names to relative or absolute resources.
 *
 * @author David Green
 * @since 3.0
 */
public interface PageMapping {
	/**
	 * provide a relative mapping for the given page name.
	 *
	 * @param pageName
	 *            the name of the page, for example "Mylyn/User_Guide", ":Category:Mylyn", "#Some_link?" or "Main
	 *            Page#help".
	 * @return a relative or absolute URL, or null if no mapping is available or if the default mapping should apply.
	 */
	String mapPageNameToHref(String pageName);

}
