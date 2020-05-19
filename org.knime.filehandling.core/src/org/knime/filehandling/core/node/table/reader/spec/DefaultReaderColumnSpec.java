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
 *   May 14, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation of a column solely by its name.</br>
 * The name is optional because it should only be set if it is read from the data.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
class DefaultReaderColumnSpec implements ReaderColumnSpec {

    private final String m_name;

    private final int m_hashCode;

    /**
     * Constructor to be used if the column has a name read from the data.
     *
     * @param name the name of the column read from the data
     */
    DefaultReaderColumnSpec(final String name) {
        m_name = name;
        m_hashCode = Objects.hash(m_name);
    }

    @Override
    public final Optional<String> getName() {
        return Optional.ofNullable(m_name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultReaderColumnSpec) {
            // if the T doesn't match, m_type.equals(other.m_type) will return false anyway
            final DefaultReaderColumnSpec other = (DefaultReaderColumnSpec)obj;
            return Objects.equals(m_name, other.m_name);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (m_name == null) {
            return "<no name>";
        } else {
            return m_name;
        }
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }
}
