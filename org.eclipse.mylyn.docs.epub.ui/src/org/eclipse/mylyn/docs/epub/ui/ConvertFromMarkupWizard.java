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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.OPSPublication;
import org.eclipse.mylyn.docs.epub.wikitext.MarkupToEPUB;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public class ConvertFromMarkupWizard extends Wizard {

	private static final String PLUGIN_ID = "org.eclipse.mylyn.docs.epub.ui";

	private EPUB2Bean bean;

	OPSPublication epub;

	private IFile epubFile;

	private IFile markupFile;

	private MarkupLanguage markupLanguage;

	private MainPage page;

	public ConvertFromMarkupWizard() {
		setWindowTitle("Generate EPUB");
		setNeedsProgressMonitor(true);
	}


	@Override
	public void addPages() {
		epub = OPSPublication.getVersion2Instance();
		// XXX: Read back epub
		File workingFolder = null;
		// if (epubFile.exists()) {
		// try {
		// workingFolder = epub.unpack(epubFile.getLocation().toFile());
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
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
		final MarkupToEPUB markupToEPUB = new MarkupToEPUB();
		markupToEPUB.setMarkupLanguage(markupLanguage);
		try {
			getContainer().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					MultiStatus ms = new MultiStatus(PLUGIN_ID, 0, "Could not generate EPUB",
							null);
					monitor.beginTask("Generate EPUB", 3);
					try {
						if (epubFile.exists()) {
							// Delete the old one
							epubFile.delete(true, monitor);
						}
						// Parse the wiki markup and populate the EPUB
						markupToEPUB.parse(epub, markupFile.getLocation().toFile());
						monitor.worked(1);
						List<Diagnostic> problems = epub.validateMetadata();
						
						if (problems.size()>0){
							for (Diagnostic diagnostic : problems) {
								System.out.println(diagnostic.getMessage());
								ms.add(new Status(IStatus.ERROR, PLUGIN_ID, diagnostic
										.getMessage()));
							}
							monitor.setCanceled(true);
							StatusManager.getManager().handle(ms, StatusManager.BLOCK);
							return;
						}
						EPUB publication = new EPUB();
						publication.add(epub);
						publication.pack(epubFile.getLocation().toFile());
						monitor.worked(1);
						epubFile.refreshLocal(IResource.DEPTH_ONE, monitor);
						monitor.worked(1);
					} catch (Exception e) {
						monitor.setCanceled(true);
						StatusManager.getManager().handle(ms, StatusManager.SHOW);
					} finally {
						monitor.done();
					}
				}
			});
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
