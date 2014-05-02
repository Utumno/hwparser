package gr.uoa.di.helpers.eclipse;

import gr.uoa.di.java7.helpers.io.Reader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ProjectFiles {

	private static final String CPROJECT;
	private static final String PROJECT;
	private static final String CPROJECT_FILENAME = ".cproject";
	private static final String PROJECT_FILENAME = ".project";
	private static final Charset ECLIPSE_FILES_CHARSET = StandardCharsets.UTF_8;
	private static final String TEMPLATES_DIR = "C:\\dropbox\\"
		+ "eclipse_workspaces\\_\\_templates";
	static {
		CPROJECT = templateString(CPROJECT_FILENAME);
		PROJECT = templateString(PROJECT_FILENAME);
	}

	private ProjectFiles() {}

	public static void createCProjectFile(String directoryPath,
			String projectName) throws IOException {
		String cproj = String.format(CPROJECT, projectName);
		Path filePath = Paths.get(directoryPath, CPROJECT_FILENAME);
		Files.write(filePath, cproj.getBytes(ECLIPSE_FILES_CHARSET),
			StandardOpenOption.CREATE);
	}

	public static void createProjectFile(String directoryPath,
			String projectName) throws IOException {
		String cproj = String.format(PROJECT, projectName);
		Path filePath = Paths.get(directoryPath, PROJECT_FILENAME);
		Files.write(filePath, cproj.getBytes(ECLIPSE_FILES_CHARSET),
			StandardOpenOption.CREATE);
	}

	// =========================================================================
	// Helpers
	// =========================================================================
	private static String templateString(final String filename) {
		try {
			Path path = Paths.get(TEMPLATES_DIR, filename);
			return Reader.readFile(path, ECLIPSE_FILES_CHARSET);
		} catch (IOException e) {
			throw new RuntimeException("Construction of the eclipse template "
				+ filename + " file failed", e);
		}
	}
}
