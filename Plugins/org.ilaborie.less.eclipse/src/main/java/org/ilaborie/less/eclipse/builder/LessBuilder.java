package org.ilaborie.less.eclipse.builder;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * The Class LessBuilder.
 */
public class LessBuilder extends IncrementalProjectBuilder {

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The Class SampleDeltaVisitor.
	 */
	private class LessDeltaVisitor implements IResourceDeltaVisitor {

		private final IProgressMonitor monitor;

		public LessDeltaVisitor(IProgressMonitor monitor) {
			super();
			this.monitor = monitor;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					// handle added resource
					LessBuilder.this.buildLess(file, this.monitor);
					break;
				case IResourceDelta.REMOVED:
					LessBuilder.this.deleteMarkers(file);
					// handle removed resource
					break;
				case IResourceDelta.CHANGED:
					// handle changed resource
					LessBuilder.this.buildLess(file, this.monitor);
					break;
				}
			}
			// return true to continue visiting children.
			return true;
		}
	}

	/**
	 * The Class SampleResourceVisitor.
	 */
	private class NatureResourceVisitor implements IResourceVisitor {

		private final IProgressMonitor monitor;

		public NatureResourceVisitor(IProgressMonitor monitor) {
			super();
			this.monitor = monitor;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core
		 * .resources.IResource)
		 */
		public boolean visit(IResource resource) {
			if (resource instanceof IFile) {
				LessBuilder.this.buildLess((IFile) resource, this.monitor);
			}
			// return true to continue visiting children.
			return true;
		}
	}

	/** The Constant BUILDER_ID. */
	public static final String BUILDER_ID = "org.ilaborie.less.eclipse.LessBuilder";

	/** The Constant MARKER_TYPE. */
	private static final String MARKER_TYPE = "org.ilaborie.less.eclipse.lessProblem";

	/** The less exception pattern. */
	private Pattern lessExceptionPattern = Pattern
			.compile(".*\\QSyntax Error on line \\E(.*)");

	/**
	 * Instantiates a new less builder.
	 */
	public LessBuilder() {
		super();
	}

	/**
	 * Adds the error.
	 * 
	 * @param file
	 *            the file
	 * @param message
	 *            the message
	 */
	private void addError(IFile file, String message) {
		addError(file, 1, message);
	}

	/**
	 * Adds the marker.
	 * 
	 * @param file
	 *            the file
	 * @param message
	 *            the message
	 */
	private void addError(IFile file, int line, String message) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
		} catch (CoreException ce) {
			this.log.error(ce.toString(), ce);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		this.log.info("Build Less ...");
		if (kind == FULL_BUILD) {
			this.fullBuild(monitor);
		} else {
			IResourceDelta delta = this.getDelta(this.getProject());
			if (delta == null) {
				this.fullBuild(monitor);
			} else {
				this.incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	/**
	 * Builds the less.
	 * 
	 * @param ifile
	 *            the ifile
	 */
	protected void buildLess(IFile ifile, IProgressMonitor monitor) {
		Less less = new Less();
		less.setProject(ifile.getProject());

		if (less.apply(ifile)) {
			try {

				this.deleteMarkers(ifile);
				this.log.debug("Compile Less for {}", ifile);
				File file = ifile.getRawLocation().makeAbsolute().toFile();
				String name = file.getName();
				String outputName = name.substring(0,
						name.length() - "less".length())
						+ "css";
				File output = new File(file.getParentFile(), outputName);
				this.log.trace("Output file: {}", output);

				// Configure
				LessCompiler compiler = new LessCompiler();
				compiler.setEncoding(ifile.getCharset());
				compiler.setCompress(less.isCompress());

				// Compile
				monitor.subTask("Compile " + ifile.getLocation());
				compiler.compile(file, output, true);

				// Refresh
				this.log.trace("Refresh");
				ifile.getParent().refreshLocal(1, monitor);
			} catch (LessException e) {
				String message = e.getMessage();
				Matcher matcher = lessExceptionPattern.matcher(message);
				if (matcher.matches()) {
					String sLine = matcher.group(1);
					try {
						int line = Integer.valueOf(sLine);
						Throwable cause = e.getCause();
						this.addError(ifile, line, cause.getMessage());
					} catch (NumberFormatException nfe) {
						this.addError(ifile, e.toString());
					}
				} else {
					this.addError(ifile, e.toString());
				}
			} catch (Exception e) {
				this.log.error(e.toString(), e);
			}
		}
	}

	/**
	 * Delete markers.
	 * 
	 * @param file
	 *            the file
	 */
	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
			this.log.error(ce.toString(), ce);
		}
	}

	/**
	 * Full build.
	 * 
	 * @param monitor
	 *            the monitor
	 * @throws CoreException
	 *             the core exception
	 */
	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			this.getProject().accept(new NatureResourceVisitor(monitor));
		} catch (CoreException ce) {
			this.log.error(ce.toString(), ce);
		}
	}

	/**
	 * Incremental build.
	 * 
	 * @param delta
	 *            the delta
	 * @param monitor
	 *            the monitor
	 * @throws CoreException
	 *             the core exception
	 */
	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new LessDeltaVisitor(monitor));
	}
}
