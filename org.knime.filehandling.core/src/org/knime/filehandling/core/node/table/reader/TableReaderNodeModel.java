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
 *   Jan 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeFactory.ConfigAndSourceSerializer;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Generic implementation of a Reader node that reads tables.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <I> the type of the item to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @noreference non-public API
 * @noimplement non-public API
 */
public class TableReaderNodeModel<I, C extends ReaderSpecificConfig<C>, T>
    extends CommonTableReaderNodeModel<I, SourceSettings<I>, C, T, StorableMultiTableReadConfig<C, T>> {

    static final class GenericConfigAndSourceSerializer<I, C extends ReaderSpecificConfig<C>, T>
        implements ConfigAndSourceSerializer<I, SourceSettings<I>, C, T, StorableMultiTableReadConfig<C, T>> {

        @Override
        public void saveSettingsTo(final SourceSettings<I> sourceSettings,
            final StorableMultiTableReadConfig<C, T> config, final NodeSettingsWO settings) {
            config.saveInModel(settings);
            sourceSettings.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        }

        @Override
        public void validateSettings(final SourceSettings<I> sourceSettings,
            final StorableMultiTableReadConfig<C, T> config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
            config.validate(settings);
            sourceSettings.validateSettings(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        }

        @Override
        public void loadValidatedSettingsFrom(final SourceSettings<I> sourceSettings,
            final StorableMultiTableReadConfig<C, T> config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
            config.loadInModel(settings);
            sourceSettings.loadSettingsFrom(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        }
    }

    static <I, C extends ReaderSpecificConfig<C>, T> GenericConfigAndSourceSerializer<I, C, T> createSerializer() {
        return new GenericConfigAndSourceSerializer<>();
    }

    /**
     * Constructs a node model with no inputs and one output.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     */
    protected TableReaderNodeModel(final StorableMultiTableReadConfig<C, T> config,
        final SourceSettings<I> pathSettingsModel, final MultiTableReader<I, C, T> tableReader) {
        super(() -> config, pathSettingsModel, tableReader, createSerializer());
    }

    /**
     * Constructs a node model with the inputs and outputs specified in the passed {@link PortsConfiguration}.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     * @param portsConfig determines the in and outports.
     */
    protected TableReaderNodeModel(final StorableMultiTableReadConfig<C, T> config,
        final SourceSettings<I> pathSettingsModel, final MultiTableReader<I, C, T> tableReader,
        final PortsConfiguration portsConfig) {
        super(() -> config, pathSettingsModel, tableReader, createSerializer(), portsConfig);
    }

}
