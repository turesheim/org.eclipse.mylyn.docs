/*******************************************************************************
 * Copyright (c) 2012, 2016 Stefan Seelmann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Seelmann - initial API and implementation
 *     Max Rydahl Andersen - Bug 474084
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 474084
 *     Jeremie Bresson - Bug 492301
 *******************************************************************************/

package org.eclipse.mylyn.internal.wikitext.asciidoc.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Tests for asciidoc block elements.
 *
 * @author Max Rydahl Andersen
 */
public class AsciiDocLanguageBlockElementsTest extends AsciiDocLanguageTestBase {

	@Test
	public void paragraphWithOneLine() {
		String html = parseToHtml("a paragraph");
		assertEquals("<p>a paragraph</p>\n", html);
	}

	@Test
	public void testParagraphWithMulitpleLines() {
		String html = parseToHtml("a paragraph\nwith multiple\nlines");

		assertEquals("<p>a paragraph\nwith multiple\nlines</p>\n", html);
	}

	@Test
	public void testParagraphsSeparatedBySingleBlankLine() {
		String html = parseToHtml("a paragraph\n\nanother paragraph\n\n");
		String expected = "<p>a paragraph</p>\n<p>another paragraph</p>\n";
		assertEquals(expected, html);
	}

	@Test
	public void testParagraphsSeparatedByMulitpleBlankLines() {
		String html = parseToHtml("a paragraph\n\n\nanother paragraph\n\n\n");
		String expected = "<p>a paragraph</p>\n<p>another paragraph</p>\n";
		assertEquals(expected, html);
	}

	@Test
	public void testParagraphsSeparatedByMulitpleBlankLinesWithSpacesAndTabs() {
		String html = parseToHtml("a paragraph\n \n\t\nanother paragraph");
		String expected = "<p>a paragraph</p>\n<p>another paragraph</p>\n";
		assertEquals(expected, html);
	}

	@Test
	public void testLineBreakInParagraph() {
		String html = parseToHtml("line  1 +\nline  2 +\nline  3");
		assertEquals("<p>line  1<br/>\nline  2<br/>\nline  3</p>\n", html);
	}

	@Test
	public void testLineBreakInParagraphWithTabAndMultipleSpaces() {
		String html = parseToHtml("line  1   +\nline  2\t+\nline  3");
		assertEquals("<p>line  1  <br/>\nline  2<br/>\nline  3</p>\n", html);
	}

	@Test
	public void testLineBreakInParagraphTrailingSpaces() {
		String html = parseToHtml("line  1 +   \nline  2 +\t\nline  3");
		assertEquals("<p>line  1<br/>\nline  2<br/>\nline  3</p>\n", html);
	}

	/*
	 * Headers.
	 */
	@Test
	public void testEqStyleHeaderLevel1() {
		String html = parseToHtml("== This is an H2");

		assertEquals("<h2 id=\"_this_is_an_h2\">This is an H2</h2>", html);
	}

	@Test
	public void testEqStyleHeaderLevel2() {
		String html = parseToHtml("=== This is an H3");

		assertEquals("<h3 id=\"_this_is_an_h3\">This is an H3</h3>", html);
	}

	@Test
	public void testEqStyleHeaderLevel3() {
		String html = parseToHtml("==== This is an H4");

		assertEquals("<h4 id=\"_this_is_an_h4\">This is an H4</h4>", html);
	}

	@Test
	public void testEqStyleHeaderLevel4() {
		String html = parseToHtml("===== This is an H5");

		assertEquals("<h5 id=\"_this_is_an_h5\">This is an H5</h5>", html);
	}

	@Test
	public void testEqStyleHeaderNotH6() {
		String html = parseToHtml("====== This is not h6");

		assertEquals("<p>====== This is not h6</p>\n", html);
	}

	@Test
	public void testEqStyleHeaderNoTitleWith7eq() {
		String html = parseToHtml("======== This is not a title (7)");

		assertEquals("<p>======== This is not a title (7)</p>\n", html);
	}

	@Test
	public void testEqStyleHeaderNoTitleWith10eq() {
		String html = parseToHtml("=========== This is not a title (10)");

		assertEquals("<p>=========== This is not a title (10)</p>\n", html);
	}

	@Test
	public void headerLevel1WithLeveloffset2() {
		String html = parseToHtml(":leveloffset: 2\n\n== This is Level 1");

		assertEquals("<h4 id=\"_this_is_level_1\">This is Level 1</h4>", html);
	}

	@Test
	public void headerLevel1WithLeveloffset3() {
		String html = parseToHtml(":leveloffset: 3\n\n== This is Level 1");

		assertEquals("<h5 id=\"_this_is_level_1\">This is Level 1</h5>", html);
	}

	@Test
	public void headerLevel1WithLeveloffset6() {
		String html = parseToHtml(":leveloffset: 6\n\n== This is Level 1");

		assertEquals("<h6 id=\"_this_is_level_1\">This is Level 1</h6>", html);
	}

	@Test
	public void headerLevel4WithLeveloffsetMinus2() {
		String html = parseToHtml(":leveloffset: -2\n\n==== This is Level 3");

		assertEquals("<h2 id=\"_this_is_level_3\">This is Level 3</h2>", html);
	}

	@Test
	public void headerLevel4WithLeveloffsetMinus6() {
		String html = parseToHtml(":leveloffset: -6\n\n==== This is Level 3");

		assertEquals("<h1 id=\"_this_is_level_3\">This is Level 3</h1>", html);
	}

	@Test
	public void testHeadingContainingEmphasisStyle() {
		//Bug 492301
		String text = "=== This _is_ true!";

		String html = parseToHtml(text.toString());

		assertEquals("<h3 id=\"_this_em_is_em_true\">This <em>is</em> true!</h3>", html);
	}

	@Test
	public void testHeadingContainingCodeStyle() {
		//Bug 492301
		String text = "==== The `HelloWorld` class";

		String html = parseToHtml(text.toString());

		assertEquals("<h4 id=\"_the_code_helloworld_code_class\">The <code>HelloWorld</code> class</h4>", html);
	}

	@Test
	public void testIdWithAlternativeFormat() {
		String text = ":idprefix: \n" + //
				":idseparator: -\n" + //
				"\n" + //
				"== This is an H2\n";

		String html = parseToHtml(text.toString());

		assertEquals("<h2 id=\"this-is-an-h2\">This is an H2</h2>", html);
	}

	@Test
	public void testIdWithChangesInAlternativeFormat() {
		String text = "" + //
				":idprefix: .\n" + //
				":idseparator: -\n" + //
				"\n" + //
				"== h2 title\n" + //
				"\n" + //
				":idprefix: -\n" + //
				":idseparator: =\n" + //
				"\n" + //
				"=== SOME    H3\n";

		String html = parseToHtml(text.toString());

		assertEquals("<h2 id=\".h2-title\">h2 title</h2><h3 id=\"-some=h3\">SOME    H3</h3>", html);
	}

	@Test
	public void testMultipleTitleIds() {
		String text = "" + //
				"== Lorem Ipsum\n\n" + //
				"== Lorem Ipsum\n\n" + //
				"== Lorem Ipsum 2\n\n" + //
				"== Lorem Ipsum 3\n\n" + //
				"== Lorem Ipsum\n\n";
		String html = parseToHtml(text.toString());

		String expected = "<h2 id=\"_lorem_ipsum\">Lorem Ipsum</h2>" + //
				"<h2 id=\"_lorem_ipsum_2\">Lorem Ipsum</h2>" + //
				"<h2 id=\"_lorem_ipsum_2_2\">Lorem Ipsum 2</h2>" + //
				"<h2 id=\"_lorem_ipsum_3\">Lorem Ipsum 3</h2>" + //
				"<h2 id=\"_lorem_ipsum_4\">Lorem Ipsum</h2>";

		assertEquals(expected, html);
	}

	/*
	 * Optionally, you may "close" equals-style headers.
	 */
	@Test
	public void testClosedEqStyleHeaderLevel1() {
		String html = parseToHtml("== This is also H2 ==");

		assertEquals("<h2 id=\"_this_is_also_h2\">This is also H2</h2>", html);
	}

	@Test
	public void testClosedEqStyleHeaderLevel2() {
		String html = parseToHtml("=== This is also H3 ===");

		assertEquals("<h3 id=\"_this_is_also_h3\">This is also H3</h3>", html);
	}

	@Test
	public void testClosedEqStyleHeaderLevel3() {
		String html = parseToHtml("==== This is also H4 ====");

		assertEquals("<h4 id=\"_this_is_also_h4\">This is also H4</h4>", html);
	}

	@Test
	public void testClosedEqStyleHeaderLevel4() {
		String html = parseToHtml("===== This is also H5 =====");

		assertEquals("<h5 id=\"_this_is_also_h5\">This is also H5</h5>", html);
	}

	@Test
	public void testClosedEqStyleHeaderLevel4WithSpaces() {
		String html = parseToHtml("===== This is H5 with spaces    =====");

		assertEquals("<h5 id=\"_this_is_h5_with_spaces\">This is H5 with spaces</h5>", html);
	}

	@Test
	public void testClosedEqStyleHeaderNotH6() {
		String html = parseToHtml("====== This is also not h6 ======");

		assertEquals("<p>====== This is also not h6 ======</p>\n", html);
	}

	@Test
	public void testClosedEqStyleHeaderNoTitleWith7eq() {
		String html = parseToHtml("======= This is also not a title (7) =======");

		assertEquals("<p>======= This is also not a title (7) =======</p>\n", html);
	}

	@Test
	public void testClosedEqStyleHeaderNoTitleWith12eq() {
		String html = parseToHtml("============ This is also not a title (12) ============");

		assertEquals("<p>============ This is also not a title (12) ============</p>\n", html);
	}

	@Test
	public void testClosedEqStyleHeaderWithMoreClosingEq() {
		String html = parseToHtml("== This is an H2 again ==================");

		assertEquals("<h2 id=\"_this_is_an_h2_again\">This is an H2 again ==================</h2>", html);
	}

	@Test
	public void testClosedEqStyleHeaderWithMoreClosingEqAndSpaces() {
		String html = parseToHtml("== This is an H2 with spaces     ====");

		assertEquals("<h2 id=\"_this_is_an_h2_with_spaces\">This is an H2 with spaces     ====</h2>", html);
	}

	@Test
	public void testClosedAtxStyleHeaderWithLessCosingEq() {
		String html = parseToHtml("===== This is an H5 again ==");

		assertEquals("<h5 id=\"_this_is_an_h5_again\">This is an H5 again ==</h5>", html);
	}

	/*
	 * "underlined" headers
	 */
	@Test
	public void testUnderlinedLevel1() {
		String html = parseToHtml("This is an underlined H2\n------------------------");

		assertEquals("<h2 id=\"_this_is_an_underlined_h2\">This is an underlined H2</h2>", html);
	}

	@Test
	public void testUnderlinedLevel2() {
		String html = parseToHtml("This is an underlined H3\n~~~~~~~~~~~~~~~~~~~~~~~~");

		assertEquals("<h3 id=\"_this_is_an_underlined_h3\">This is an underlined H3</h3>", html);
	}

	@Test
	public void testUnderlinedLevel3() {
		String html = parseToHtml("This is an underlined H4\n^^^^^^^^^^^^^^^^^^^^^^^^");

		assertEquals("<h4 id=\"_this_is_an_underlined_h4\">This is an underlined H4</h4>", html);
	}

	@Test
	public void testUnderlinedLevel4() {
		String html = parseToHtml("This is an underlined H5\n++++++++++++++++++++++++");

		assertEquals("<h5 id=\"_this_is_an_underlined_h5\">This is an underlined H5</h5>", html);
	}

	@Test
	public void testUnderlinedLevel1LineMinusOneChar() {
		String html = parseToHtml("Lorem Ipsum\n----------"); //title 11 chars, line 10 chars

		assertEquals("<h2 id=\"_lorem_ipsum\">Lorem Ipsum</h2>", html);
	}

	@Test
	public void testUnderlinedLevel2LineMinusOneChar() {
		String html = parseToHtml("Lorem Ipsum Dolor\n~~~~~~~~~~~~~~~~"); //title 17 chars, line 16 chars

		assertEquals("<h3 id=\"_lorem_ipsum_dolor\">Lorem Ipsum Dolor</h3>", html);
	}

	@Test
	public void testUnderlinedLevel3LineMinusOneChar() {
		String html = parseToHtml("LoremIpsumDolor\n^^^^^^^^^^^^^^"); //title 15 chars, line 14 chars

		assertEquals("<h4 id=\"_loremipsumdolor\">LoremIpsumDolor</h4>", html);
	}

	@Test
	public void testUnderlinedLevel4LineMinusOneChar() {
		String html = parseToHtml("Lorem-Ipsum\n++++++++++"); //title 11 chars, line 10 chars

		assertEquals("<h5 id=\"_lorem_ipsum\">Lorem-Ipsum</h5>", html);
	}

	@Test
	public void testUnderlinedLevel1LinePlusOneChar() {
		String html = parseToHtml("Lorem Ipsum\n------------"); //title 11 chars, line 12 chars

		assertEquals("<h2 id=\"_lorem_ipsum\">Lorem Ipsum</h2>", html);
	}

	@Test
	public void testNotUnderlinedLevel1LineMinusTwoChars() {
		String html = parseToHtml("Lorem Ipsum\n---------"); //title 11 chars, line 9 chars

		assertFalse(Pattern.compile("<h2 id=\"_lorem_ipsum\">Lorem Ipsum</h2>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel2LineMinusTwoChars() {
		String html = parseToHtml("Lorem Ipsum Dolor\n~~~~~~~~~~~~~~~"); //title 17 chars, line 15 chars

		assertFalse(Pattern.compile("<h3 id=\"_lorem_ipsum_dolor\">Lorem Ipsum Dolor</h3>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel3LineMinusTwoChars() {
		String html = parseToHtml("LoremIpsumDolor\n^^^^^^^^^^^^^"); //title 15 chars, line 13 chars

		assertFalse(Pattern.compile("<h4 id=\"_loremipsumdolor\">LoremIpsumDolor</h4>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel4LineMinusTwoChars() {
		String html = parseToHtml("Lorem-Ipsum\n+++++++++"); //title 11 chars, line 9 chars

		assertFalse(Pattern.compile("<h5 id=\"_lorem-ipsum\">Lorem-Ipsum</h5>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel1LinePlusTwoChars() {
		String html = parseToHtml("Lorem Ipsum\n-------------"); //title 11 chars, line 13 chars

		assertFalse(Pattern.compile("<h2 id=\"_lorem_ipsum\">Lorem Ipsum</h2>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel2LinePlusTwoChars() {
		String html = parseToHtml("Lorem Ipsum Dolor\n~~~~~~~~~~~~~~~~~~~"); //title 17 chars, line 18 chars

		assertFalse(Pattern.compile("<h3 id=\"_lorem_ipsum_dolor\">Lorem Ipsum Dolor</h3>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel3LinePlusTwoChars() {
		String html = parseToHtml("LoremIpsumDolor\n^^^^^^^^^^^^^^^^^"); //title 15 chars, line 16 chars

		assertFalse(Pattern.compile("<h4 id=\"_loremipsumdolor\">LoremIpsumDolor</h4>").matcher(html).find());
	}

	@Test
	public void testNotUnderlinedLevel4LinePlusTwoChars() {
		String html = parseToHtml("Lorem-Ipsum\n+++++++++++++"); //title 11 chars, line 12 chars

		assertFalse(Pattern.compile("<h5 id=\"_lorem-ipsum\">Lorem-Ipsum</h5>").matcher(html).find());
	}

	@Test
	public void testUnderlinedLevel1TitleTrailingSpaces() {
		String html = parseToHtml("Title test underlined H2     \n------------------------");

		assertEquals("<h2 id=\"_title_test_underlined_h2\">Title test underlined H2</h2>", html);
	}

	@Test
	public void testUnderlinedLevel2TitleTrailingSpaces() {
		String html = parseToHtml("Title test underlined H3\t\n~~~~~~~~~~~~~~~~~~~~~~~~");

		assertEquals("<h3 id=\"_title_test_underlined_h3\">Title test underlined H3</h3>", html);
	}

	@Test
	public void testUnderlinedLevel3TitleTrailingSpaces() {
		String html = parseToHtml("Title test underlined H4   \t\n^^^^^^^^^^^^^^^^^^^^^^^^");

		assertEquals("<h4 id=\"_title_test_underlined_h4\">Title test underlined H4</h4>", html);
	}

	@Test
	public void testUnderlinedLevel4TitleTrailingSpaces() {
		String html = parseToHtml("Title test underlined H5\t  \n++++++++++++++++++++++++");

		assertEquals("<h5 id=\"_title_test_underlined_h5\">Title test underlined H5</h5>", html);
	}

	@Test
	public void testUnderlinedLevel1LineWithTrailingSpaces() {
		String html = parseToHtml("Title test underlined H2\n------------------------     ");

		assertEquals("<h2 id=\"_title_test_underlined_h2\">Title test underlined H2</h2>", html);
	}

	@Test
	public void testUnderlinedLevel2LineWithTrailingSpaces() {
		String html = parseToHtml("Title test underlined H3\n~~~~~~~~~~~~~~~~~~~~~~~~\t");

		assertEquals("<h3 id=\"_title_test_underlined_h3\">Title test underlined H3</h3>", html);
	}

	@Test
	public void testUnderlinedLevel3LineWithTrailingSpaces() {
		String html = parseToHtml("Title test underlined H4\n^^^^^^^^^^^^^^^^^^^^^^^^\t\t");

		assertEquals("<h4 id=\"_title_test_underlined_h4\">Title test underlined H4</h4>", html);
	}

	@Test
	public void testUnderlinedLevel4LineWithTrailingSpaces() {
		String html = parseToHtml("Title test underlined H5\n++++++++++++++++++++++++\t  ");

		assertEquals("<h5 id=\"_title_test_underlined_h5\">Title test underlined H5</h5>", html);
	}

	@Test
	public void underlinedLevel3WithLeveloffset1() {
		String html = parseToHtml(":leveloffset:1\n\nThis is Level 3\n^^^^^^^^^^^^^^^");

		assertEquals("<h5 id=\"_this_is_level_3\">This is Level 3</h5>", html);
	}

	@Test
	public void underlinedLevel3WithLeveloffset4() {
		String html = parseToHtml(":leveloffset:4\n\nThis is Level 3\n^^^^^^^^^^^^^^^");

		assertEquals("<h6 id=\"_this_is_level_3\">This is Level 3</h6>", html);
	}

	@Test
	public void underlinedLevel3WithLeveloffsetMinus4() {
		String html = parseToHtml(":leveloffset:-4\n\nThis is Level 3\n^^^^^^^^^^^^^^^");

		assertEquals("<h1 id=\"_this_is_level_3\">This is Level 3</h1>", html);
	}

	@Test
	public void testPreBlockIndentedByFourSpaces() {
		String html = parseToHtml("    This is a pre block.");

		assertEquals("<pre>This is a pre block.</pre>", html);
	}

	@Test
	public void testPreBlockIndentedByOneTab() {
		String html = parseToHtml("\tThis is a pre block.");

		assertEquals("<pre>This is a pre block.</pre>", html);
	}

	/*
	 * One level of indentation - 4 spaces or 1 tab - is removed from each line of the pre block.
	 */
	@Test
	public void testPreBlockMultiLineIndentedByFourSpaces() {
		String html = parseToHtml("    aaa\n        bbb\n            ccc");

		String expectedHtml = "<pre>aaa\n    bbb\n        ccc</pre>";
		assertEquals(expectedHtml, html);
	}

	@Test
	public void testPreBlockMultiLineIndentedByOneTab() {
		String html = parseToHtml("\taaa\n\t\tbbb\n\t\t\tccc");

		String expectedHtml = "<pre>aaa\n\tbbb\n\t\tccc</pre>";
		assertEquals(expectedHtml, html);
	}

	@Test
	public void testPreBlockMultiLineIndentedByFourSpacesNoContinueAfterEmptyLine() {
		String html = parseToHtml("    aaa\n    bbb\n    ccc\n        \n    after empty line");

		String expected = "<pre>aaa\nbbb\nccc</pre><pre>after empty line</pre>";
		assertEquals(expected, html);
	}

	@Test
	public void testPreBlockMultiLineIndentedByFourSpacesNoContinueAfterTabLine() {
		String html = parseToHtml("    aaa\n    bbb\n    ccc\n    \t\t\n    after empty line");

		String expected = "<pre>aaa\nbbb\nccc</pre><pre>after empty line</pre>";
		assertEquals(expected, html);
	}

	@Test
	public void testPreBlockMultiLineIndentedByOneTabNoContinueAfterEmptyLine() {
		String html = parseToHtml("\taaa\n\tbbb\n\tccc\n\t\n\tafter empty line");

		String expected = "<pre>aaa\nbbb\nccc</pre><pre>after empty line</pre>";
		assertEquals(expected, html);
	}

	/**
	 * Within a pre block, ampersands (&) and angle brackets (< and >) are automatically converted into HTML entities.
	 */
	@Test
	public void testSpecialCharactersAreConvertedInCodeBlock() {
		String html = parseToHtml("    <div class=\"footer\">\n    &copy; 2004 Foo Bar\n    </div>");

		String expectedHtml = "<pre>&lt;div class=\"footer\"&gt;\n&amp;copy; 2004 Foo Bar\n&lt;/div&gt;</pre>";
		assertEquals(expectedHtml, html);
	}

	/**
	 * Regular asciidoc syntax is not processed within code blocks.
	 */
	@Test
	public void testNoProcessingInCodeBlock() {
		String html = parseToHtml("    === Header 3\n    Lorem *ipsum*");

		String expectedHtml = "<pre>=== Header 3\nLorem *ipsum*</pre>";
		assertEquals(expectedHtml, html);
	}
}
