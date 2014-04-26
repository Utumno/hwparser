package snippet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.EnumSet;

public class CopyDirectory {

	enum Attributes {
		ACL(AclFileAttributeView.class) {

			@Override
			void copyAttrs(Path from, Path to) throws IOException {
				AclFileAttributeView acl = (AclFileAttributeView) getAttrs(from);
				if (acl == null) return;
				((AclFileAttributeView) getAttrs(to)).setAcl(acl.getAcl());
			}
		},
		DOS(DosFileAttributeView.class) {

			@Override
			void copyAttrs(Path from, Path to) throws IOException {
				final DosFileAttributeView fromAttrs = (DosFileAttributeView) getAttrs(from);
				if (fromAttrs == null) return;
				DosFileAttributes sourceDosAttrs = fromAttrs.readAttributes();
				DosFileAttributeView targetDosAttrs = (DosFileAttributeView) getAttrs(to);
				// Files.getFileAttributeView(to, fa); // errs:
				// "Cannot make a static reference to the non-static field fa"
				// TODO why ?? (private --> default solves the problem)
				targetDosAttrs.setArchive(sourceDosAttrs.isArchive());
				targetDosAttrs.setHidden(sourceDosAttrs.isHidden());
				targetDosAttrs.setReadOnly(sourceDosAttrs.isReadOnly());
				targetDosAttrs.setSystem(sourceDosAttrs.isSystem());
			}
		},
		FOA(FileOwnerAttributeView.class) {

			@Override
			void copyAttrs(Path from, Path to) throws IOException {
				FileOwnerAttributeView fow = (FileOwnerAttributeView) getAttrs(from);
				if (fow == null) return;
				FileOwnerAttributeView targetOwner = (FileOwnerAttributeView) getAttrs(to);
				targetOwner.setOwner(fow.getOwner());
			}
		},
		POS(PosixFileAttributeView.class) {

			@Override
			void copyAttrs(Path from, Path to) throws IOException {
				final PosixFileAttributeView fromAttrs = (PosixFileAttributeView) getAttrs(from);
				if (fromAttrs == null) return;
				PosixFileAttributes sourcePosix = fromAttrs.readAttributes();
				PosixFileAttributeView targetPosix = (PosixFileAttributeView) getAttrs(to);
				targetPosix.setPermissions(sourcePosix.permissions());
				targetPosix.setGroup(sourcePosix.group());
			}
		},
		UDF(UserDefinedFileAttributeView.class) {

			@Override
			void copyAttrs(Path from, Path to) throws IOException {
				final UserDefinedFileAttributeView fromAttrs = (UserDefinedFileAttributeView) getAttrs(from);
				if (fromAttrs == null) return;
				UserDefinedFileAttributeView targetUser = (UserDefinedFileAttributeView) getAttrs(to);
				for (String key : fromAttrs.list()) {
					ByteBuffer buffer = ByteBuffer
						.allocate(fromAttrs.size(key));
					fromAttrs.read(key, buffer);
					buffer.flip();
					targetUser.write(key, buffer);
				}
			}
		};

		private final Class<? extends FileAttributeView> fa;

		private Attributes(Class<? extends FileAttributeView> fa) {
			this.fa = fa;
		}

		FileAttributeView getAttrs(Path dir) {
			return Files.getFileAttributeView(dir, fa);
		}

		abstract void copyAttrs(Path from, Path to) throws IOException;
	}

	/**
	 * Copies a directory. NOTE: This method is not thread-safe.
	 *
	 * @param source
	 *            the directory to copy from
	 * @param target
	 *            the directory to copy into
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static void copyDirectory(final Path source, final Path target)
			throws IOException {
		Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
			Integer.MAX_VALUE, new CopyDirectoryVisitor(source, target));
	}

	private static final class CopyDirectoryVisitor extends
			SimpleFileVisitor<Path> {

		private final Path source;
		private final Path target;

		private CopyDirectoryVisitor(Path source, Path target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes sourceBasic) throws IOException {
			Path targetDir = Files.createDirectories(target.resolve(source
				.relativize(dir)));
			for (Attributes attr : Attributes.values()) {
				attr.copyAttrs(dir, targetDir);
			}
			// Must be done last, otherwise last-modified time may be
			// wrong
			BasicFileAttributeView targetBasic = Files.getFileAttributeView(
				targetDir, BasicFileAttributeView.class);
			targetBasic.setTimes(sourceBasic.lastModifiedTime(),
				sourceBasic.lastAccessTime(), sourceBasic.creationTime());
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.copy(file, target.resolve(source.relativize(file)),
				StandardCopyOption.COPY_ATTRIBUTES);
			return FileVisitResult.CONTINUE;
		}
	}
}
