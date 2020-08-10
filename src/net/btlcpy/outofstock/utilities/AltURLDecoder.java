package net.btlcpy.outofstock.utilities;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

/**
 * <p>
 * The class java.net.URLDecoder is sadly broken in Java 1.3 (NOT later versions) 
 * because to decode application/x-www-form-urlencoded MIME format strings, it assumes 
 * the charset is the charset of the computer which is sometimes OK, but not always. This
 * problem was fixed in Java 1.4 - but Bottling Company's installed software base as of 09/05/07
 * utilizes Java 1.3.
 * </p>
 * <p>
 * Hence we need to use an alternative to Java 1.4 URLDecoder, and that alternative is found
 * in the typically excellent Apache libraries - many of them have indeed been written to
 * solve these problems with earlier versions of Java.
 * </p>
 * <p>
 * The Apache solution provides a decode method at the object level; this class provides a
 * thin wrapper around a single object and allows static access (similar to Java 1.4).
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class AltURLDecoder
{
	static private URLCodec urlCodec = new URLCodec("UTF-8");

	/**
	 * "URL-decodes" the passed in string using the passed in encoding (typically UTF-8).
	 * 
	 * @param s the string to decode
	 * @param enc the character set encoding of the string
	 * @return the decoded string
	 * @throws DecoderException
	 * @throws UnsupportedEncodingException
	 */
	static public String decode(String s, String enc)
		throws DecoderException, UnsupportedEncodingException
	{
		return urlCodec.decode(s, enc);
	}
}


