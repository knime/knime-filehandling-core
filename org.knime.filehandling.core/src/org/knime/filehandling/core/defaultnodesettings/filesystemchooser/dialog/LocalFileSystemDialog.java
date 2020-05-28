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
 *   Apr 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog;

import java.awt.Color;
import java.awt.Component;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * FileSystemDialog for the local file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public enum LocalFileSystemDialog implements FileSystemSpecificDialog {

    /**
     * The singleton instance.
     */
    INSTANCE;

    private static final String ID = FileSystemChoice.getLocalFsChoice().getId();

    @Override
    public Component getSpecifierComponent() {
        throw new NotImplementedException("The local file system has no specifier."
            + "This indicates a coding error because this method should only be called "
            + "if hasSpecifierComponent returned true.");
    }

    @Override
    public boolean hasSpecifierComponent() {
        return false;
    }

    @Override
    public String toString() {
        return ID;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        // nothing to do
    }

    @Override
    public Choice getChoice() {
        return Choice.LOCAL_FS;
    }

    @Override
    public Color getTextColor() {
        return Color.BLACK;
    }

    @Override
    public void setTooltip(final String tooltip) {
        // nothing to set the tooltip on
    }

}