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
package org.knime.filehandling.core.fs.tests.integration.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for input streams operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class InputStreamTest extends AbstractParameterizedFSTest {

    private static final String TEST_CONTENT = "This is read by an input stream!!";

    public InputStreamTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    public void testReadFromFile(final Path file) throws IOException {
        final String result;
        try (InputStream inputStream = Files.newInputStream(file)) {
            result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        assertEquals(TEST_CONTENT, result);
    }

    @Test
    public void test_read_from_input_stream() throws Exception {
        testReadFromFile(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir", "fileName"));
    }

    @Test
    public void test_read_from_input_stream_with_spaces() throws Exception {
        testReadFromFile(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir with spaces", "file with spaces"));
    }

    @Test
    public void test_read_from_input_stream_with_pluses() throws Exception {
        testReadFromFile(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir+with+pluses", "file+with+pluses"));
    }

    @Test
    public void test_read_from_input_stream_with_percent_encoding() throws Exception {
        testReadFromFile(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir%20with%20percent%2520encodings",
            "file%20with%20percent%2520encodingsA"));
    }

    @Test(expected = NoSuchFileException.class)
    public void test_read_from_input_stream_non_existing_file() throws Exception {
        Path file = m_testInitializer.createFile("dir", "fileName");
        Path nonExistingFile = file.getParent().resolve("non-existing");
        try (InputStream inputStream = Files.newInputStream(nonExistingFile)) {
            inputStream.read(); // try to read
        }
    }

    @Test(expected = IOException.class)
    public void test_read_from_input_stream_from_directory() throws Exception {
        Path file = m_testInitializer.createFile("dir", "fileName");
        Files.newInputStream(file.getParent()).read();
        fail("IOException should have been thrown before");
    }

    @Test
    public void test_read_from_empty_file() throws Exception{
        Path file = m_testInitializer.createFile("dir", "emptyFile");
        try (InputStream in = Files.newInputStream(file)) {
            assertEquals(-1, in.read());
        }
    }
}
