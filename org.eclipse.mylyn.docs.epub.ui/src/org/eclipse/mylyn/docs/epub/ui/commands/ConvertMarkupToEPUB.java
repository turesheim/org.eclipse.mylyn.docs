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
package org.eclipse.mylyn.docs.epub.ui.commands;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.docs.epub.wikitext.MarkupToEPUB;
import org.eclipse.mylyn.wikitext.ui.commands.AbstractMarkupResourceHandler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Handler that is used to convert files containing wiki markup into EPUB.
 * 
 * @author Torkild U. Resheim
 * @since 1.0
 */
public class ConvertMarkupToEPUB extends AbstractMarkupResourceHandler {

	@Override
	protected void handleFile(final IFile file, String name)
			throws ExecutionException {
		final IFile newFile = file.getParent()
				.getFile(new Path(name + ".epub")); //$NON-NLS-1$
		if (newFile.exists()) {
			if (!MessageDialog.openQuestion(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(),
					Messages.ConvertMarkupToEPUB_overwrite, NLS.bind(
							Messages.ConvertMarkupToEPUB_fileExistsOverwrite,
							new Object[] { newFile.getFullPath() }))) {
				return;
			}
		}

		final MarkupToEPUB markupToEPUB = new MarkupToEPUB();
		markupToEPUB.setMarkupLanguage(markupLanguage);
		markupToEPUB.setBookTitle(name);

		try {

			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						if (newFile.exists()) {
							newFile.delete(true, monitor);
						}
						markupToEPUB.parse(file.getLocation().toFile(), newFile
								.getLocation()
								.toFile());
						newFile.refreshLocal(IResource.DEPTH_ONE, monitor);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(runnable);
			} catch (InterruptedException e) {
				return;
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (Throwable e) {
			StringWriter message = new StringWriter();
			PrintWriter out = new PrintWriter(message);
			out.println(Messages.ConvertMarkupToEPUB_cannotConvert
					+ e.getMessage());
			out.println(Messages.ConvertMarkupToEPUB_detailsFollow);
			e.printStackTrace(out);
			out.close();

			MessageDialog.openError(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(),
					Messages.ConvertMarkupToEPUB_cannotCompleteOperation,
					message.toString());
		}
	}
}
