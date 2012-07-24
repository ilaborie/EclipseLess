package org.ilaborie.less.eclipse.builder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.ilaborie.less.eclipse.functions.IFile2String;
import org.ilaborie.less.eclipse.functions.LessCommandPredicate;
import org.ilaborie.less.eclipse.functions.String2IFile;
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
public class Less implements IProjectNature, IAdaptable, Predicate<IFile> {

	private static final String SEPARATOR = ";";

	/** ID of this project nature. */
	public static final String NATURE_ID = "org.ilaborie.less.eclipse.Less";

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/** The project. */
	private IProject project;

	/** The compress. */
	private boolean compress = false;

	/** The files. */
	private final List<IFile> files;

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
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (IProject.class.equals(adapter)) {
			return this.project;
		}
		return null;
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

		Iterable<ICommand> filter = Iterables.filter(cmds,
				new LessCommandPredicate());
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
		return this.compress;
	}

	/**
	 * Sets the compress.
	 * 
	 * @param compress
	 *            the new compress
	 */
	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	/**
	 * Sets the files.
	 * 
	 * @param files
	 *            the new files
	 */
	public void setFiles(List<IFile> files) {
		this.files.clear();
		this.files.addAll(files);
	}

	public List<IFile> getFiles() {
		return this.files;
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
				Predicates.not(new LessCommandPredicate()));

		description.setBuildSpec(Iterables.toArray(commands, ICommand.class));
		this.project.setDescription(description, null);
	}

	public IProject getProject() {
		return this.project;
	}

	public void setProject(IProject project) {
		this.project = project;

		// File
		File file = this.getPropertiesFiles();
		try {
			if (file.exists()) {
				this.read(file);
			} else {
				this.store();
			}
		} catch (IOException e) {
			this.log.error(e.toString(), e);
		}
	}

	/**
	 * Read.
	 * 
	 * @param file
	 *            the file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void read(File file) throws IOException {
		this.log.debug("Read file: {}", file);
		List<String> lines = Files.readLines(file, Charsets.UTF_8);
		this.compress = false;
		if (!lines.isEmpty()) {
			this.compress = Boolean.valueOf(lines.get(0));
			this.files.clear();
			if (lines.size() == 2) {
				Iterable<String> split = Splitter.on(SEPARATOR).split(
						lines.get(1));
				Iterable<IFile> elements = Iterables.transform(Lists
						.newArrayList(split), new String2IFile(this.project));
				this.files.addAll(Lists.newArrayList(elements));
			}
		}
	}

	/**
	 * Store.
	 * 
	 * @return true, if successful
	 */
	public boolean store() {
		File file = this.getPropertiesFiles();
		this.log.debug("Store default properties to file: {}", file);
		String f = Joiner.on(SEPARATOR).join(
				Iterables.transform(this.files, new IFile2String()));
		try {
			if (file.exists()) {
				file.delete();
			} else {
				Files.createParentDirs(file.getParentFile());
			}
			Files.append(String.valueOf(this.compress)+"\n", file, Charsets.UTF_8);
			Files.append(f, file, Charsets.UTF_8);
			return true;
		} catch (IOException e) {
			this.log.error(e.toString(), e);
			return false;
		}
	}

	/**
	 * Gets the properties files.
	 * 
	 * @return the properties files
	 */
	private File getPropertiesFiles() {
		IPath workingLocation = project.getWorkingLocation(NATURE_ID);
		return new File(workingLocation.toFile(), "options");
	}

	/**
	 * Apply.
	 * 
	 * @param ifile
	 *            the ifile
	 * @return true, if successful
	 */
	public boolean apply(IFile ifile) {
		return (this.files.isEmpty() && ifile.getName().endsWith(".less"))
				|| (this.files.contains(ifile));
	}
}
