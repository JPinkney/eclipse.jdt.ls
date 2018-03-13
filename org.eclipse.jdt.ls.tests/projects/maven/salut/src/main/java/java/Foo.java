package java;

import org.apache.commons.lang3.StringUtils;

/**
 * This is
 * {@link String}
 */
public class Foo {

	public static void main(String[] args) {
	System.out.print( StringUtils.capitalize("Hello world! from "+Foo.class));
	
	System.out.println(StringUtils.trimToNull(""));
	}

	/**
	 * This is
	 * {@link invoke}
	 */
	public static void test(){

	}
}