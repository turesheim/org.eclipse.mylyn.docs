/*******************************************************************************
 * Copyright (c) 2015 Torkild U. Resheim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Torkild U. Resheim - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.docs.epub.core;

import org.eclipse.mylyn.docs.epub.opf.OPFFactory;

/**
 * This type represents one EPUB revision 3.0 formatted publication. It inherits from the EPUB revision 2.0.1
 * implementation since this will allow for forward compatible publications to be produced.
 *
 * @author Torkild U. Resheim
 * @since 3.0
 * @see http://www.idpf.org/epub/301/spec/epub-publications.html
 */
public class EPUBPublication extends OPSPublication {

	@Override
	protected String getVersion() {
		return "3.0"; //$NON-NLS-1$
	}

	/**
	 * Creates a new EPUB 3.0 formatted publication.
	 *
	 * @since 2.0
	 */
	public EPUBPublication() {
		super();
	}

	/**
	 * Creates a new EPUB 2.0 formatted publication, logging all events to the specified logger.
	 *
	 * @since 3.3
	 */
	public EPUBPublication(ILogger logger) {
		super(logger);
	}

	/**
	 * Adds a new EPUB 3 meta item to the publication.
	 *
	 * @param id
	 *            optional identifier
	 * @param property
	 *            a required property
	 * @param refines
	 *            optional expression or resource augmented with the element
	 * @param scheme
	 *            the optional property data type
	 * @return the new meta
	 * @see http://www.idpf.org/epub/30/spec/epub30-publications.html#sec-meta-elem
	 * @see OPSPublication#addMeta(String, String)
	 * @since 3.0
	 */
	public org.eclipse.mylyn.docs.epub.opf.Meta addMeta(String id, String property, String refines, String scheme) {
		if (property == null) {
			throw new IllegalArgumentException("A property must be specified"); //$NON-NLS-1$
		}
		org.eclipse.mylyn.docs.epub.opf.Meta opf = OPFFactory.eINSTANCE.createMeta();
		opf.setId(id);
		opf.setProperty(property);
		opf.setRefines(refines);
		opf.setScheme(scheme);
		opfPackage.getMetadata().getMetas().add(opf);
		return opf;
	}
}
