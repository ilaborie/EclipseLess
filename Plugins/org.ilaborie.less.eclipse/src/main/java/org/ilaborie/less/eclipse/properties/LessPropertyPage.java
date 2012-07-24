/*
 * 
 */
package org.ilaborie.less.eclipse.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.UIJob;
import org.ilaborie.less.eclipse.builder.Less;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * The Class LessPropertyPage.
 */
public class LessPropertyPage extends PropertyPage implements
		SelectionListener, ISelectionChangedListener {
	
	/** The log. */
	private Logger log = LoggerFactory.getLogger(this.getClass());

	/** The less. */
	private Less less;

	/** The files. */
	private List<IFile> files;

	/** The owner text. */
	private Button chkCompress;

	/** The tab files. */
	private TableViewer tabFiles;

	/** The btn add less file. */
	private Button btnAddLessFile;

	/** The btn del less file. */
	private Button btnDelLessFile;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public LessPropertyPage() {
		super();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

		IProject project = (IProject) this.getElement();
		this.less = new Less();
		this.less.setProject(project);

		// Compress
		this.chkCompress = new Button(composite, SWT.CHECK);
		this.chkCompress.setText("Compress");
		this.chkCompress.setSelection(this.less.isCompress());

		// Files
		Label label = new Label(composite, SWT.WRAP);
		label.setText("Files to compress");
		Composite cmp = this.createFilesComposite(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(cmp);

		return composite;
	}

	/**
	 * Creates the files composite.
	 * 
	 * @param parent
	 *            the parent
	 * @return the composite
	 */
	private Composite createFilesComposite(Composite parent) {
		Composite cmp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cmp);

		// Table
		this.tabFiles = new TableViewer(cmp);
		GridDataFactory.fillDefaults().grab(true, true).span(1, 3)
				.applyTo(this.tabFiles.getControl());
		this.tabFiles.setContentProvider(new ArrayContentProvider());
		this.tabFiles.setLabelProvider(new WorkbenchLabelProvider());

		this.files = Lists.newArrayList(this.less.getFiles());
		this.tabFiles.setInput(this.files);

		// Add Button
		this.btnAddLessFile = new Button(cmp, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING)
				.applyTo(this.btnAddLessFile);
		this.btnAddLessFile.setText("Add ...");
		this.btnAddLessFile.addSelectionListener(this);

		// Delete Button
		this.btnDelLessFile = new Button(cmp, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING)
				.applyTo(this.btnDelLessFile);
		this.btnDelLessFile.setText("Remove ...");
		this.btnDelLessFile.addSelectionListener(this);
		this.btnDelLessFile.setEnabled(false);

		this.tabFiles.addSelectionChangedListener(this);

		return cmp;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection) event.getSelection();
		this.btnDelLessFile.setEnabled(!sel.isEmpty());
	}

	public void widgetDefaultSelected(SelectionEvent e) {
		// Nothing to do
	}

	public void widgetSelected(SelectionEvent e) {
		Object src = e.getSource();
		if (this.btnAddLessFile.equals(src)) {
			CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(
					this.getShell(), new WorkbenchLabelProvider(),
					new BaseWorkbenchContentProvider());
			dialog.setTitle("Less File");
			dialog.setMessage("Select Less file to compile:");
			dialog.setInput(this.less.getProject());
			dialog.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement,
						Object element) {
					return (element instanceof IContainer)
							|| ((element instanceof IFile) && ((IFile) element)
									.getName().endsWith(".less"));
				}
			});

			if (Window.OK == dialog.open()) {
				IFile f;
				for (Object obj : dialog.getResult()) {
					if (obj instanceof IFile) {
						f = (IFile) obj;
						this.tabFiles.add(f);
						this.files.add(f);
					}
				}
			}
		} else if (this.btnDelLessFile.equals(src)) {
			IFile file = (IFile) ((IStructuredSelection) this.tabFiles
					.getSelection()).getFirstElement();
			this.files.remove(file);
			this.tabFiles.remove(file);
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		this.chkCompress.setSelection(this.less.isCompress());
		this.tabFiles.setInput(new ArrayList<String>());
	}

	@Override
	public boolean performOk() {
		this.less.setCompress(this.chkCompress.getSelection());
		this.less.setFiles(this.files);
		boolean result= this.less.store();
		
		if (result) {
			final IProject project = this.less.getProject();
			Job job = new UIJob(this.getShell().getDisplay(),"Rebuild") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					} catch (CoreException e) {
						log.error(e.toString(),e);
					}
					return Status.OK_STATUS;
				}
			}; 
			job.schedule();
		}
		return result;
	}

}