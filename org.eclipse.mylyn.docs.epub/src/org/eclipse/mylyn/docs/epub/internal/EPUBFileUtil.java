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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Various EPUB file related utilities.
 * 
 * @author Torkild U. Resheim
 */
public class EPUBFileUtil {

	private static final String MIMETYPE = "application/epub+zip";

	public static void copy(File source, File destination) throws IOException {
		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(source);
			to = new FileOutputStream(destination);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1) {
				to.write(buffer, 0, bytesRead);
			}
		} finally {
			if (from != null)
				try {
					from.close();
				} catch (IOException e) {
				}
			if (to != null)
				try {
					to.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * Creates a path segment list.
	 * 
	 * @param root
	 *            the root folder
	 * @param file
	 *            the destination file
	 * @param segments
	 * @see #getRelativePath(File, File)
	 */
	private static void getPathSegments(File root, File file,
			ArrayList<String> segments) {
		if (root.equals(file)) {
			return;
		}
		segments.add(0, file.getName());
		getPathSegments(root, file.getParentFile(), segments);
	}

	/**
	 * Determines the <i>root</i> relative path of <i>file</i> in a platform
	 * independent manner. The returned string is a path starting from but
	 * excluding <i>root</i> using the '/' character as a directory separator.
	 * If the <i>file</i> argument is a folder a trailing directory separator is
	 * added.
	 * 
	 * @param root
	 *            the root directory
	 * @param file
	 *            the root contained file or directory
	 * @return the platform independent, relative path
	 */
	public static String getRelativePath(File root, File file) {
		ArrayList<String> segments = new ArrayList<String>();
		getPathSegments(root, file, segments);
		StringBuilder path = new StringBuilder();
		for (int p = 0; p < segments.size(); p++) {
			if (p > 0) {
				path.append('/');
			}
			path.append(segments.get(p));
		}
		if (file.isDirectory()) {
			path.append('/');
		}
		return path.toString();
	}

	/**
	 * A correctly formatted EPUB file must contain an uncompressed entry named
	 * "mimetype" that is placed at the beginning. This method will create this
	 * file.
	 * 
	 * @param zos
	 *            the zip output stream to write to.
	 * @throws IOException
	 */
	public static void writeEPUBHeader(ZipOutputStream zos) throws IOException {
		byte[] bytes = MIMETYPE.getBytes("ASCII");
		ZipEntry mimetype = new ZipEntry("mimetype");
		mimetype.setMethod(ZipOutputStream.STORED);
		mimetype.setSize(bytes.length);
		mimetype.setCompressedSize(bytes.length);
		CRC32 crc = new CRC32();
		crc.update(bytes);
		mimetype.setCrc(crc.getValue());
		zos.putNextEntry(mimetype);
		zos.write(bytes);
		zos.closeEntry();
	}

	/**
	 * Recursively compresses contents of the given folder into a zip-file. If a
	 * file already exists in the given location an exception will be thrown.
	 * 
	 * @param destination
	 *            the destination file
	 * @param folder
	 *            the source folder
	 * @param uncompressed
	 *            a list of files to keep uncompressed
	 * @throws ZipException
	 * @throws IOException
	 */
	public static void zip(File destination, File folder) throws ZipException,
			IOException {
		if (destination.exists()) {
			throw new IOException("The destination zip-file already exists.");
		}
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
				destination));
		writeEPUBHeader(out);
		zip(folder, folder, out);
		out.close();
	}

	/**
	 * Adds a folder recursively to the output stream.
	 * 
	 * @param folder
	 *            the root folder
	 * @param out
	 *            the output stream
	 * @throws IOException
	 */
	private static void zip(File root, File folder, ZipOutputStream out)
			throws IOException {
		// Files first in order to make sure "metadata" is placed first in the
		// zip file. We need that in order to support EPUB properly.
		File[] files = folder.listFiles(new java.io.FileFilter() {
			public boolean accept(File pathname) {
				return !pathname.isDirectory();
			}
		});
		byte[] tmpBuf = new byte[1024];

		for (int i = 0; i < files.length; i++) {
			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			ZipEntry zipEntry = new ZipEntry(getRelativePath(root, files[i]));
			out.putNextEntry(zipEntry);
			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
		File[] dirs = folder.listFiles(new java.io.FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		for (int i = 0; i < dirs.length; i++) {
			out.putNextEntry(new ZipEntry(getRelativePath(root, dirs[i])));
			zip(root, dirs[i], out);
		}
	}

}
