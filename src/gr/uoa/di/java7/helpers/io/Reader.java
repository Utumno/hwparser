package gr.uoa.di.java7.helpers.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class Reader {

	private Reader() {}

	/** http://stackoverflow.com/a/326440/281545 */
	public static String readFile(Path path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded, encoding);
	}
}
