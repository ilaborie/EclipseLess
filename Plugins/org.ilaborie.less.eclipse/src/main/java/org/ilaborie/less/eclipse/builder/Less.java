package org.ilaborie.less.eclipse.builder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

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

	private boolean compress = false;

	/** The files. */
	private final List<String> files;

	/**
	 * Instantiates a new less.
	 */
	public Less() {
		super();
		this.files = Lists.newArrayList();
	}

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

	public boolean isCompress() {
		return compress;
	}

	protected List<String> getFiles() {
		return files;
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

		//
		IPath workingLocation = project.getWorkingLocation(NATURE_ID);
		File file = new File(workingLocation.toFile(), "options");
		Charset utf8 = Charsets.UTF_8;
		String separator = ";";
		try {
			if (file.exists()) {
				log.info("Read file: {}", file);
				List<String> lines = Files.readLines(file, utf8);
				this.compress = false;
				if (!lines.isEmpty()) {
					this.compress = Boolean.valueOf(lines.get(0));
					this.files.clear();
					if (lines.size() == 2) {
						Iterable<String> split = Splitter.on(separator).split(
								lines.get(1));
						this.files.addAll(Lists.newArrayList(split));
					}
				}
			} else {
				log.info("Store default properties to file: {}", file);
				String f = Joiner.on(separator).join(this.files);

				Files.createParentDirs(file.getParentFile());
				Files.append(String.valueOf(this.compress), file, utf8);
				Files.append(f, file, utf8);
			}
		} catch (IOException e) {
			this.log.error(e.toString(), e);
		}
	}

	/**
	 * Apply.
	 * 
	 * @param ifile
	 *            the ifile
	 * @return true, if successful
	 */
	public boolean apply(IFile ifile) {
		String path = ifile.getLocation().toPortableString();
		return (this.files.isEmpty() && ifile.getName().endsWith(".less"))
				|| (this.files.contains(path));
	}
}
