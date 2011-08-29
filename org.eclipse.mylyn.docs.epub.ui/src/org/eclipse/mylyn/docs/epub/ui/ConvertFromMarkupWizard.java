package org.eclipse.mylyn.docs.epub.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.docs.epub.EPUB;
import org.eclipse.mylyn.docs.epub.wikitext.MarkupToEPUB;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.ui.PlatformUI;

public class ConvertFromMarkupWizard extends Wizard {

	private IFile markupFile;

	private IFile epubFile;

	private MarkupLanguage markupLanguage;

	private MainPage page;

	private EPUB2Bean bean;

	public ConvertFromMarkupWizard() {
		setWindowTitle("New Wizard");
	}

	EPUB epub;

	@Override
	public void addPages() {
		epub = EPUB.getVersion2Instance();
		if (epubFile.exists()) {
			try {
				epub.unpack(epubFile.getLocation().toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		bean = new EPUB2Bean(epub, markupFile.getLocation().toFile());
		page = new MainPage(bean);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
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

	public void init(IFile markupFile, IFile epubFile, MarkupLanguage markupLanguage) {
		this.markupFile = markupFile;
		this.epubFile = epubFile;
		this.markupLanguage = markupLanguage;
	}

}
