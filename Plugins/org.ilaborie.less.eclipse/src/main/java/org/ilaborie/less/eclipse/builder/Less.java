package org.ilaborie.less.eclipse.builder;

import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * The Class Less.
 */
public class Less implements IProjectNature {

	/** ID of this project nature. */
	public static final String NATURE_ID = "org.ilaborie.less.eclipse.Less";

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/** The project. */
	private IProject project;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		this.log.info("Configure");
		IProjectDescription desc = this.project.getDescription();
		List<ICommand> cmds = Lists.newArrayList(desc.getBuildSpec());

		Iterable<ICommand> filter = Iterables.filter(cmds, new LessCommand());
		if (!filter.iterator().hasNext()) {
			this.log.info("Add Less Nature");
			// Create a command
			ICommand command = desc.newCommand();
			command.setBuilderName(LessBuilder.BUILDER_ID);
			cmds.add(command);

			// Add the command
			desc.setBuildSpec(Iterables.toArray(cmds, ICommand.class));
			this.project.setDescription(desc, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = this.getProject().getDescription();
		List<ICommand> cmds = Lists.newArrayList(description.getBuildSpec());
		Iterable<ICommand> commands = Iterables.filter(cmds,
				Predicates.not(new LessCommand()));

		description.setBuildSpec(Iterables.toArray(commands, ICommand.class));
		this.project.setDescription(description, null);
	}

	private class LessCommand implements Predicate<ICommand> {
		public boolean apply(ICommand command) {
			return (command != null)
					&& LessBuilder.BUILDER_ID.equals(command.getBuilderName());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return this.project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core
	 * .resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
