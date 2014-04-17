package gr.uoa.di.hw.helpers;

import gr.uoa.di.helpers.eclipse.ProjectFiles;
import gr.uoa.di.java7.helpers.io.FileVisitors;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class Setup {

	List<Sdi> sdis;
	private static final Path GIT_DEFAULT = FileSystems.getDefault().getPath(
		"C:\\dropbox\\eclipse_workspaces\\_\\_templates\\_git");

	// List
	static class Sdi {

		private long _sdi;

		public static Sdi from(String s) throws Sdi.ParserException {
			final Sdi sdi = new Sdi();
			if (s.split("sdi").length == 2) sdi._sdi = Long.valueOf(s
				.split("sdi")[1]);
			else if (s.split("1115").length == 2) sdi._sdi = Long.valueOf(s
				.split("sdi")[1]);
			else throw new Sdi.ParserException(s);
			return sdi;
		}

		static final class ParserException extends Exception {

			private static final long serialVersionUID = -2878114791066924687L;

			ParserException(String s) {
				super("Sdi::Can't parse " + s + " as an sdi.");
			}
		}
	}

	public static void main(String[] args) throws IOException {
		int i = -1;
		String hwDirName = args[++i];
		String destDirName = args[++i];
		String hwFileName = args[++i];
		String lastDigit = args[++i];
		Path hwDirPath = FileSystems.getDefault().getPath(hwDirName);
		final List<Path> filteredDirs = new ArrayList<>();
		for (Path entry : Files.newDirectoryStream(hwDirPath)) {
			if (checkAndLog(entry, hwFileName, lastDigit))
				filteredDirs.add(entry);
		}
		if (filteredDirs.isEmpty()) {
			log("No hw directories");
			return;
		}
		Path destDirPath = FileSystems.getDefault().getPath(destDirName);
		for (Path path : filteredDirs) {
			FileVisitors.copyRecursive(path, destDirPath,
				StandardCopyOption.COPY_ATTRIBUTES);
			final String projectName = path.getFileName().toString();
			Path copied = FileSystems.getDefault().getPath(destDirName,
				projectName);
			FileVisitors.copyRecursiveContents(GIT_DEFAULT, copied,
				StandardCopyOption.COPY_ATTRIBUTES);
			ProjectFiles.createCProjectFile(copied.toString(), projectName
				+ "_hw1");
			ProjectFiles.createProjectFile(copied.toString(), projectName
				+ "_hw1");
			FileVisitors.gitInit(copied);
		}
	}

	private static boolean checkAndLog(Path entry, String hwFileName,
			String lastDigit) throws IOException {
		final String abPathToHwDir = entry.toAbsolutePath().toString();
		if (!entry.toFile().isDirectory()) {
			log("Not a directory: " + abPathToHwDir);
			return false;
		}
		final String dirName = entry.getFileName().toString();
		try {
			Sdi.from(dirName);
		} catch (Sdi.ParserException e) {
			log(e.getMessage());
			return false;
		}
		if (!lastDigit.equals(dirName.substring(dirName.length() - 1,
			dirName.length()))) {
			// skip the sdi's that are not for me
			return false;
		}
		log("Directory: " + abPathToHwDir);
		for (Path sdi : Files.newDirectoryStream(entry)) {
			Path fileName = sdi.getFileName();
			if (!fileName.toString().equals(hwFileName)) {
				log("\tUFO item: " + fileName);
				continue;
			}
		}
		return true;
	}

	private static void log(String msg) {
		System.out.println(msg);
	}
}
