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
package org.knime.filehandling.core.node.table.reader.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

import com.google.common.collect.Iterators;

/**
 * Handles loading, saving and validation of {@link DefaultTableSpecConfig}
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultTableSpecConfigSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableSpecConfigSerializer.class);

    /** Enforce types key. */
    private static final String CFG_ENFORCE_TYPES = "enforce_types";

    /** Include unknown column config key. */
    private static final String CFG_INCLUDE_UNKNOWN = "include_unknown_columns" + SettingsModel.CFGKEY_INTERNAL;

    /** New column position config key. */
    private static final String CFG_NEW_COLUMN_POSITION = "unknown_column_position" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_KEEP = "keep" + SettingsModel.CFGKEY_INTERNAL;

    /** Individual spec config key. */
    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPEC = "individual_spec_";

    /** Root path/item config key. */
    private static final String CFG_ROOT_PATH = "root_path" + SettingsModel.CFGKEY_INTERNAL;

    /** File path config key. */
    private static final String CFG_FILE_PATHS = "file_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATHS = "production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NUM_PRODUCTION_PATHS = "num_production_paths" + SettingsModel.CFGKEY_INTERNAL;

    /** Table spec config key. */
    private static final String CFG_DATATABLE_SPEC = "datatable_spec" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATH = "production_path_";

    private static final String CFG_ORIGINAL_NAMES = "original_names" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_POSITIONAL_MAPPING = "positional_mapping" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode" + SettingsModel.CFGKEY_INTERNAL;

    private final ProductionPathLoader m_productionPathLoader;

    private final Object m_mostGenericType;

    /**
     * Constructor.
     *
     * @param productionPathLoader the {@link ProductionPathLoader} to use for loading {@link ProductionPath
     *            ProductionPaths}
     * @param mostGenericType the most generic type in the hierarchy (used only for workflows created in 4.2)
     */
    public DefaultTableSpecConfigSerializer(final ProductionPathLoader productionPathLoader,
        final Object mostGenericType) {
        m_productionPathLoader = productionPathLoader;
        m_mostGenericType = mostGenericType;
    }

    /**
     * Constructor.
     *
     * @param producerRegistry the {@link ProducerRegistry} to use for loading {@link ProductionPath
     *            ProductionPaths}
     * @param mostGenericType the most generic type in the hierarchy (used only for workflows created in 4.2)
     */
    public DefaultTableSpecConfigSerializer(final ProducerRegistry<?, ?> producerRegistry,
        final Object mostGenericType) {
        this(new DefaultProductionPathLoader(producerRegistry), mostGenericType);
    }

    /**
     * De-serializes the {@link DefaultTableSpecConfig} previously written to the given settings.
     *
     * @param settings containing the serialized {@link DefaultTableSpecConfig}
     * @param specMergeModeOld for workflows stored with 4.2, should be {@code null} for workflows stored with 4.3 and
     *            later
     * @return the de-serialized {@link DefaultTableSpecConfig}
     * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
     */
    public DefaultTableSpecConfig load(final NodeSettingsRO settings,
        @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        final String rootItem = settings.getString(CFG_ROOT_PATH);
        final String[] items = settings.getStringArray(CFG_FILE_PATHS);
        final ReaderTableSpec<?>[] individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), items.length);
        final Set<String> allColumns = union(individualSpecs);

        final boolean includeUnknownColumns = settings.getBoolean(CFG_INCLUDE_UNKNOWN, true);

        final boolean enforceTypes = settings.getBoolean(CFG_ENFORCE_TYPES, false);

        // For old workflows (created with 4.2), the spec might not contain all columns contained in union if
        // SpecMergeMode#INTERSECTION was used to create the final spec
        final DataTableSpec loadedKnimeSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        final DataTableSpec fullKnimeSpec = constructFullKnimeSpec(allColumns, loadedKnimeSpec);

        ProductionPath[] allProdPaths = loadProductionPaths(settings, allColumns, loadedKnimeSpec);

        final String[] originalNames = loadOriginalNames(fullKnimeSpec, settings);
        final int[] positionalMapping = loadPositionalMapping(fullKnimeSpec.getNumColumns(), settings);
        final boolean[] keep = loadKeep(loadedKnimeSpec, allColumns, settings);
        final int newColPosition = settings.getInt(CFG_NEW_COLUMN_POSITION, allProdPaths.length);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings, specMergeModeOld);

        return new DefaultTableSpecConfig(rootItem, fullKnimeSpec, items, individualSpecs, allProdPaths, originalNames,
            positionalMapping, keep, newColPosition, columnFilterMode, includeUnknownColumns, enforceTypes);
    }

    /**
     * @param settings {@link NodeSettingsRO} to read from
     * @param specMergeModeOld the old {@link SpecMergeMode}
     * @return the {@link ColumnFilterMode} to use
     * @throws InvalidSettingsException
     */
    private static ColumnFilterMode loadColumnFilterMode(final NodeSettingsRO settings,
        @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        try {
            return ColumnFilterMode.valueOf(settings.getString(CFG_COLUMN_FILTER_MODE));
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("The settings contained no ColumnFilterMode.", ise);
            CheckUtils.checkSetting(specMergeModeOld != null,
                "The settings are missing both the SpecMergeMode (4.2) and the ColumnFilterMode (4.3 and later).");
            @SuppressWarnings({"null", "deprecation"}) // checked above
            final ColumnFilterMode columnFilterMode = specMergeModeOld.getColumnFilterMode();
            return columnFilterMode;
        }
    }

    /**
     * @param spec {@link DataTableSpec}
     * @param allColumns all column names
     * @param settings {@link NodeSettingsRO} to read from
     * @return the keep flag array
     */
    private static boolean[] loadKeep(final DataTableSpec spec, final Set<String> allColumns,
        final NodeSettingsRO settings) {
        final boolean[] keep = settings.getBooleanArray(CFG_KEEP, (boolean[])null);
        if (keep == null) {
            /**
             * Settings stored before 4.3 didn't have this settings, so we need to reconstruct it. Before 4.3
             * potentially not all columns were contained in spec in case the user selected the SpecMergeMode
             * intersection. Hence we reconstruct this behavior by checking which columns of the individual specs were
             * contained in the output and which were not.
             */
            return createKeepForOldWorkflows(spec, allColumns);
        } else {
            return keep;
        }
    }

    /**
     * Before 4.3 potentially not all columns were contained in spec in case the user selected the SpecMergeMode
     * intersection.
     *
     * @param spec the output {@link DataTableSpec} which might not contain all columns contained in
     *            <b>individualSpecs</b>
     * @param individualSpecs the column names contained in the individual files
     * @return the reconstructed keep array
     */
    private static boolean[] createKeepForOldWorkflows(final DataTableSpec spec, final Set<String> allColumns) {

        final boolean[] keep = new boolean[allColumns.size()];
        int i = 0;
        for (String colName : allColumns) {
            keep[i] = spec.containsName(colName);
            i++;
        }
        return keep;
    }

    /**
     * @param allColumns all column names
     * @param loadedKnimeSpec {@link DataTableSpec}
     * @return reconstructed KNIME {@link DataTableSpec}
     */
    private static DataTableSpec constructFullKnimeSpec(final Set<String> allColumns,
        final DataTableSpec loadedKnimeSpec) {
        if (allColumns.size() == loadedKnimeSpec.getNumColumns()) {
            return loadedKnimeSpec;
        } else {
            return reconstructFullKnimeSpec(allColumns, loadedKnimeSpec);
        }
    }

    private static DataTableSpec reconstructFullKnimeSpec(final Set<String> allColumns,
        final DataTableSpec loadedKnimeSpec) {
        final DataTableSpecCreator fullKnimeSpecCreator = new DataTableSpecCreator();
        for (String col : allColumns) {
            if (loadedKnimeSpec.containsName(col)) {
                fullKnimeSpecCreator.addColumns(loadedKnimeSpec.getColumnSpec(col));
            } else {
                fullKnimeSpecCreator.addColumns(new DataColumnSpecCreator(col, StringCell.TYPE).createSpec());
            }
        }
        return fullKnimeSpecCreator.createSpec();
    }

    /**
     * @param individualSpecs individual {@link ReaderTableSpec}s
     * @return the union of the column names
     */
    private static Set<String> union(final ReaderTableSpec<?>[] individualSpecs) {
        final Set<String> allColumns = new LinkedHashSet<>();
        for (ReaderTableSpec<?> ts : individualSpecs) {
            for (ReaderColumnSpec col : ts) {
                allColumns.add(MultiTableUtils.getNameAfterInit(col));
            }
        }
        return allColumns;
    }

    /**
     * @param spec {@link DataTableSpec}
     * @param settings {@link NodeSettingsRO} to read from
     * @return the original column names
     */
    private static String[] loadOriginalNames(final DataTableSpec spec, final NodeSettingsRO settings) {
        final String[] originalNames = settings.getStringArray(CFG_ORIGINAL_NAMES, (String[])null);
        if (originalNames == null) {
            // Settings stored before 4.3 didn't have this setting, which means that all columns kept their original name
            return spec.stream().map(DataColumnSpec::getName).toArray(String[]::new);
        } else {
            return originalNames;
        }
    }

    /**
     * @param numColumns number of columns
     * @param settings {@link NodeSettingsRO} to read from
     * @return positional mapping
     */
    private static int[] loadPositionalMapping(final int numColumns, final NodeSettingsRO settings) {
        final int[] positionalMapping = settings.getIntArray(CFG_POSITIONAL_MAPPING, (int[])null);
        if (positionalMapping == null) {
            // Settings stored before 4.3 didn't have this setting, which means that there was no reordering or filtering
            return IntStream.range(0, numColumns).toArray();
        } else {
            return positionalMapping;
        }
    }

    /**
     * @param nodeSettings to read from
     * @param numIndividualPaths number of paths to read
     * @return {@link ReaderTableSpec}
     * @throws InvalidSettingsException
     */
    private static ReaderTableSpec<?>[] loadIndividualSpecs(final NodeSettingsRO nodeSettings,
        final int numIndividualPaths) throws InvalidSettingsException {
        final ReaderTableSpec<?>[] individualSpecs = new ReaderTableSpec[numIndividualPaths];
        for (int i = 0; i < numIndividualPaths; i++) {
            individualSpecs[i] = ReaderTableSpec
                .createReaderTableSpec(Arrays.asList(nodeSettings.getStringArray(CFG_INDIVIDUAL_SPEC + i)));
        }
        return individualSpecs;
    }

    /**
     * @param settings {@link NodeSettingsRO}
     * @param pathLoader {@link ProductionPathLoader}
     * @param mostGenericExternalType the most generic external type
     * @param allColumns all column names
     * @param dataTableSpec {@link DataTableSpec}
     * @return {@link ProductionPath}s
     * @throws InvalidSettingsException
     */
    private ProductionPath[] loadProductionPaths(final NodeSettingsRO settings, final Set<String> allColumns,
        final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final ProductionPath[] prodPaths =
            loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS));
        if (allColumns.size() == dataTableSpec.getNumColumns()) {
            return prodPaths;
        } else {
            return reconstructProdPathsFor42Intersection(m_productionPathLoader.getProducerRegistry(),
                m_mostGenericType, allColumns, dataTableSpec, prodPaths);
        }
    }

    private ProductionPath[] loadProductionPathsFromSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final ProductionPath[] prodPaths = new ProductionPath[settings.getInt(CFG_NUM_PRODUCTION_PATHS)];
        for (int i = 0; i < prodPaths.length; i++) {
            final int idx = i;
            prodPaths[i] = m_productionPathLoader.loadProductionPath(settings, CFG_PRODUCTION_PATH + i)
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("No production path associated with key <%s>", CFG_PRODUCTION_PATH + idx)));
        }
        return prodPaths;
    }

    /**
     * In KAP 4.2 we only stored the {@link ProductionPath ProductionPaths} for the columns that were in the KNIME
     * output spec. If the user read in multiple files and selected intersection as spec merge mode, this meant that we
     * didn't store the ProductionPath for those columns that were not part of the intersection.</br>
     * In KAP 4.3, we introduce the Transformation tab which allows to manipulate all columns of the union of the read
     * files, so we need ProductionPaths for the left-out columns as well. To this end we will assume that those columns
     * had the most generic type (typically String) and use the default ProductionPath to convert them into a String
     * column.
     *
     * @param registry {@link ProducerRegistry}
     * @param mostGenericExternalType typically String
     * @param allColumns the {@link Set} of all columns
     * @param dataTableSpec the loaded spec (i.e. potentially the intersection)
     * @param prodPaths the ProductionPaths corresponding to <b>dataTableSpec</b>
     * @return the ProductionPath array for the complete spec (i.e. union of all specs)
     */
    private static ProductionPath[] reconstructProdPathsFor42Intersection(final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType, final Set<String> allColumns, final DataTableSpec dataTableSpec,
        final ProductionPath[] prodPaths) {
        final ProductionPath defaultProdPath = findDefaultProdPath(registry, mostGenericExternalType);
        final List<ProductionPath> allProdPaths = new ArrayList<>(allColumns.size());
        final Iterator<ProductionPath> loadedProdPaths = Iterators.forArray(prodPaths);
        for (String col : allColumns) {
            if (dataTableSpec.containsName(col)) {
                allProdPaths.add(loadedProdPaths.next());
            } else {
                allProdPaths.add(defaultProdPath);
            }
        }
        return allProdPaths.toArray(new ProductionPath[0]);
    }

    private static ProductionPath findDefaultProdPath(final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType) {
        return registry.getAvailableProductionPaths().stream()//
            .filter(p -> p.getProducerFactory().getSourceType().equals(mostGenericExternalType))//
            .filter(p -> p.getConverterFactory().getDestinationType() == StringCell.TYPE)//
            .findFirst()//
            .orElseThrow(() -> new IllegalStateException(
                "No string converter available for the supposedly most generic external type: "
                    + mostGenericExternalType));
    }

    static void save(final DefaultTableSpecConfig config, final NodeSettingsWO settings) {
        settings.addString(CFG_ROOT_PATH, config.getRootItem());
        config.getFullDataSpec().save(settings.addNodeSettings(CFG_DATATABLE_SPEC));
        settings.addStringArray(CFG_FILE_PATHS, config.getItems().toArray(new String[0]));
        saveIndividualSpecs(config.getIndividualSpecs(), settings.addNodeSettings(CFG_INDIVIDUAL_SPECS));
        saveProductionPaths(config.getAllProductionPaths(), settings.addNodeSettings(CFG_PRODUCTION_PATHS));
        settings.addStringArray(CFG_ORIGINAL_NAMES, config.getOriginalNames());
        settings.addIntArray(CFG_POSITIONAL_MAPPING, config.getPositions());
        settings.addBooleanArray(CFG_KEEP, config.getKeep());
        settings.addInt(CFG_NEW_COLUMN_POSITION, config.getUnknownColumnPosition());
        settings.addBoolean(CFG_INCLUDE_UNKNOWN, config.includeUnknownColumns());
        settings.addBoolean(CFG_ENFORCE_TYPES, config.enforceTypes());
        settings.addString(CFG_COLUMN_FILTER_MODE, config.getColumnFilterMode().name());
    }

    private static void saveProductionPaths(final ProductionPath[] prodPaths, final NodeSettingsWO settings) {
        int i = 0;
        for (final ProductionPath pP : prodPaths) {
            SerializeUtil.storeProductionPath(pP, settings, CFG_PRODUCTION_PATH + i);
            i++;
        }
        settings.addInt(CFG_NUM_PRODUCTION_PATHS, i);
    }

    private static void saveIndividualSpecs(final Collection<ReaderTableSpec<?>> individualSpecs,
        final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<? extends ReaderColumnSpec> readerTableSpec : individualSpecs) {
            settings.addStringArray(CFG_INDIVIDUAL_SPEC + i//
                , readerTableSpec.stream()//
                    .map(MultiTableUtils::getNameAfterInit)//
                    .toArray(String[]::new)//
            );
            i++;
        }
    }

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_ROOT_PATH);
        DataTableSpec.load(settings.getNodeSettings(CFG_DATATABLE_SPEC));
        final int numIndividualPaths = settings.getStringArray(CFG_FILE_PATHS).length;
        validateIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), numIndividualPaths);
        //try to load the production paths
        loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS));
    }

    private static void validateIndividualSpecs(final NodeSettingsRO settings, final int numIndividualPaths)
        throws InvalidSettingsException {
        for (int i = 0; i < numIndividualPaths; i++) {
            settings.getStringArray(CFG_INDIVIDUAL_SPEC + i);
        }
    }

    private static final class DefaultProductionPathLoader implements ProductionPathLoader {

        private ProducerRegistry<?, ?> m_registry;

        private DefaultProductionPathLoader(final ProducerRegistry<?, ?> registry) {
            m_registry = registry;
        }

        @Override
        public Optional<ProductionPath> loadProductionPath(final NodeSettingsRO config, final String key)
            throws InvalidSettingsException {
            return SerializeUtil.loadProductionPath(config, getProducerRegistry(), key);
        }

        @Override
        public ProducerRegistry<?, ?> getProducerRegistry() {
            return m_registry;
        }
    }
}
