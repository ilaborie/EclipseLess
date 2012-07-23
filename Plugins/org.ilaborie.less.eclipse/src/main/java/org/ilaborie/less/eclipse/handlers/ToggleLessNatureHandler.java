package org.ilaborie.less.eclipse.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ilaborie.less.eclipse.builder.Less;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * The Class ToggleLessNatureHandler.
 */
public class ToggleLessNatureHandler extends AbstractHandler {

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
	 * .ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object element = sel.getFirstElement();
			if (element instanceof IProject) {
				IProject project = (IProject) element;
				this.toggleNature(project);
			}
		}
		return null;
	}

	/**
	 * Toggle nature.
	 * 
	 * @param project
	 *            the project
	 * @throws ExecutionException
	 *             the execution exception
	 */
	private void toggleNature(IProject project) throws ExecutionException {
		this.log.info("Toggle Less Nature on {}", project);
		try {
			IProjectDescription description = project.getDescription();
			List<String> natures = Lists.newArrayList(description
					.getNatureIds());
			if (natures.contains(Less.NATURE_ID)) {
				this.log.debug("Remove Less Nature");
				natures.remove(Less.NATURE_ID);
			} else {
				this.log.debug("Add Less Nature");
				natures.add(Less.NATURE_ID);
			}

			// Update Project description
			this.log.trace("Add Less Nature");
			description.setNatureIds(Iterables.toArray(natures, String.class));
			project.setDescription(description, null);
		} catch (CoreException e) {
			this.log.error(e.toString(), e);
			throw new ExecutionException(e.toString(), e);
		}
	}

}
