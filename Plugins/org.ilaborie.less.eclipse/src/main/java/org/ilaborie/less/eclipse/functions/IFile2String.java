package org.ilaborie.less.eclipse.functions;

import org.eclipse.core.resources.IFile;

import com.google.common.base.Function;

public class IFile2String implements Function<IFile, String> {

	public String apply(IFile file) {
		return (file != null) ? file.getProjectRelativePath().toString() : null;
	}

}
