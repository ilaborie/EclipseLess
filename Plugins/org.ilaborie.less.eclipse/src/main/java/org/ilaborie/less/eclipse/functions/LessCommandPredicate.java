package org.ilaborie.less.eclipse.functions;

import org.eclipse.core.resources.ICommand;
import org.ilaborie.less.eclipse.builder.LessBuilder;

import com.google.common.base.Predicate;

/**
 * The Class LessCommandPredicate.
 */
public class LessCommandPredicate implements Predicate<ICommand> {
	public boolean apply(ICommand command) {
		return (command != null)
				&& LessBuilder.BUILDER_ID.equals(command.getBuilderName());
	}
}