/*******************************************************************************
 * Copyright (c) 2011 Torkild U. Resheim.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Torkild U. Resheim - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.docs.epub.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.mylyn.docs.epub.opf.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This type is a SAX parser that will read a XHTML file and create a list of
 * all images that referenced.
 * 
 * @author Torkild U. Resheim
 */
public class ImageScanner extends DefaultHandler {

	ArrayList<File> files;

	public ImageScanner(Item item) {
		super();
		files = new ArrayList<File>();
		currentItem = item;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
	}

	/**
	 * Case-insensitive method for obtaining an attribute.
	 * 
	 * @param attributes
	 *            SAX attributes
	 * @param name
	 *            the attribute name
	 * @return the attribute value or <code>null</code>
	 */
	private String getAttribute(Attributes attributes, String name) {
		for (int i = 0; i < attributes.getLength(); i++) {
			String aname = attributes.getQName(i);
			if (aname.equalsIgnoreCase(name)) {
				return attributes.getValue(i);
			}
		}
		return null;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("img")) {
			String ref = getAttribute(attributes, "src");
			if (ref != null) {
				File refPath = new File(ref);
				if (refPath.isAbsolute()) {
					files.add(refPath);
				} else {
					File source = new File(currentItem.getSourcePath());
					File file = new File(source.getParentFile()
							.getAbsolutePath() + File.separator + ref);
					files.add(file);
				}
			}
		}
	}

	Item currentItem;

	public static List<File> parse(Item item)
			throws ParserConfigurationException, SAXException, IOException {
		FileReader fr = new FileReader(item.getFile());
		InputSource file = new InputSource(fr);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				false);
		SAXParser parser = factory.newSAXParser();
		String href = item.getHref();
		ImageScanner scanner = new ImageScanner(item);
		try {
			parser.parse(file, scanner);
			return scanner.files;
		} catch (SAXException e) {
			System.err.println("Could not parse " + href);
			e.printStackTrace();
		}
		return null;
	}

}