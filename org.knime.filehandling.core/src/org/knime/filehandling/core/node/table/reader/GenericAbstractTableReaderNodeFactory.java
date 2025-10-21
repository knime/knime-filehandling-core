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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractTableReaderNodeDialog;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * An abstract implementation of a node factory for table reader nodes that builds on top of
 * {@link CommonTableReaderNodeFactory}, but uses a swing-based {@link NodeDialogPane}, as well as a
 * {@link StorableMultiTableReadConfig} and {@link SourceSettings} that provide methods to validate, save to, and load
 * from {@link NodeSettings}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <V> the type used as value by the reader
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class GenericAbstractTableReaderNodeFactory<I, C extends ReaderSpecificConfig<C>, T, V>
    extends CommonTableReaderNodeFactory<I, SourceSettings<I>, C, T, StorableMultiTableReadConfig<C, T>, V> {

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        final MultiTableReadFactory<I, C, T> readFactory = createMultiTableReadFactory(createReader());
        final ProductionPathProvider<T> productionPathProvider = createProductionPathProvider();
        return createNodeDialogPane(creationConfig, readFactory, productionPathProvider);
    }

    /**
     * Creates the node dialog.
     *
     * @param creationConfig {@link NodeCreationConfiguration}
     * @param readFactory the {@link MultiTableReadFactory} needed to create an {@link AbstractTableReaderNodeDialog}
     * @param defaultProductionPathFn provides the default {@link ProductionPath} for all external types
     * @return the node dialog
     */
    protected abstract AbstractTableReaderNodeDialog<I, C, T> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig, final MultiTableReadFactory<I, C, T> readFactory,
        final ProductionPathProvider<T> defaultProductionPathFn);

    final class GenericConfigAndSourceSerializer
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

    @Override
    protected ConfigAndSourceSerializer<I, SourceSettings<I>, C, T, StorableMultiTableReadConfig<C, T>>
        createSerializer() {
        return new GenericConfigAndSourceSerializer();
    }

}