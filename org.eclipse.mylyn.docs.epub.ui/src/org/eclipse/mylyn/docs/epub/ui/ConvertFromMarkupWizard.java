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
package org.eclipse.mylyn.docs.epub.ui;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.wikitext.MarkupToEPUB;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.ui.PlatformUI;

public class ConvertFromMarkupWizard extends Wizard {

	private EPUB2Bean bean;

	EPUB epub;

	private IFile epubFile;

	private IFile markupFile;

	private MarkupLanguage markupLanguage;

	private MainPage page;

	public ConvertFromMarkupWizard() {
		setWindowTitle("Create new publication");
	}


	@Override
	public void addPages() {
		epub = EPUB.getVersion2Instance();
		File workingFolder = null;
		if (epubFile.exists()) {
			try {
				workingFolder = epub.unpack(epubFile.getLocation().toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		bean = new EPUB2Bean(epub, markupFile.getLocation().toFile(), epubFile.getLocation().toFile(), workingFolder);
		page = new MainPage(bean);
		addPage(page);
	}

	public void init(IFile markupFile, IFile epubFile, MarkupLanguage markupLanguage) {
		this.markupFile = markupFile;
		this.epubFile = epubFile;
		this.markupLanguage = markupLanguage;
	}

	@Override
	public boolean performFinish() {
		List<Diagnostic> problems = epub.validate();
		for (Diagnostic diagnostic : problems) {
			System.out.println(diagnostic.getMessage());
		}
		final MarkupToEPUB markupToEPUB = new MarkupToEPUB();
		markupToEPUB.setMarkupLanguage(markupLanguage);
		try {

			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						if (epubFile.exists()) {
							// Delete the old one
							epubFile.delete(true, monitor);
						}
						// Create a new file
						markupToEPUB.parse(epub, markupFile.getLocation().toFile());
						epub.pack(epubFile.getLocation().toFile());
						epubFile.refreshLocal(IResource.DEPTH_ONE, monitor);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			} catch (InterruptedException e) {
				// return;
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (Throwable e) {
			StringWriter message = new StringWriter();
			PrintWriter out = new PrintWriter(message);
			out.println(Messages.ConvertMarkupToEPUB_cannotConvert + e.getMessage());
			out.println(Messages.ConvertMarkupToEPUB_detailsFollow);
			e.printStackTrace(out);
			out.close();

			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					Messages.ConvertMarkupToEPUB_cannotCompleteOperation, message.toString());
		}
		return true;
	}

}
