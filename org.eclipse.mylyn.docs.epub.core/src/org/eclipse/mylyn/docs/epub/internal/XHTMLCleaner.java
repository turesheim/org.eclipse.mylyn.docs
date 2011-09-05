package org.eclipse.mylyn.docs.epub.internal;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.mylyn.docs.epub.ncx.Ncx;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This type is a SAX parser that will read a XHTML file and produce a new
 * version where elements and attributes not in the EPUB 2.0.1 preferred
 * vocabulary are stripped. This comes in handy when producing content for
 * reading systems that have limited HTML support and will fail.
 * 
 * 
 * @author Torkild U. Resheim
 */
public class XHTMLCleaner extends DefaultHandler {
	private StringBuilder buffer = null;

	private static StringBuilder contents = null;

	private boolean recording = false;

	public XHTMLCleaner(String href, Ncx ncx, int playOrder) {
		super();
		buffer = new StringBuilder();
		contents = new StringBuilder();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (recording) {
			buffer.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (isLegalElement(qName)) {
			if (buffer.length() == 0) {
				contents.append("/>");
			} else {
				contents.append(">");
				contents.append(buffer);
				contents.append("</" + qName + ">");
				buffer.setLength(0);
			}
		}
		recording = false;
	}

	/**
	 * A list of legal elements according to the EPUB 2.0.1 specification
	 * 
	 * @see http://idpf.org/epub/20/spec/OPS_2.0.1_draft.htm#Section1.3.4
	 * @see http://idpf.org/epub/20/spec/OPS_2.0.1_draft.htm#Section2.2
	 */
	private final String[] legalElements = new String[] { "body", "head", "html", "title", "abbr", "acronym",
			"address", "blockquote", "br", "cite", "code", "dfn", "div", "em", "h1", "h2", "h3", "h4", "h5", "h6",
			"kbd", "p", "pre", "q", " samp", "span", "strong", "var", "a", "dl", "dt", "dd", "ol", "ul", "li",
			"object", "param", "b", "big", "hr", "i", "small", "sub", "sup", "tt", "del", "ins", "bdo", "caption",
			"col", "colgroup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "img", "area", "map", "meta",
			"style", "link", "base" };

	private final String[] legalAttributes = new String[] { "accesskey", "charset", "class", "coords", "dir", "href",
			"hreflang", "id", "rel", "rev", "shape", "style", "tabindex", "target", "title", "type", "xml:lang" };

	private boolean isLegalElement(String name) {
		for (String legal : legalElements) {
			if (name.equalsIgnoreCase(legal)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the given attribute name is legal.
	 * 
	 * @param name
	 * @return
	 */
	private boolean isLegalAttribute(String name) {
		for (String legal : legalAttributes) {
			if (name.equalsIgnoreCase(legal)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (isLegalElement(qName)) {
			// Record any text content
			contents.append("<" + qName);
			for (int i = 0; i < attributes.getLength(); i++) {
				if (isLegalAttribute(attributes.getQName(i))) {
					contents.append(' ');
					contents.append(attributes.getQName(i));
					contents.append("=\"");
					contents.append(attributes.getValue(i));
					contents.append("\"");
				}
			}
			recording = true;
		}
		if (!isLegalElement(qName)) {
			System.err.println(qName);
		}
	}

	public static void parse(InputSource file, String href, Ncx ncx, int playOrder)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		SAXParser parser = factory.newSAXParser();
		XHTMLCleaner tocGenerator = new XHTMLCleaner(href, ncx, playOrder);
		try {
			parser.parse(file, tocGenerator);
			System.out.println(contents);
		} catch (SAXException e) {
			System.err.println("Could nto parse " + href);
			e.printStackTrace();
		}
	}

}