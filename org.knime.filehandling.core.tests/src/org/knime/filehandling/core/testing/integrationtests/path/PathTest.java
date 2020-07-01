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
 */
package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
import org.knime.filehandling.core.util.IOESupplier;

public class PathTest extends AbstractParameterizedFSTest {

    public PathTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void testFileName() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals("file", path.getFileName().toString());
    }

    @Test
    public void testNameCount() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromString() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "", "to", "", "file"));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromStringSeveralSeparatorsTrailing() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "to", "file", "", ""));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testRealtivize() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRealtivizeAbsolutToRelative() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("/de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParent() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/fg/../../");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath2() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("../../../abc/de/fg/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithEmptyPath() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "as/de/";
        final String other = "";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        final Path backForth = path.relativize(path.resolve(other));
        assertEquals(otherPath, backForth);
    }

    @Test
    public void testRelativizeAgainstRootPath() {
        final FileSystem fileSystem = getFileSystem();
        final Path root = fileSystem.getRootDirectories().iterator().next();
        final Path somePath = root.resolve("/some/path");
        final Path relativePath = root.relativize(somePath);
        assertTrue(somePath.isAbsolute());
        assertFalse(relativePath.isAbsolute());
        assertEquals(fileSystem.getPath("some/path"), relativePath);
    }

    @Test
    public void testRealtivizeRealtiveEmptyPath() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "";
        final String other = "as/de/";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        final Path backForth = path.relativize(path.resolve(other));
        assertEquals(otherPath, backForth);
    }

    @Test
    public void testRelativizeRelativePathToEmpty() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/cd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath(""), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeRelativePathSimple() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/xy";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeAbsolutePathSimple() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/xy";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeRelativePathWithDotDot() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/xy/../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy/../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeAbsolutePathWithDotDot() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/xy/../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy/../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeStartsWith() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/cd/../../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeStartsWith2() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd/../../xv";
        final String other = "/ab/cd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../../.."), path.relativize(otherPath));
    }

    @Test
    public void testNormalize() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/a/b/../../abc/././de");
        final Path normalizedPath = fileSystem.getPath("/abc/de");

        assertEquals(normalizedPath, path.normalize());
    }

    @Test
    public void testNormalizeAbsolute() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/../b");
        final Path normalizedPath = fileSystem.getPath("/b");

        assertEquals(normalizedPath, path.normalize());
    }

    @Test
    public void testNormalizeAbsolute2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/../../b");
        final Path normalizedPath = fileSystem.getPath("/b");

        assertEquals(normalizedPath, path.normalize());
    }

    @Test
    public void testNormalizeRelative() {
        final FileSystem fileSystem = getFileSystem();
        final String separator = fileSystem.getSeparator();
        final Path path = fileSystem.getPath("../b");
        final Path oriPath = fileSystem.getPath("../b");

        assertEquals(oriPath, path.normalize());
    }

    @Test
    public void testNormalizeRelative2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("../../b");
        final Path oriPath = fileSystem.getPath("../../b");

        assertEquals(oriPath, path.normalize());
    }

    @Test
    public void testNormalizeToEmpty() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("de/..");

        assertEquals(fileSystem.getPath(""), path.normalize());
    }

    @Test
    public void testPathEquals() {
        final FileSystem fileSystem = getFileSystem();
        final Path first = fileSystem.getPath("some-dir", "first-file.txt");
        final Path sameFirst = fileSystem.getPath("some-dir", "first-file.txt");
        final Path second = fileSystem.getPath("some-dir", "other-file.txt");
        final Path third = fileSystem.getPath("other-dir", "first-file.txt");

        assertEquals(first, first);
        assertEquals(first, sameFirst);
        assertNotEquals(first, second);
        assertNotEquals(first, third);
        assertNotEquals(second, third);
    }

    public void testNormalizeToEmpty2() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals("", path.normalize().toString());
    }

    public void testNormalizeToEmpty2BlobStore() {
        ignoreAllExcept(S3, GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals("/", path.normalize().toString());
    }

    @Test
    public void testEquals() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "as/de/";
        final String other = "as/de";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(path, otherPath);
    }

    @Test
    public void testNotEqualRealtiveAndAbsolutPath() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/abcd";
        final String other = "abcd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertNotEquals(path, otherPath);
    }

    @Test
    public void testGetParent() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/a/b/c/d");

        assertEquals(fileSystem.getPath("/a/b/c/"), path.getParent());
    }

    @Test
    public void testGetParentRelative() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("a/b/c/d");

        assertEquals(fileSystem.getPath("a/b/c/"), path.getParent());
    }

    @Test
    public void testGetParentEmptyPath() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentDot() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentDotSlash() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("./");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentAbsolutToNull2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetName() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/abc/de/fg");

        assertEquals(fileSystem.getPath("abc"), path.getName(0));
        assertEquals(fileSystem.getPath("de"), path.getName(1));
        assertEquals(fileSystem.getPath("fg"), path.getName(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNameNonexistent() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/abc");

        path.getName(1).toString();
    }

    @Test
    public void testGetRootSimple() {
        final Path path = m_testInitializer.makePath("a", "b");
        final Set<Path> roots = new HashSet<>();
        getFileSystem().getRootDirectories().forEach(roots::add);
        assertTrue(roots.contains(path.getRoot()));
    }

    @Test
    public void testGetRootNull() {
        // a relative path should have a null root
        final Path path = getFileSystem().getPath("a", "b");
        assertNull(path.getRoot());
    }
}
