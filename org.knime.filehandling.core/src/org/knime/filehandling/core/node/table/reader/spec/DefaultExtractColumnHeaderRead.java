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
 *   Apr 22, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.AbstractReadDecorator;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * A read that extracts the row containing the column headers from the provided {@link Read source}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultExtractColumnHeaderRead<V> extends AbstractReadDecorator<V> implements ExtractColumnHeaderRead<V> {

    private final long m_columnHeaderIdx;

    private RandomAccessible<V> m_columnHeaders = null;

    private long m_rowIdx = -1;

    /**
     * Constructor.
     *
     * @param source the underlying read to extract the column header from
     * @param columnHeaderIdx index of the column header row (set to -1 if no column header is contained)
     */
    DefaultExtractColumnHeaderRead(final Read<V> source, final long columnHeaderIdx) {
        super(source);
        m_columnHeaderIdx = columnHeaderIdx;
    }

    @Override
    public Optional<RandomAccessible<V>> getColumnHeaders() throws IOException {
        if (m_columnHeaderIdx >= 0 && m_columnHeaders == null) {
            // make sure that the column headers are read
            while (next() != null && m_columnHeaders == null) {
                // all the action is in the header
            }
        }
        return Optional.ofNullable(m_columnHeaders);
    }

    /**
     * @return true if the next row contains the column headers
     */
    private boolean isColumnHeaderRow() {
        return m_rowIdx == m_columnHeaderIdx;
    }

    @Override
    public RandomAccessible<V> next() throws IOException {
        m_rowIdx++;
        if (isColumnHeaderRow()) {
            RandomAccessible<V> colHeaderRow = getSource().next();
            CheckUtils.checkState(colHeaderRow != null,
                "The row containing the column headers is not part of the table.");
            m_columnHeaders = colHeaderRow;
        }
        return getSource().next();
    }

}
