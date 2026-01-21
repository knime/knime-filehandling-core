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

import java.util.function.Predicate;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeFactory.ConfigAndSourceSerializer;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.Source;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;

/**
 * Used to provide backwards compatibility for nodes based on {@link CommonTableReaderNodeModel} that previously used a
 * different source implementation and had a different overall settings structure.
 *
 * I.e. the added implementation wrt the {@link CommonTableReaderNodeModel} is similar to the one the
 * {@link TableReaderNodeModel} consists of.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Paul Baernreuther, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of the item to read from
 * @param <S> the type of {@link Source}
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <M> the type of {@link MultiTableReadConfig}
 * @noreference non-public API
 * @noimplement non-public API
 */
public class BackwardsCompatibleCommonTableReaderNodeModel<I, S extends Source<I>, C extends ReaderSpecificConfig<C>, T, //
        M extends StorableMultiTableReadConfig<C, T>>
    extends CommonTableReaderNodeModel<I, S, C, T, M> {

    private final SourceSettings<I> m_legacySourceSettings;

    private final Predicate<NodeSettingsRO> m_isLegacySettingsPredicate;

    private boolean m_useLegacySourceSettings;

    @Override
    protected Source<I> getSource() {
        if (m_useLegacySourceSettings) {
            return m_legacySourceSettings;
        }
        return super.getSource();
    }

    /**
     * Constructs a node model with no inputs and one output.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     * @param serializer serializer for the source and config
     * @param legacySourceSettings the source settings loaded from legacy settings
     * @param isLegacySettingsPredicate a method that returns true for all states of the settings before node parameters
     *            have been introduced and false for all states after that.
     */
    protected BackwardsCompatibleCommonTableReaderNodeModel(final M config, final S pathSettingsModel,
        final MultiTableReader<I, C, T> tableReader, final ConfigAndSourceSerializer<I, S, C, T, M> serializer,
        final SourceSettings<I> legacySourceSettings, final Predicate<NodeSettingsRO> isLegacySettingsPredicate) {
        super(config, pathSettingsModel, tableReader, serializer);
        m_legacySourceSettings = legacySourceSettings;
        m_isLegacySettingsPredicate = isLegacySettingsPredicate;
    }

    /**
     * Constructs a node model with the inputs and outputs specified in the passed {@link PortsConfiguration}.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     * @param serializer serializer for the source and config
     * @param portsConfig determines the in and outports.
     * @param legacySourceSettings the source settings loaded from legacy settings
     * @param isLegacySettingsPredicate a method that returns true for all states of the settings before node parameters
     *            have been introduced and false for all states after that.
     */
    protected BackwardsCompatibleCommonTableReaderNodeModel(final M config, final S pathSettingsModel,
        final MultiTableReader<I, C, T> tableReader, final ConfigAndSourceSerializer<I, S, C, T, M> serializer,
        final PortsConfiguration portsConfig, final SourceSettings<I> legacySourceSettings,
        final Predicate<NodeSettingsRO> isLegacySettingsPredicate) {
        super(config, pathSettingsModel, tableReader, serializer, portsConfig);
        m_legacySourceSettings = legacySourceSettings;
        m_isLegacySettingsPredicate = isLegacySettingsPredicate;
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useLegacySourceSettings = m_isLegacySettingsPredicate.test(settings);
        if (m_useLegacySourceSettings) {
            m_legacySourceSettings.validateSettings(settings);
            getConfig().validate(settings);
        } else {
            super.validateSettings(settings);
        }

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useLegacySourceSettings = m_isLegacySettingsPredicate.test(settings);
        if (m_useLegacySourceSettings) {
            m_legacySourceSettings.loadSettingsFrom(settings);
            getConfig().loadInModel(settings);
        } else {
            super.loadValidatedSettingsFrom(settings);
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_useLegacySourceSettings) {
            m_legacySourceSettings.saveSettingsTo(settings);
            getConfig().saveInModel(settings);
        } else {
            super.saveSettingsTo(settings);
        }
    }

}
