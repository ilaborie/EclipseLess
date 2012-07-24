package org.ilaborie.less.eclipse.functions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

import com.google.common.base.Function;

public class String2IFile implements Function<String, IFile> {

	/** The project. */
	private final IProject project;

	/**
	 * Instantiates a new string2 i file.
	 * 
	 * @param project
	 *            the project
	 */
	public String2IFile(IProject project) {
		super();
		this.project = project;
	}

	public IFile apply(String file) {
		return (file != null) ? this.project.getFile(new Path(file)) : null;
	}

}
