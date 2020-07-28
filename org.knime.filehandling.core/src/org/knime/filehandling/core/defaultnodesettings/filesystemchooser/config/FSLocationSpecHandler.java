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
 *   Jul 20, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Used by {@link FileSystemConfiguration} to handle specific implementations of {@link FSLocationSpec}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <L> the type of {@link FSLocationSpec} this handler handles
 */
public interface FSLocationSpecHandler<L> {

    /**
     * Loads a location spec from the provided {@link NodeSettingsRO}.
     *
     * @param settings to load from
     * @return the location spec stored in {@link NodeSettingsRO} settings
     * @throws InvalidSettingsException if the settings are invalid
     */
    L load(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the <b>spec</b> into the {@link NodeSettingsWO}.
     *
     * @param settings to save to
     * @param spec to save
     */
    void save(final NodeSettingsWO settings, final L spec);

    /**
     * Adapts the provided {@link FSLocationSpec spec} to be of the same type as the oldSpec.
     *
     * @param oldSpec previously stored spec (may be {@code null})
     * @param spec to adapt
     * @return an adapted version of {@link FSLocationSpec spec}
     */
    L adapt(final L oldSpec, final FSLocationSpec spec);

    /**
     * Called if the config has a fs port and the file system is overwritten with a flow variable.
     *
     * @param flowVarLocationSpec the locationSpec provided via flow variable
     * @return the {@link StatusMessage} to display to the user
     */
    StatusMessage warnIfConnectedOverwrittenWithFlowVariable(L flowVarLocationSpec);

    /**
     * Returns the {@link VariableType} compatible with this handler.
     * 
     * @return the {@link VariableType} with which this location handler can work
     */
    VariableType<L> getVariableType();

}
