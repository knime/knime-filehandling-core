/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   17.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.filechooser;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.node.NodeLogger;

/**
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public final class NioFile extends File {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NioFile.class);

    private static final long serialVersionUID = -5343680976176201255L;

    private final Path m_path;

    private final FileSystem m_fileSys;

    /**
     * Constructs a NioFile from a {@link Path}.
     *
     * @param path the path
     */
    public NioFile(final Path path) {
        super(path.toString());
        m_fileSys = path.getFileSystem();
        m_path = path;
    }

    /**
     * Constructs a NioFile from a path string and a {@link FileSystem}.
     *
     * @param pathname the path as string
     * @param fileSystem the file system this file belongs to
     */
    protected NioFile(final String pathname, final FileSystem fileSystem) {
        super(pathname);
        m_fileSys = fileSystem;
        m_path = m_fileSys.getPath(pathname);
    }

    /**
     * Constructs a NioFile from a {@link File} and a path string for a child and a {@link FileSystem}.
     *
     * @param parent the File for the parent
     * @param child the child string
     * @param fileSystem the file system this file belongs to
     */
    protected NioFile(final File parent, final String child, final FileSystem fileSystem) {
        super(parent, child);
        m_fileSys = fileSystem;
        m_path = m_fileSys.getPath(parent.getPath(), child);
    }

    @Override
    public String getName() {
        return m_path.getFileName().toString();
    }

    @Override
    public boolean isAbsolute() {
        return m_path.isAbsolute();
    }

    @Override
    public boolean exists() {
        return Files.exists(m_path);
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(m_path);
    }

    @Override
    public boolean isFile() {
        return Files.isRegularFile(m_path);
    }

    @Override
    public boolean isHidden() {
        try {
            return Files.isHidden(m_path);
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return getAbsolutePath();
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return new NioFile(getAbsolutePath(), m_fileSys);
    }

    /**
     * @deprecated This method does not automatically escape characters that are illegal in URLs. It is recommended that
     *             new code convert an abstract pathname into a URL by first converting it into a URI, via the
     *             {@link #toURI() toURI} method, and then converting the URI into a URL via the
     *             {@link java.net.URI#toURL() URI.toURL} method.
     */
    @Deprecated
    @Override
    public URL toURL() throws MalformedURLException {
        return toURI().toURL();
    }

    @Override
    public URI toURI() {
        return m_path.toUri();
    }

    @Override
    public File[] listFiles() {

        return listFiles((d, f) -> true);
    }

    @Override
    public File[] listFiles(final FilenameFilter filter) {
        final List<File> files = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(m_path,
            p -> filter.accept(new NioFile(p.getParent()), p.getFileName().toString()))) {

            directoryStream.iterator().forEachRemaining(p -> files.add(new NioFile(p.toString(), m_fileSys)));
        } catch (final Exception ex) {
            LOGGER.warn(ex);
        }
        return files.stream().toArray(NioFile[]::new);
    }

    @Override
    public File[] listFiles(final FileFilter filter) {
        final List<File> files = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream =
            Files.newDirectoryStream(m_path, p -> filter.accept(new NioFile(p)))) {

            directoryStream.iterator().forEachRemaining(p -> files.add(new NioFile(p.toString(), m_fileSys)));
        } catch (final Exception ex) {
            LOGGER.warn(ex);
        }
        return files.stream().toArray(NioFile[]::new);
    }

    @Override
    public String[] list() {
        return list((d, f) -> true);
    }

    @Override
    public String[] list(final FilenameFilter filter) {
        final List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(m_path,
            p -> filter.accept(new NioFile(p.getParent()), p.getFileName().toString()))) {

            directoryStream.iterator().forEachRemaining(p -> files.add(p.getFileName().toString()));
        } catch (final Exception ex) {
            LOGGER.warn(ex);
        }
        return files.stream().toArray(String[]::new);
    }

    @Override
    public String getPath() {
        return m_path.toString();
    }

    @Override
    public boolean canRead() {
        return Files.isReadable(m_path);
    }

    @Override
    public boolean canWrite() {
        return Files.isWritable(m_path);
    }

    @Override
    public long length() {
        try {
            return Files.size(m_path);
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return 0L;
        }
    }

    @Override
    public long lastModified() {
        try {
            return Files.getLastModifiedTime(m_path).toMillis();
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return 0L;
        }
    }

    @Override
    public String getAbsolutePath() {
        return m_path.toAbsolutePath().toString();
    }

    @Override
    public File getAbsoluteFile() {
        return new NioFile(getAbsolutePath(), m_fileSys);
    }

    @Override
    public String getParent() {
        return m_path.getParent() == null ? null : m_path.getParent().toString();
    }

    @Override
    public File getParentFile() {
        return getParent() == null ? null : new NioFile(getParent(), m_fileSys);
    }

    @Override
    public boolean createNewFile() throws IOException {
        try {
            Files.createFile(m_path);
            return true;
        } catch (final FileAlreadyExistsException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean mkdir() {
        try {
            Files.createDirectory(m_path);
            return true;
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean mkdirs() {
        try {
            Files.createDirectories(m_path);
            return true;
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean renameTo(final File dest) {

        final Path nioDest = m_fileSys.getPath(dest.getAbsolutePath());
        try {
            Files.move(m_path, nioDest);
            return true;
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }

    }

    @Override
    public boolean setLastModified(final long time) {
        try {
            Files.setLastModifiedTime(m_path, FileTime.fromMillis(time));
            return true;
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean setReadOnly() {
        try {

            final Set<PosixFilePermission> newPermissions = new HashSet<>();
            newPermissions.add(PosixFilePermission.OWNER_READ);
            newPermissions.add(PosixFilePermission.GROUP_READ);
            newPermissions.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(m_path, newPermissions);

            return true;

        } catch (final Exception ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean setReadable(final boolean readable, final boolean ownerOnly) {
        try {

            final PosixFileAttributes attrs = Files.readAttributes(m_path, PosixFileAttributes.class);
            final Set<PosixFilePermission> newPermissions = attrs.permissions();
            newPermissions.add(PosixFilePermission.OWNER_READ);
            if (!ownerOnly) {
                newPermissions.add(PosixFilePermission.GROUP_READ);
                newPermissions.add(PosixFilePermission.OTHERS_READ);
            }
            Files.setPosixFilePermissions(m_path, newPermissions);
            return true;

        } catch (final Exception ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean setReadable(final boolean readable) {
        return setReadable(readable, true);
    }

    @Override
    public boolean setWritable(final boolean writable, final boolean ownerOnly) {
        try {
            final PosixFileAttributes attrs = Files.readAttributes(m_path, PosixFileAttributes.class);
            final Set<PosixFilePermission> newPermissions = attrs.permissions();
            newPermissions.add(PosixFilePermission.OWNER_WRITE);
            if (!ownerOnly) {
                newPermissions.add(PosixFilePermission.GROUP_WRITE);
                newPermissions.add(PosixFilePermission.OTHERS_WRITE);
            }
            Files.setPosixFilePermissions(m_path, newPermissions);

            return true;

        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean setWritable(final boolean writable) {
        return setWritable(writable, true);
    }

    @Override
    public boolean setExecutable(final boolean executable, final boolean ownerOnly) {
        try {
            final PosixFileAttributes attrs = Files.readAttributes(m_path, PosixFileAttributes.class);
            final Set<PosixFilePermission> newPermissions = attrs.permissions();
            newPermissions.add(PosixFilePermission.OWNER_EXECUTE);
            if (!ownerOnly) {
                newPermissions.add(PosixFilePermission.GROUP_EXECUTE);
                newPermissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            return true;
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public boolean setExecutable(final boolean executable) {
        return setExecutable(executable, true);
    }

    @Override
    public boolean canExecute() {
        return Files.isExecutable(m_path);
    }

    @Override
    public long getTotalSpace() {
        try {
            return Files.getFileStore(m_path).getTotalSpace();
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return 0L;
        }
    }

    @Override
    public long getUsableSpace() {
        try {
            return Files.getFileStore(m_path).getUsableSpace();
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return 0L;
        }
    }

    @Override
    public long getFreeSpace() {
        try {
            return Files.getFileStore(m_path).getUnallocatedSpace();
        } catch (final IOException ex) {
            LOGGER.warn(ex);
            return 0L;
        }
    }

    @Override
    public boolean delete() {
        try {
            Files.delete(m_path);
            return true;
        } catch (final Exception ex) {
            LOGGER.warn(ex);
            return false;
        }
    }

    @Override
    public int compareTo(final File pathname) {
        final Path other = m_fileSys.getPath(pathname.getAbsolutePath());
        return m_path.compareTo(other);
    }

    @Override
    public boolean equals(final Object obj) {
        return m_path.equals(obj);
    }

    @Override
    public int hashCode() {
        return m_path.hashCode();
    }

    @Override
    public Path toPath() {
        return m_path;
    }

    @SuppressWarnings("static-method")
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    @SuppressWarnings("static-method")
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.readObject();
    }

}
