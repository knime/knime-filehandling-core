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
 *   Apr 1, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.read;

import java.io.IOException;

import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * A {@link Read} that skips the provided index in the underlying read.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of item the read reads from
 * @param <V> the type of values read reads
 */
public final class SkipIdxRead<I, V> extends AbstractReadDecorator<I, V> {

    private final long m_idxToSkip;

    private long m_current = -1;

    /**
     * Constructor.
     *
     * @param source the underlying {@link Read}
     * @param idxToSkip the index to skip (0 based indexing i.e. the first row has index 0)
     */
    public SkipIdxRead(final Read<I, V> source, final long idxToSkip) {
        super(source);
        m_idxToSkip = idxToSkip;
    }

    @SuppressWarnings("resource") // the source is closed in AbstractReadDecorator#close
    @Override
    public RandomAccessible<V> next() throws IOException {
        m_current++;
        if (m_current == m_idxToSkip) {
            getSource().next();
        }
        return getSource().next();
    }

}
