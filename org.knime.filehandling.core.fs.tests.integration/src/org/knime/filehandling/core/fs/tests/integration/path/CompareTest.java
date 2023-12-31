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
package org.knime.filehandling.core.fs.tests.integration.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

public class CompareTest extends AbstractParameterizedFSTest {

    public CompareTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void testCompareRoot() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", AMAZON_S3);
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", GENERIC_S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GOOGLE_CS);
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String sep = fileSystem.getSeparator();
        final String that = sep;
        final String other = "";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareDifferent() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String sep = fileSystem.getSeparator();
        final String that = sep + "abc" + sep + "def" + sep;
        final String other = sep + "aaa" + sep + "be" + sep + "erk";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareChild() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String sep = fileSystem.getSeparator();
        final String that = "abc" + sep + "def";
        final String other = "abc" + sep + "def" + sep + "hij";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        // note:
        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareAbsoluteRelative() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath("abc", "def");
        final Path otherPath = fileSystem.getRootDirectories().iterator().next() //
                .resolve("abc") //
                .resolve("def");

        assertNotEquals(0, path.compareTo(otherPath));
    }

    @Test
    public void testCompareEqual() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String sep = fileSystem.getSeparator();
        final String that = sep + "abc" + sep + "def" + sep;
        final Path path = fileSystem.getPath(that);

        assertEquals(that.compareTo(that), path.compareTo(path));
        assertEquals(0, path.compareTo(path));
    }
}
