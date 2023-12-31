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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for output stream operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class OutputStreamTest extends AbstractParameterizedFSTest {

    public OutputStreamTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_output_stream() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path file = m_testInitializer.createFile("dir", "fileName");

        String contentToWrite = "This is written by an output stream!!";
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            outputStream.write(contentToWrite.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals(1, fileContent.size());
        assertEquals(contentToWrite, fileContent.get(0));
    }

    @Test
    public void test_output_stream_append() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        String content = "This was already there";
        Path file = m_testInitializer.createFileWithContent(content, "dir", "fileName");

        String contentToWrite = ", but this was appended!";
        try (OutputStream outputStream = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
            outputStream.write(contentToWrite.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals("This was already there, but this was appended!", fileContent.get(0));
    }

    public static void testWriteNewFile(final Path file) throws IOException {
        final byte[] contentToWrite = "The wheel is come full circle: I am here.".getBytes(StandardCharsets.UTF_8);

        try (OutputStream outputStream = //
            Files.newOutputStream(//
                file, //
                StandardOpenOption.CREATE_NEW, //
                StandardOpenOption.WRITE//
            )) {
            outputStream.write(contentToWrite);
        }

        assertArrayEquals(contentToWrite, Files.readAllBytes(file));
    }

    @Test
    public void test_output_stream_create_file() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        testWriteNewFile(m_testInitializer.makePath("some-file"));
    }

    @Test
    public void test_output_stream_create_file_with_spaces() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        testWriteNewFile(m_testInitializer.makePath("some file"));
    }

    @Test
    public void test_output_stream_create_file_with_pluses() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        testWriteNewFile(m_testInitializer.makePath("some+file"));
    }

    @Test
    public void test_output_stream_create_file_with_percent_encoding() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        testWriteNewFile(m_testInitializer.makePath("file%20with%20percent%2520encodings"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_output_stream_create_new_file_failure() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path file = m_testInitializer.createFile("file");
        Files.newOutputStream(//
            file, //
            StandardOpenOption.CREATE_NEW, //
            StandardOpenOption.WRITE);
    }

    @Test
    public void test_output_stream_overwrite() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        String content = "I burn, I pine, I perish.";
        Path file = m_testInitializer.createFileWithContent(content, "dir", "file");

        String overwriteContent = "enough Shakespeare quotes!";
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            outputStream.write(overwriteContent.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals(overwriteContent, fileContent.get(0));
    }

    @Test(expected = IOException.class)
    public void test_output_stream_on_directory_failure() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path file = m_testInitializer.createFile("dir", "file");
        Files.newOutputStream(file.getParent());
    }

    @Test
    public void test_write_three_files_same_folder() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());

        Path dir = m_testInitializer.makePath("dir");
        Path file1 = dir.resolve("file1");
        Path file2 = dir.resolve("file2");

        Files.createDirectories(dir);

        final byte[] file1Bytes = new byte[] {1};
        try(OutputStream out = Files.newOutputStream(file1)) {
            out.write(file1Bytes);
        }

        final byte[] file2Bytes = new byte[] {2};
        try(OutputStream out = Files.newOutputStream(file2)) {
            out.write(file2Bytes);
        }

        assertTrue(Files.isDirectory(dir));
        assertTrue(Files.isRegularFile(file1));
        assertArrayEquals(file1Bytes, Files.readAllBytes(file1));
        assertTrue(Files.isRegularFile(file2));
        assertArrayEquals(file2Bytes, Files.readAllBytes(file2));
    }

    @Test
    public void test_overwrite_updates_attribute_times() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        ignoreWithReason("Some FTP servers don't have second resolution of mtime", FTP);

        final Path file = m_testInitializer.createFileWithContent("a", "file");

        final BasicFileAttributes beforeTgtAttributes = Files.readAttributes(file, BasicFileAttributes.class);
        Thread.sleep(1000);
        try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write(0);
        }
        final BasicFileAttributes afterTgtAttributes = Files.readAttributes(file, BasicFileAttributes.class);

        assertTrue(beforeTgtAttributes.creationTime().toMillis() <= afterTgtAttributes.creationTime().toMillis());
        assertTrue(beforeTgtAttributes.lastModifiedTime().toMillis() < afterTgtAttributes.lastModifiedTime().toMillis());
    }
}
