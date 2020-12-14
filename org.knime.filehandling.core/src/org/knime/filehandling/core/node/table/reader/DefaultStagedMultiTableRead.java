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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.DefaultTypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Default implementation of a {@link StagedMultiTableRead}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
public final class DefaultStagedMultiTableRead<I, C extends ReaderSpecificConfig<C>, T, V>
    implements StagedMultiTableRead<I, T> {

    private final Map<I, TypedReaderTableSpec<T>> m_individualSpecs;

    private final String m_rootItem;

    private final RawSpec<T> m_rawSpec;

    private final GenericRowKeyGeneratorContextFactory<I, V> m_rowKeyGenFactory;

    private final Supplier<ReadAdapter<T, V>> m_readAdapterSupplier;

    private final TableReadConfig<C> m_tableReadConfig;

    private final TableTransformation<T> m_defaultTransformation;

    private final GenericTableReader<I, C, T, V> m_reader;

    /**
     * Constructor.
     *
     * @param reader {@link GenericTableReader}
     * @param rootItem root item
     * @param individualSpecs individuals specs
     * @param rowKeyGenFactory {@link GenericRowKeyGeneratorContextFactory}
     * @param readAdapterSupplier {@link ReadAdapter} supplier
     * @param defaultTransformation {@link TableTransformation}
     * @param tableReadConfig {@link TableReadConfig}
     */
    protected DefaultStagedMultiTableRead(final GenericTableReader<I, C, T, V> reader, final String rootItem,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs,
        final GenericRowKeyGeneratorContextFactory<I, V> rowKeyGenFactory,
        final Supplier<ReadAdapter<T, V>> readAdapterSupplier, final TableTransformation<T> defaultTransformation,
        final TableReadConfig<C> tableReadConfig) {
        m_rawSpec = defaultTransformation.getRawSpec();
        m_rootItem = rootItem;
        m_individualSpecs = individualSpecs;
        m_rowKeyGenFactory = rowKeyGenFactory;
        m_tableReadConfig = tableReadConfig;
        m_defaultTransformation = defaultTransformation;
        m_reader = reader;
        m_readAdapterSupplier = readAdapterSupplier;
    }

    @Override
    public MultiTableRead withoutTransformation(final Collection<I> items) {
        return withTransformation(items, m_defaultTransformation);
    }

    @Override
    public MultiTableRead withTransformation(final Collection<I> items,
        final TableTransformation<T> transformationModel) {
        final TableSpecConfig tableSpecConfig = DefaultTableSpecConfig
            .createFromTransformationModel(m_rootItem, m_individualSpecs, transformationModel);
        return new DefaultMultiTableRead<>(items, p -> createRead(p, m_tableReadConfig), () -> {
            IndividualTableReaderFactory<I, T, V> factory =
                createIndividualTableReaderFactory(transformationModel);
            return factory::create;
        }, tableSpecConfig, TableTransformationUtils.toDataTableSpec(transformationModel));
    }

    private IndividualTableReaderFactory<I, T, V>
        createIndividualTableReaderFactory(final TableTransformation<T> transformationModel) {
        return new IndividualTableReaderFactory<>(m_individualSpecs, m_tableReadConfig,
            TableTransformationUtils.getOutputTransformations(transformationModel), this::createTypeMapper,
            m_rowKeyGenFactory.createContext(m_tableReadConfig));
    }

    private Read<I, V> createRead(final I path, final TableReadConfig<C> config) throws IOException {
        final Read<I, V> rawRead = m_reader.read(path, config);
        if (config.decorateRead()) {
            return ReadUtils.decorateForReading(rawRead, config);
        }
        return rawRead;
    }

    private TypeMapper<V> createTypeMapper(final ProductionPath[] prodPaths, final FileStoreFactory fsFactory) {
        return new DefaultTypeMapper<>(m_readAdapterSupplier.get(), prodPaths, fsFactory,
            m_tableReadConfig.getReaderSpecificConfig());
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean isValidFor(final Collection<I> items) {
        return items.size() == m_individualSpecs.size() && m_individualSpecs.keySet().containsAll(items);
    }

}