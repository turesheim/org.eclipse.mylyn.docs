package org.eclipse.mylyn.docs.epub.internal;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.mylyn.docs.epub.ncx.Content;
import org.eclipse.mylyn.docs.epub.ncx.NCXFactory;
import org.eclipse.mylyn.docs.epub.ncx.NavLabel;
import org.eclipse.mylyn.docs.epub.ncx.NavPoint;
import org.eclipse.mylyn.docs.epub.ncx.Ncx;
import org.eclipse.mylyn.docs.epub.ncx.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This type is a SAX parser that will read a XHTML file, locate headers and
 * create NCX items for the EPUB table of contents. Each header must have an
 * "id" attribute or it will not be possible to link to the header.
 * 
 * 
 * @author Torkild U. Resheim
 */
public class TOCGenerator extends DefaultHandler {
	private StringBuilder buffer = null;
	private String currentHref = null;
	private String currentId = null;
	private NavPoint[] headers = null;
	private final Ncx ncx;
	private int playOrder = 0;

	private boolean recording = false;

	public TOCGenerator(String href, Ncx ncx) {
		super();
		buffer = new StringBuilder();
		currentHref = href;
		headers = new NavPoint[6];
		this.ncx = ncx;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (recording) {
			buffer.append(ch, start, length);
		}
	}

	private int isHeader(String qName) {
		if (qName.startsWith("h") && qName.length() == 2) {
			String n = qName.substring(1);
			int i = Integer.parseInt(n);
			// Levels must be between 1 and 6
			if (i > 0 && i < 7) {
				return i;
			}
		}
		return 0;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		int level = isHeader(qName);
		if (level > 0 && recording) {
			NavPoint np = createNavPoint();
			NavPoint h = headers[level - 1];

			while (level > 1 && h == null) {
				level--;
				if (level == 1) {
					break;
				}
				h = headers[level - 1];
			}

			if (level > 1) {
				h.getNavPoint().add(np);
			} else {
				ncx.getNavMap().getNavPoint().add(np);
			}
			headers[level] = np;
		}
		buffer.setLength(0);
		recording = false;
	}

	private NavPoint createNavPoint() {
		NavPoint np = NCXFactory.eINSTANCE.createNavPoint();
		NavLabel nl = NCXFactory.eINSTANCE.createNavLabel();
		Content c = NCXFactory.eINSTANCE.createContent();
		c.setSrc(currentHref + "#" + currentId);
		Text text = NCXFactory.eINSTANCE.createText();
		FeatureMapUtil.addText(text.getMixed(), buffer.toString());
		nl.setText(text);
		np.getNavLabels().add(nl);
		np.setPlayOrder(++playOrder);
		np.setId("navpoint" + playOrder);
		np.setContent(c);
		return np;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (isHeader(qName) > 0) {
			if (attributes.getValue("id") != null) {
				recording = true;
				currentId = attributes.getValue("id");
			}
		}
	}

	public static void parse(InputSource file, String href, Ncx ncx)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				false);
		SAXParser parser = factory.newSAXParser();
		parser.parse(file, new TOCGenerator(href, ncx));
	}

}