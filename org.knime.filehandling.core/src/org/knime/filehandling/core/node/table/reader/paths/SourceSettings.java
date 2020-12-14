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
 *   Dec 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.paths;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.preview.dialog.GenericItemAccessor;

/**
 * Interface defining classes that can be validated, saved to, and loaded from {@link NodeSettings} and allow to access
 * source items.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of source items the item accessor returns
 */
public interface SourceSettings<I> {

    /**
     * Returns the stored source identifier, which can be {@code null}.
     *
     * @return the source identifier
     */
    String getSourceIdentifier();

    /**
     * Validates the provided specs against the settings and either provides warnings via the
     * <b>statusMessageConsumer</b> if the issues are non fatal or throws an InvalidSettingsException if the current
     * configuration and the provided specs make a successful execution impossible.
     *
     * @param specs the input {@link PortObjectSpec specs} of the node
     * @param statusMessageConsumer consumer for status messages e.g. warnings
     * @throws InvalidSettingsException if the specs are not compatible with the settings
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException;

    /**
     * Creates an {@link GenericItemAccessor} for accessing the individual source items.
     *
     * @return a {@link GenericItemAccessor}
     */
    public GenericItemAccessor<I> createItemAccessor();

    /**
     * Serializes the class specific settings to the given <code>NodeSettingsWO</code>.
     *
     * @param settings to serialize the class settings to
     */
    public void saveSettingsTo(final NodeSettingsWO settings);

    /**
     * Loads the class specific settings from the given <code>NodeSettingsRO</code>.
     *
     * @param settings to load the class settings from
     * @throws InvalidSettingsException If the validation of the settings failed.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Read the expected values from the settings object, without assigning them to the internal variables!
     *
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object are invalid.
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException;
}
