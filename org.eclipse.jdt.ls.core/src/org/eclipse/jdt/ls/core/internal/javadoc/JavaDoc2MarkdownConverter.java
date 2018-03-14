/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.javadoc;

import java.io.Reader;

import com.overzealous.remark.Options;
import com.overzealous.remark.Options.Tables;

/**
 * Converts JavaDoc tags into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class JavaDoc2MarkdownConverter extends AbstractJavaDocConverter {

	private static RemarkExtension remark;

	static {
		Options options = new Options();
		options.tables = Tables.CONVERT_TO_CODE_BLOCK;
		options.hardwraps = true;
		options.inlineLinks = true;
		options.autoLinks = true;
		options.reverseHtmlSmartPunctuation = true;
		remark = new RemarkExtension(options);
	}
	public JavaDoc2MarkdownConverter(Reader reader) {
		super(reader);
	}


	public JavaDoc2MarkdownConverter(String javadoc) {
		super(javadoc);
	}

	@Override
	String convert(String rawHtml) {
		return remark.convert(rawHtml);
	}
}
