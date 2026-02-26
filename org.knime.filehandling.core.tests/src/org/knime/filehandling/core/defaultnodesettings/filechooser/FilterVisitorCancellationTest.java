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
 *   Feb 26, 2026 (Tim Crundall, TNG Technology Consulting GmbH): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FilterOptionsSettings;

/**
 * Tests that {@link FilterVisitor} correctly terminates traversal when the calling thread has been interrupted.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 */
public class FilterVisitorCancellationTest {

    private Path m_tempDir;

    private Path m_file;

    private BasicFileAttributes m_fileAttrs;

    private BasicFileAttributes m_dirAttrs;

    private FilterVisitor m_visitor;

    /**
     * Creates a temporary directory and a file inside it, reads their attributes, and constructs a
     * {@link FilterVisitor} with default (permissive) filter settings.
     *
     * @throws IOException if temp-dir creation fails
     */
    @Before
    public void setUp() throws IOException {
        m_tempDir = Files.createTempDirectory("filter_visitor_cancellation_test");
        m_file = Files.createFile(m_tempDir.resolve("testfile.txt"));
        m_fileAttrs = Files.readAttributes(m_file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        m_dirAttrs = Files.readAttributes(m_tempDir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        final FileAndFolderFilter filter = new FileAndFolderFilter(m_tempDir, new FilterOptionsSettings());
        m_visitor = new FilterVisitor(filter, true, true, true);
    }

    /**
     * Clears the interrupt flag after each test so later tests are not affected.
     */
    @After
    public void clearInterruptFlag() {
        Thread.interrupted(); // clears flag
    }

    /**
     * When the current thread is interrupted, {@link FilterVisitor#visitFile} must return
     * {@link FileVisitResult#TERMINATE} immediately without processing the file.
     *
     * @throws IOException not thrown
     */
    @Test
    public void testVisitFileTerminatesWhenInterrupted() throws IOException {
        Thread.currentThread().interrupt();
        final FileVisitResult result = m_visitor.visitFile(m_file, m_fileAttrs);
        assertEquals("visitFile should return TERMINATE when thread is interrupted",
            FileVisitResult.TERMINATE, result);
    }

    /**
     * When the current thread is interrupted, {@link FilterVisitor#preVisitDirectory} must return
     * {@link FileVisitResult#TERMINATE} immediately without descending into the directory.
     *
     * @throws IOException not thrown
     */
    @Test
    public void testPreVisitDirectoryTerminatesWhenInterrupted() throws IOException {
        Thread.currentThread().interrupt();
        final FileVisitResult result = m_visitor.preVisitDirectory(m_tempDir, m_dirAttrs);
        assertEquals("preVisitDirectory should return TERMINATE when thread is interrupted",
            FileVisitResult.TERMINATE, result);
    }

    /**
     * When the thread is NOT interrupted, {@link FilterVisitor#visitFile} must not return
     * {@link FileVisitResult#TERMINATE} — the walk should proceed normally.
     *
     * @throws IOException not thrown
     */
    @Test
    public void testVisitFileContinuesWhenNotInterrupted() throws IOException {
        final FileVisitResult result = m_visitor.visitFile(m_file, m_fileAttrs);
        assertNotEquals("visitFile should not return TERMINATE when thread is not interrupted",
            FileVisitResult.TERMINATE, result);
    }

    /**
     * When the thread is NOT interrupted, {@link FilterVisitor#preVisitDirectory} must not return
     * {@link FileVisitResult#TERMINATE} — the walk should proceed normally.
     *
     * @throws IOException not thrown
     */
    @Test
    public void testPreVisitDirectoryContinuesWhenNotInterrupted() throws IOException {
        final FileVisitResult result = m_visitor.preVisitDirectory(m_tempDir, m_dirAttrs);
        assertNotEquals("preVisitDirectory should not return TERMINATE when thread is not interrupted",
            FileVisitResult.TERMINATE, result);
    }
}
