package gr.uoa.di.java7.helpers.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileVisitors {

	private FileVisitors() {}

	// =========================================================================
	// API
	// =========================================================================
	public static void removeRecursive(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitorExtension());
	}

	/**
	 * Copies recursively the subtree rooted at fromPath inside toPath
	 * _including fromPath_. So in the destination we have
	 * toPath/fromPath/<contents of fromPath>.
	 */
	public static void copyRecursive(Path fromPath, Path toPath,
			CopyOption... copyOption) throws IOException {
		Files.walkFileTree(fromPath, new CopyFileVisitor(toPath, copyOption));
	}

	/**
	 * Copies recursively the subtree rooted at fromPath inside toPath
	 * _excluding fromPath_. So in the destination we have toPath/<contents of
	 * fromPath>.
	 */
	public static void copyRecursiveContents(Path fromPath, Path toPath,
			CopyOption... copyOption) throws IOException {
		Files.walkFileTree(fromPath, new CopyDirVisitor(fromPath, toPath,
			copyOption));
	}

	public static void gitInit(Path path) throws IOException {
		Files.walkFileTree(path, new GitCommandVisitor());
	}

	/** http://stackoverflow.com/a/8685959/281545 */
	private static final class SimpleFileVisitorExtension extends
			SimpleFileVisitor<Path> {

		final Logger logger = LoggerFactory.getLogger(this.getClass());

		SimpleFileVisitorExtension() {}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			logger.warn("Deleting " + file.getFileName());
			Files.delete(file);
			logger.warn("DELETED " + file.getFileName());
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			// try to delete the file anyway, even if its attributes
			// could not be read, since delete-only access is
			// theoretically possible
			logger.warn("Delete file " + file + " failed", exc);
			try {
				Files.delete(file);
			} catch (IOException e) {
				logger.warn("Delete file " + file + " failed again", exc);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			if (exc == null) {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			// directory iteration failed; propagate exception
			throw exc;
		}
	}

	private static final class GitCommandVisitor extends
			SimpleFileVisitor<Path> {

		GitCommandVisitor() {}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attrs) throws IOException {
			Repository repo = new FileRepository(FileSystems.getDefault()
				.getPath(dir.toString(), ".git").toFile());
			repo.create();
			Git git = new Git(repo);
			try {
				// TODO Files.copy git files here
				git.add().addFilepattern(".").call();
				// Thread.yield(); // no help TODO there are changes still not
				// committed after I run git add !!!!!!!!!!
				git.commit().setMessage("Initial commit")
					.setAuthor("MrD", "iwonttell@gmail.com").call();
			} catch (GitAPIException e) {
				// TODO ...
				e.printStackTrace();
			} finally {
				repo.close();
			}
			return FileVisitResult.SKIP_SUBTREE;
		}
	}

	/**
	 * <pre>
	 * File src = new File(&quot;c:\\temp\\srctest&quot;);
	 * File dest = new File(&quot;c:\\temp\\desttest&quot;);
	 * Path srcPath = src.toPath();
	 * Path destPath = dest.toPath();
	 * Files.walkFileTree(srcPath, new CopyDirVisitor(srcPath, destPath,
	 * 	StandardCopyOption.REPLACE_EXISTING));
	 * </pre>
	 */
	private static class CopyDirVisitor extends SimpleFileVisitor<Path> {

		private final Path sourcePath;
		private final Path toPath;
		private final CopyOption[] copyOption;

		public CopyDirVisitor(Path fromPath, Path toPath,
				CopyOption[] copyOption) {
			this.sourcePath = fromPath;
			this.toPath = toPath;
			this.copyOption = copyOption;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attrs) throws IOException {
			Path targetPath = toPath.resolve(sourcePath.relativize(dir));
			if (!Files.exists(targetPath)) {
				Files.createDirectory(targetPath);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.copy(file, toPath.resolve(sourcePath.relativize(file)),
				copyOption);
			return FileVisitResult.CONTINUE;
		}
	}

	/** http://stackoverflow.com/a/10068306/281545 */
	private static final class CopyFileVisitor extends SimpleFileVisitor<Path> {

		private Path targetPath;
		private Path sourcePath = null;
		private final CopyOption[] copyOption;

		public CopyFileVisitor(Path targetPath, CopyOption[] copyOption2) {
			this.targetPath = targetPath;
			this.copyOption = copyOption2;
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path dir,
				final BasicFileAttributes attrs) throws IOException {
			if (sourcePath == null) {
				sourcePath = dir;
				final Path newTarget = targetPath.resolve(sourcePath
					.getFileName());
				Files.createDirectories(newTarget);
				targetPath = newTarget;
			} else {
				Files.createDirectories(targetPath.resolve(sourcePath
					.relativize(dir)));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final Path file,
				final BasicFileAttributes attrs) throws IOException {
			Files.copy(file, targetPath.resolve(sourcePath.relativize(file)),
				copyOption);
			return FileVisitResult.CONTINUE;
		}
	}

	@SuppressWarnings("unused")
	private static final class IOCopier {

		public static void joinFiles(File destination, File[] sources)
				throws IOException {
			OutputStream output = null;
			try {
				output = createAppendableStream(destination);
				for (File source : sources) {
					appendFile(output, source);
				}
			} finally {
				IOUtils.closeQuietly(output);
			}
		}

		private static BufferedOutputStream createAppendableStream(
				File destination) throws FileNotFoundException {
			return new BufferedOutputStream(new FileOutputStream(destination,
				true));
		}

		private static void appendFile(OutputStream output, File source)
				throws IOException {
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(source));
				IOUtils.copy(input, output);
			} finally {
				IOUtils.closeQuietly(input);
			}
		}
	}

	private static final class IOUtils {

		private static final int BUFFER_SIZE = 1024 * 4;

		public static long copy(InputStream input, OutputStream output)
				throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			long count = 0;
			for (int n = 0; -1 != (n = input.read(buffer));) {
				output.write(buffer, 0, n);
				count += n;
			}
			return count;
		}

		public static void closeQuietly(Closeable output) {
			try {
				if (output != null) output.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
