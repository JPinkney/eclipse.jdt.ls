/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beryozkin <eclipse@genady.org> - [misc] Display values for constant fields in the Javadoc view - https://bugs.eclipse.org/bugs/show_bug.cgi?id=204914
 *     Brock Janiczak <brockj@tpg.com.au> - [implementation] Streams not being closed in Javadoc views - https://bugs.eclipse.org/bugs/show_bug.cgi?id=214854
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [javadoc view] NPE on enumerations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=223586
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jface.text.Region;

import com.google.common.io.CharStreams;

/**
 * View which shows Javadoc for a given Java element.
 *
 * FIXME: As of 3.0 selectAll() and getSelection() is not working see
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
 *
 * @since 3.0
 */
public class JavadocView {

	public static Object computeInput(IJavaElement input) {
		if (input == null) {
			return null;
		}

		String javadocHtml;

		switch (input.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				try {
					IType[] types = ((IType) input).getTypes();
					if (types.length == 0 && JavaModelUtil.isPackageInfo((org.eclipse.jdt.core.ICompilationUnit) input)) {
						javadocHtml = getJavadocHtml(new IJavaElement[] { input.getParent() });
					} else {
						javadocHtml = getJavadocHtml(types);
					}
				} catch (JavaModelException ex) {
					javadocHtml = null;
				}
				break;
			case IJavaElement.CLASS_FILE:
				if (JavaModelUtil.PACKAGE_INFO_CLASS.equals(input.getElementName())) {
					javadocHtml = getJavadocHtml(new IJavaElement[] { input.getParent() });
				} else if (input instanceof IModularClassFile) {
					try {
						javadocHtml = getJavadocHtml(new IJavaElement[] { ((IModularClassFile) input).getModule() });
					} catch (JavaModelException e) {
						return null;
					}
				} else {
					javadocHtml = getJavadocHtml(new IJavaElement[] { ((IOrdinaryClassFile) input).getType() });
				}
				break;
			default:
				javadocHtml = getJavadocHtml(new IJavaElement[] { input });
		}

		return javadocHtml;
	}

	/**
	 * Returns the Javadoc of the Java element in HTML format.
	 *
	 * @param result
	 *            the Java elements for which to get the Javadoc
	 * @return a string with the Javadoc in HTML format, or <code>null</code> if
	 *         none
	 */
	private static String getJavadocHtml(IJavaElement[] result) {
		StringBuilder buffer = new StringBuilder();
		int nResults = result.length;

		if (nResults == 0) {
			return null;
		}

		String base = null;
		if (nResults > 1) {
			for (int i = 0; i < result.length; i++) {
				//				HTMLPrinter.startBulletList(buffer);
				//				IJavaElement curr = result[i];
				//				if (curr instanceof IMember || curr instanceof IPackageFragment || curr instanceof IPackageDeclaration || curr.getElementType() == IJavaElement.LOCAL_VARIABLE) {
				//					HTMLPrinter.addBullet(buffer, getInfoText(curr, null, null, false));
				//					HTMLPrinter.endBulletList(buffer);
				//				}
			}
		} else {
			IJavaElement curr = result[0];
			if (curr instanceof IPackageDeclaration || curr instanceof IPackageFragment) {
				//HTMLPrinter.addSmallHeader(buffer, getInfoText(curr, null, null, true));
				buffer.append("<br>"); //$NON-NLS-1$
				Reader reader = null;
				String content = null;
				try {
					if (curr instanceof IPackageDeclaration) {
						try {
							ISourceRange nameRange = ((IPackageDeclaration) curr).getNameRange();
							if (SourceRange.isAvailable(nameRange)) {
								ITypeRoot typeRoot = (ITypeRoot) ((IPackageDeclaration) curr).getParent();
								Region hoverRegion = new Region(nameRange.getOffset(), nameRange.getLength());
								//JavadocHover.addAnnotations(buffer, typeRoot.getParent(), typeRoot, hoverRegion);
							}
						} catch (JavaModelException e) {
							// no annotations this time...
						}

						content = JavadocContentAccess2.getHTMLContent((IPackageDeclaration) curr);
					} else if (curr instanceof IPackageFragment) {
						//JavadocHover.addAnnotations(buffer, curr, null, null);
						content = JavadocContentAccess2.getHTMLContent((IPackageFragment) curr);
					}
				} catch (CoreException e) {
					//reader = new StringReader(JavaDocLocations.handleFailedJavadocFetch(e));
				}
				IPackageFragmentRoot root = (IPackageFragmentRoot) curr.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				try {
					boolean isBinary = root.getKind() == IPackageFragmentRoot.K_BINARY;
					if (content != null) {
						base = JavadocContentAccess2.extractBaseURL(content);
						if (base == null) {
							base = JavaDocLocations.getBaseURL(curr, isBinary);
						}
						reader = new StringReader(content);
					} else if (reader == null) {
						//						String explanationForMissingJavadoc = JavaDocLocations.getExplanationForMissingJavadoc(curr, root);
						//						if (explanationForMissingJavadoc != null) {
						//							reader = new StringReader(explanationForMissingJavadoc);
						//						}
					}
				} catch (JavaModelException e) {
					//					reader = new StringReader(InfoViewMessages.JavadocView_error_gettingJavadoc);
					//					JavaPlugin.log(e);
				}
				if (reader != null) {
					//HTMLPrinter.addParagraph(buffer, reader);
				}
			} else if (curr instanceof IMember || curr instanceof ILocalVariable || curr instanceof ITypeParameter) {
				final IJavaElement element = curr;

				ITypeRoot typeRoot = null;
				Region hoverRegion = null;
				try {
					ISourceRange nameRange = ((ISourceReference) curr).getNameRange();
					if (SourceRange.isAvailable(nameRange)) {
						if (element instanceof ILocalVariable) {
							typeRoot = ((ILocalVariable) curr).getTypeRoot();
						} else if (element instanceof ITypeParameter) {
							typeRoot = ((ITypeParameter) curr).getTypeRoot();
						} else {
							typeRoot = ((IMember) curr).getTypeRoot();
						}
						hoverRegion = new Region(nameRange.getOffset(), nameRange.getLength());
					}
				} catch (JavaModelException e) {
					// no annotations this time...
				}

				String constantValue = null;
				if (element instanceof IField) {
					//					constantValue = computeFieldConstant(activePart);
					//					if (constantValue != null) {
					//						//constantValue= HTMLPrinter.convertToHTMLContentWithWhitespace(constantValue);
					//					}
				}

				String defaultValue = null;
				if (element instanceof IMethod) {
					//					try {
					//						defaultValue = JavadocHover.getAnnotationMemberDefaultValue((IMethod) element, typeRoot, hoverRegion);
					//						if (defaultValue != null) {
					//							defaultValue = HTMLPrinter.convertToHTMLContentWithWhitespace(defaultValue);
					//						}
					//					} catch (JavaModelException e) {
					//						// no default value
					//					}
				}

				//				HTMLPrinter.addSmallHeader(buffer, getInfoText(element, constantValue, defaultValue, true));
				//
				//				if (typeRoot != null && hoverRegion != null) {
				//					buffer.append("<br>"); //$NON-NLS-1$
				//					JavadocHover.addAnnotations(buffer, curr, typeRoot, hoverRegion);
				//				}

				Reader reader = null;
				try {
					String content = JavadocContentAccess2.getHTMLContent(element, true);
					IPackageFragmentRoot root = (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (content != null) {
						IMember member;
						if (element instanceof ILocalVariable) {
							member = ((ILocalVariable) element).getDeclaringMember();
						} else if (element instanceof ITypeParameter) {
							member = ((ITypeParameter) element).getDeclaringMember();
						} else {
							member = (IMember) element;
						}
						base = JavadocContentAccess2.extractBaseURL(content);
						if (base == null) {
							base = JavaDocLocations.getBaseURL(member, member.isBinary());
						}
						reader = new StringReader(content);
					} else {
						//						String explanationForMissingJavadoc = JavaDocLocations.getExplanationForMissingJavadoc(element, root);
						//						if (explanationForMissingJavadoc != null) {
						//							reader = new StringReader(explanationForMissingJavadoc);
						//						}
					}
				} catch (CoreException ex) {
					//reader = new StringReader(JavaDocLocations.handleFailedJavadocFetch(ex));
				}
				if (reader != null) {
					//HTMLPrinter.addParagraph(buffer, reader);
					try {
						return CharStreams.toString(reader);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}

		if (buffer.length() == 0) {
			return null;
		}

		//HTMLPrinter.insertPageProlog(buffer, 0, fForegroundColorRGB, fBackgroundColorRGB, fgStyleSheet);
		if (base != null) {
			int endHeadIdx = buffer.indexOf("</head>"); //$NON-NLS-1$
			buffer.insert(endHeadIdx, "\n<base href='" + base + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		//HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

}
