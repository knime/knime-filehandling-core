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
 *   Feb 24, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location;

import java.util.Objects;

import org.knime.core.data.DataCell;
import org.knime.core.data.meta.DataColumnMetaDataCreator;
import org.knime.core.node.util.CheckUtils;

/**
 * {@link DataColumnMetaDataCreator} for {@link FSLocationValueMetaData}. The {@link #update(DataCell)},
 * {@link #merge(DataColumnMetaDataCreator)} and {@link #merge(FSLocationValueMetaData)} throw exceptions if the file
 * system type and specifier, respectively, do not match.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class FSLocationValueMetaDataCreator implements DataColumnMetaDataCreator<FSLocationValueMetaData> {

    private String m_fileSystemType;

    private String m_fileSystemSpecifier;

    FSLocationValueMetaDataCreator() {
        this(null, null);
    }

    private FSLocationValueMetaDataCreator(final String fileSystemType, final String fileSystemSpecifier) {
        m_fileSystemType = fileSystemType;
        m_fileSystemSpecifier = fileSystemSpecifier;
    }

    @Override
    public void update(final DataCell cell) {
        if (cell.isMissing() || !(cell instanceof FSLocationValue)) {
            return;
        }
        final FSLocationValue value = (FSLocationValue)cell;
        checkCompatibilityAndSet(value.getFSLocation().getFileSystemType(),
            value.getFSLocation().getFileSystemSpecifier().orElse(null));
    }

    private void checkCompatibilityAndSet(final String fileSystemType, final String fileSystemSpecifier) {
        if (fileSystemType != null && m_fileSystemType != null) {
            CheckUtils.checkArgument(Objects.equals(m_fileSystemType, fileSystemType),
                "Locations with incompatible file system types cannot be in the same data column: %s vs. %s.",
                m_fileSystemType, fileSystemType);
            CheckUtils.checkArgument(Objects.equals(m_fileSystemSpecifier, fileSystemSpecifier),
                "Locations with incompatible file system specifiers cannot be in the same data column: %s vs. %s.",
                m_fileSystemSpecifier, fileSystemSpecifier);
        } else {
            m_fileSystemType = fileSystemType;
            m_fileSystemSpecifier = fileSystemSpecifier;
        }
    }

    @Override
    public FSLocationValueMetaData create() {
        return new FSLocationValueMetaData(m_fileSystemType, m_fileSystemSpecifier);
    }

    @Override
    public FSLocationValueMetaDataCreator copy() {
        return new FSLocationValueMetaDataCreator(m_fileSystemType, m_fileSystemSpecifier);
    }

    @Override
    public FSLocationValueMetaDataCreator merge(final DataColumnMetaDataCreator<FSLocationValueMetaData> other) {
        CheckUtils.checkArgument(other instanceof FSLocationValueMetaDataCreator,
            "Can only merge with FSLocationValueMetaDataCreator but received object of type %s.",
            other.getClass().getName());
        final FSLocationValueMetaDataCreator otherCreator = (FSLocationValueMetaDataCreator)other;
        checkCompatibilityAndSet(otherCreator.m_fileSystemType, otherCreator.m_fileSystemSpecifier);
        return this;
    }

    @Override
    public FSLocationValueMetaDataCreator merge(final FSLocationValueMetaData other) {
        checkCompatibilityAndSet(other.getFileSystemType(), other.getFileSystemSpecifier().orElse(null));
        return this;
    }

    @Override
    public Class<FSLocationValueMetaData> getMetaDataClass() {
        return FSLocationValueMetaData.class;
    }

}
