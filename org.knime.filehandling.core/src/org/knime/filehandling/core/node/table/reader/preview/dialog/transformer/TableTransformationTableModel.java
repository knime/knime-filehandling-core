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
 *   Aug 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import static java.util.stream.Collectors.toSet;
import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ObservableTransformationModelProvider;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

/**
 * A {@link TableTransformation} that is based on an {@link AbstractTableModel}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external data types
 */
public final class TableTransformationTableModel<T> extends AbstractTableModel
    implements ObservableTransformationModelProvider<T> {

    private static final int REORDER = 0;

    private static final int KEEP = 1;

    private static final int COLUMN = 2;

    private static final int RENAME = 3;

    private static final int TYPE = 4;

    private static final String[] COLUMN_NAMES = {"", "", "Column", "Name", "Type"};

    private static final DataColumnSpec NEW_COL_SPEC =
        new DataColumnSpecCreator("<any unknown new column>", DataType.getType(DataCell.class)).createSpec();

    private final transient MutableColumnTransformation<T> m_newColTransformationPlaceholder =
        new MutableColumnTransformation<T>(NEW_COL_SPEC, TypedReaderColumnSpec.getNull(), -1, "<no-name>", null, -1,
            true) {

            @Override
            public int getPosition() {
                final int position = super.getPosition();
                return position == -1 ? (m_union.size() - 1) : position;
            }

            @Override
            public String getOriginalName() {
                return "<no-name>";
            }

        };

    private final transient Function<T, ProductionPath> m_defaultProductionPathFn;

    private final transient Map<TypedReaderColumnSpec<T>, MutableColumnTransformation<T>> m_bySpec = new HashMap<>();

    private final transient SetMultimap<String, MutableColumnTransformation<T>> m_byName =
        MultimapBuilder.hashKeys().hashSetValues().build();

    private final transient List<MutableColumnTransformation<T>> m_union = new ArrayList<>();

    private final transient List<MutableColumnTransformation<T>> m_intersection = new ArrayList<>();

    private final transient CopyOnWriteArraySet<ChangeListener> m_changeListeners = new CopyOnWriteArraySet<>();

    private final transient ChangeEvent m_changeEvent = new ChangeEvent(this);

    private final transient ColumnFilterModeModel m_columnFilterMode =
        new ColumnFilterModeModel(ColumnFilterMode.UNION);

    private transient RawSpec<T> m_rawSpec = null;

    private boolean m_enabled = true;

    /* Only Serializable because AbstractTableModel is.
     * Instances of this class will never be serialized.
     */
    private static final long serialVersionUID = 1L; // NOSONAR

    /**
     * Constructor.
     *
     * @param defaultProductionPathFn provides default {@link ProductionPath ProductionPaths} for external types.
     */
    public TableTransformationTableModel(final Function<T, ProductionPath> defaultProductionPathFn) {
        m_defaultProductionPathFn = defaultProductionPathFn;
        addTableModelListener(e -> notifyChangeListeners());
        m_byName.put(m_newColTransformationPlaceholder.getName(), m_newColTransformationPlaceholder);
        m_bySpec.put(TypedReaderColumnSpec.getNull(), m_newColTransformationPlaceholder);
        m_union.add(m_newColTransformationPlaceholder);
        m_columnFilterMode.addChangeListener(e -> handleColumnFilterChange());
    }

    private void handleColumnFilterChange() {
        fireTableDataChanged();
    }

    private interface TransformationResetter<T> extends Predicate<MutableColumnTransformation<T>> {

        default boolean canRequireSort() {
            return false;
        }
    }

    @SafeVarargs // the varargs are only used for reading inside of this method
    private final void resetTransformations(final TransformationResetter<T>... resetters) {
        boolean tableChanged = false;
        boolean requiresSort = false;
        for (MutableColumnTransformation<T> transformation : m_union) {
            for (TransformationResetter<T> resetter : resetters) {
                final boolean resetHappened = resetter.test(transformation);
                tableChanged |= resetHappened;
                requiresSort |= resetter.canRequireSort() && resetHappened;
            }
        }
        if (requiresSort) {
            sortByOutputIdx();
        }
        if (tableChanged) {
            fireTableDataChanged();
        }
    }

    private void sortByOutputIdx() {
        m_union.sort(Comparator.naturalOrder());
        m_intersection.sort(Comparator.naturalOrder());
    }

    void resetToRawSpec() {
        resetTransformations(//
            this::resetName, //
            createPositionResetter(), //
            createKeepResetter(), //
            this::resetProductionPath);
    }

    void resetNames() {
        resetTransformations(this::resetName);
    }

    void resetPositions() {
        resetTransformations(createPositionResetter());
    }

    void resetKeep() {
        resetTransformations(createKeepResetter());
    }

    void resetProductionPaths() {
        resetTransformations(this::resetProductionPath);
    }

    private static <T> TransformationResetter<T> createKeepResetter() {
        return t -> t.setKeep(true);
    }

    private boolean resetName(final MutableColumnTransformation<T> transformation) {
        final String defaultName = transformation.getOriginalName();
        if (!transformation.getName().equals(defaultName)) {
            m_byName.remove(transformation.getName(), transformation);
            transformation.setName(defaultName);
            m_byName.put(defaultName, transformation);
            return true;
        }
        return false;
    }

    private static <T> TransformationResetter<T> createPositionResetter() {
        return new TransformationResetter<T>() {

            @Override
            public boolean test(final MutableColumnTransformation<T> transformation) {
                return transformation.resetPosition();
            }

            @Override
            public boolean canRequireSort() {
                return true;
            }
        };
    }

    // the unused parameter is necessary because resetKeep has to satisfy the TransformationResetter interface
    private boolean resetProductionPath(final MutableColumnTransformation<T> transformation) {
        if (transformation == m_newColTransformationPlaceholder) {
            return false;
        }
        final ProductionPath defaultProductionPath =
            m_defaultProductionPathFn.apply(transformation.getExternalSpec().getType());
        if (!defaultProductionPath.equals(transformation.getProductionPath())) {
            transformation.setProductionPath(defaultProductionPath);
            return true;
        }
        return false;
    }

    @Override
    public void updateRawSpec(final RawSpec<T> rawSpec) {
        if (rawSpec == null) {
            if (m_rawSpec != null) {
                m_rawSpec = null;
                fireTableDataChanged();
            }
            return;
        }
        m_rawSpec = rawSpec;
        updateTransformations();
    }

    private void updateTransformations() {
        final LinkedHashMap<TypedReaderColumnSpec<T>, MutableColumnTransformation<T>> newColumns =
            new LinkedHashMap<>();
        final Set<TypedReaderColumnSpec<T>> knownColumns = new HashSet<>();
        // the null spec corresponds to the placeholder for new columns and thus is always known
        knownColumns.add(TypedReaderColumnSpec.getNull());
        boolean tableChanged = false;
        int idx = 0;
        m_union.clear();
        m_intersection.clear();
        m_union.add(m_newColTransformationPlaceholder);
        m_intersection.add(m_newColTransformationPlaceholder);
        final Set<TypedReaderColumnSpec<T>> intersection = m_rawSpec.getIntersection().stream().collect(toSet());
        for (TypedReaderColumnSpec<T> column : m_rawSpec.getUnion()) {
            MutableColumnTransformation<T> transformation = m_bySpec.get(column);
            final boolean isInIntersection = intersection.contains(column);
            if (transformation != null) {
                knownColumns.add(column);
                tableChanged |= transformation.setPosition(idx);
                transformation.setOriginalPosition(idx);
            } else {
                final ProductionPath productionPath = m_defaultProductionPathFn.apply(column.getType());
                transformation = new MutableColumnTransformation<>(createDefaultSpec(column), column, idx,
                    getNameAfterInit(column), productionPath, idx, keepUnknownColumns());
                newColumns.put(column, transformation);
                m_byName.put(transformation.getName(), transformation);
            }
            if (isInIntersection) {
                m_intersection.add(transformation);
            }
            m_union.add(transformation);
            idx++;
        }
        tableChanged |= !newColumns.isEmpty();
        final int oldNumberOfTransformations = m_bySpec.size();
        // identify removed columns (we can't remove them directly as this would cause a ConcurrentModificationException
        final Set<TypedReaderColumnSpec<T>> removedColumns = m_bySpec.keySet().stream()//
            .filter(c -> !knownColumns.contains(c))//
            .collect(toSet());

        // remove columns that are no longer present from m_bySpec and m_byName
        removedColumns.stream().map(m_bySpec::remove)//
            .forEach(t -> m_byName.remove(t.getName(), t));

        tableChanged |= oldNumberOfTransformations != m_bySpec.size();

        // add new columns
        m_bySpec.putAll(newColumns);
        sortByOutputIdx();

        if (tableChanged) {
            fireTableDataChanged();
        }
    }

    private boolean keepUnknownColumns() {
        return m_newColTransformationPlaceholder.keep();
    }

    private int getPositionForUnknownColumns() {
        return m_newColTransformationPlaceholder.getPosition();
    }

    @Override
    public void load(final TableTransformation<T> transformationModel) {
        m_rawSpec = transformationModel.getRawSpec();
        m_union.clear();
        m_intersection.clear();
        final int newColPosition = transformationModel.getPositionForUnknownColumns();
        m_newColTransformationPlaceholder.setPosition(newColPosition);
        m_union.add(m_newColTransformationPlaceholder);
        m_intersection.add(m_newColTransformationPlaceholder);
        m_byName.clear();
        m_bySpec.clear();
        final ColumnFilterMode colFilterMode = transformationModel.getColumnFilterMode();
        m_columnFilterMode.setColumnFilterModel(colFilterMode);
        final Set<TypedReaderColumnSpec<T>> intersection = m_rawSpec.getIntersection().stream().collect(toSet());
        int idx = 0;
        for (TypedReaderColumnSpec<T> column : m_rawSpec.getUnion()) {
            final MutableColumnTransformation<T> transformation;
            // in case we are in intersection mode, the TranformationModel might not have a
            // Transformation for all columns in the union
            if (transformationModel.hasTransformationFor(column)) {
                transformation = createMutableTransformation(transformationModel.getTransformation(column), idx);
            } else {
                transformation = new MutableColumnTransformation<>(createDefaultSpec(column), column, idx,
                    getNameAfterInit(column), m_defaultProductionPathFn.apply(column.getType()),
                    getPositionForUnknownColumns(), keepUnknownColumns());
            }
            int pos = transformation.getPosition();
            if (pos >= newColPosition) {
                // increment position to make space for the placeholder row
                transformation.setPosition(pos + 1);
            }
            m_bySpec.put(column, transformation);
            m_byName.put(transformation.getName(), transformation);
            m_union.add(transformation);
            if (intersection.contains(column)) {
                m_intersection.add(transformation);
            }
        }
        sortByOutputIdx();
        fireTableDataChanged();
    }

    private MutableColumnTransformation<T> createMutableTransformation(final ColumnTransformation<T> transformation,
        final int idx) {
        return new MutableColumnTransformation<>(createDefaultSpec(transformation.getExternalSpec()),
            transformation.getExternalSpec(), idx, transformation.getName(), transformation.getProductionPath(),
            transformation.getPosition(), transformation.keep());
    }

    private DataColumnSpec createDefaultSpec(final TypedReaderColumnSpec<T> column) {
        final ProductionPath prodPath = m_defaultProductionPathFn.apply(column.getType());
        return new DataColumnSpecCreator(MultiTableUtils.getNameAfterInit(column),
            prodPath.getConverterFactory().getDestinationType()).createSpec();
    }

    Set<String> getCurrentNames() {
        return m_bySpec.values().stream()//
            .map(MutableColumnTransformation::getName)//
            .collect(Collectors.toSet());
    }

    Set<String> getNamesExcept(final int rowIndex) {
        return m_bySpec.values().stream()//
            .filter(c -> c.getPosition() != rowIndex)//
            .map(MutableColumnTransformation::getName)//
            .collect(toSet());
    }

    @Override
    public void addChangeListener(final ChangeListener listener) {
        m_changeListeners.add(listener);//NOSONAR a small price to pay for thread-safety
    }

    @Override
    public void removeChangeListener(final ChangeListener listener) {
        m_changeListeners.remove(listener);//NOSONAR a small price to pay for thread-safety
    }

    private void notifyChangeListeners() {
        for (ChangeListener listener : m_changeListeners) {
            listener.stateChanged(m_changeEvent);
        }
    }

    @Override
    public int getRowCount() {
        if (m_rawSpec == null) {
            return 0;
        }
        return getColumnFilterMode() == ColumnFilterMode.UNION ? m_union.size() : m_intersection.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(final int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {//NOSONAR, stupid rule
        final MutableColumnTransformation<T> transformation = getTransformation(rowIndex);
        if (columnIndex == REORDER) {
            return SharedIcons.DRAG_HANDLE.get();
        } else if (columnIndex == KEEP) {
            return transformation.keep();
        } else if (columnIndex == COLUMN) {
            return transformation.getDefaultSpec();
        } else if (columnIndex == RENAME) {
            if (transformation == m_newColTransformationPlaceholder) {
                return "";
            }
            return transformation.isRenamed() ? transformation.getName() : "";
        } else if (columnIndex == TYPE) {
            return transformation.getProductionPath();
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final MutableColumnTransformation<T> transformation = getTransformation(rowIndex);
        final Object oldValue = getValueAt(rowIndex, columnIndex);
        boolean alreadyFiredTableDataChange = false;
        if (columnIndex == REORDER) {
            throw new IllegalArgumentException("Can't set the reorder column.");
        } else if (columnIndex == KEEP) {
            transformation.setKeep((boolean)aValue);
        } else if (columnIndex == COLUMN) {
            throw new IllegalArgumentException("Can't set the default column.");
        } else if (columnIndex == RENAME) {
            alreadyFiredTableDataChange = updateName(aValue, transformation);
        } else if (columnIndex == TYPE) {
            transformation.setProductionPath((ProductionPath)aValue);
        } else {
            throw new IndexOutOfBoundsException();
        }
        if (!alreadyFiredTableDataChange && !Objects.equals(oldValue, aValue)) {
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    /**
     * Updates the name and verifies that the set of names is still valid i.e. contains no duplicates.
     *
     * @param aValue the new name as object
     * @param transformation the transformation to update
     * @return {@code true} if we already fired a table event
     */
    private boolean updateName(final Object aValue, final MutableColumnTransformation<T> transformation) {
        final String stringValue = (String)aValue;
        final String newName =
            stringValue.isEmpty() ? transformation.getOriginalName() : stringValue;
        final String oldName = transformation.getName();
        boolean fireEventForOtherRows = false;
        if (!newName.equals(oldName)) {
            m_byName.remove(oldName, transformation);
            final Set<MutableColumnTransformation<T>> colsWithOldName = m_byName.get(oldName);
            if (!oldName.isEmpty() && colsWithOldName.size() == 1) {
                colsWithOldName.forEach(t -> t.setIsValid(true));
                fireEventForOtherRows = true;
            }
            m_byName.put(newName, transformation);
            final Set<MutableColumnTransformation<T>> colsWithNewName = m_byName.get(newName);
            if (colsWithNewName.size() > 1) {
                colsWithNewName.forEach(t -> t.setIsValid(false));
                fireEventForOtherRows = true;
            } else {
                transformation.setIsValid(true);
            }
            transformation.setName(newName);
        }
        if (fireEventForOtherRows) {
            fireTableDataChanged();
        }
        return fireEventForOtherRows;
    }

    boolean isValidNameForRow(final int rowIndex, final String name) {
        if (name.isEmpty()) {
            return true;
        } else {
            final String currentName = m_union.get(rowIndex).getName();
            return currentName.equals(name) || !m_byName.containsKey(name);
        }
    }

    boolean isValid(final int rowIndex) {
        return getTransformation(rowIndex).isValid();
    }

    boolean isNameValid(final int rowIndex) {
        final MutableColumnTransformation<T> transformation = getTransformation(rowIndex);
        if (transformation == m_newColTransformationPlaceholder) {
            return true;
        }
        return !transformation.isRenamed() || transformation.isValid();
    }

    boolean isSpecValid(final int rowIndex) {
        final MutableColumnTransformation<T> transformation = getTransformation(rowIndex);
        if (transformation == m_newColTransformationPlaceholder) {
            return true;
        }
        return transformation.isRenamed() || transformation.isValid();
    }

    void setIsValid(final int rowIndex, final boolean isValid) {
        getTransformation(rowIndex).setIsValid(isValid);
    }

    void reorder(final int fromIndex, final int toIndex) {
        if (fromIndex == toIndex) {
            return; // nothing changes
        }
        final MutableColumnTransformation<T> moved = getTransformation(fromIndex);
        final int unionFromIndex = moved.getPosition();
        if (fromIndex < toIndex) {
            final int unionToIndex = getTransformation(toIndex - 1).getPosition();
            for (int i = unionFromIndex; i < unionToIndex; i++) {
                final MutableColumnTransformation<T> k = m_union.get(i + 1);
                k.setPosition(i);
            }
            moved.setPosition(unionToIndex);
        } else {
            final int unionToIndex = getTransformation(toIndex).getPosition();
            for (int i = unionFromIndex; i >= unionToIndex; i--) {
                final MutableColumnTransformation<T> k = m_union.get(i);
                k.setPosition(i + 1);
            }
            moved.setPosition(unionToIndex);
        }
        sortByOutputIdx();
        fireTableDataChanged();
    }

    private MutableColumnTransformation<T> getTransformation(final int rowIndex) {
        return getColumnFilterMode() == ColumnFilterMode.UNION ? m_union.get(rowIndex) : m_intersection.get(rowIndex);
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {// NOSONAR, stupid rule
        if (columnIndex == REORDER) {
            return Icon.class;
        } else if (columnIndex == KEEP) {
            return Boolean.class;
        } else if (columnIndex == COLUMN) {
            return DataColumnSpec.class;
        } else if (columnIndex == RENAME) {
            return String.class;
        } else if (columnIndex == TYPE) {
            return ProductionPath.class;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        if (getTransformation(rowIndex) == m_newColTransformationPlaceholder && columnIndex != KEEP) {
            return false;
        }
        return columnIndex == KEEP || columnIndex == RENAME || columnIndex == TYPE;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (m_enabled != enabled) {
            m_enabled = enabled;
            fireTableDataChanged();
        }
    }

    private ColumnFilterMode getColumnFilterMode() {
        return m_columnFilterMode.getColumnFilterMode();
    }

    ColumnFilterModeModel getColumnFilterModeModel() {
        return m_columnFilterMode;
    }

    @Override
    public TableTransformation<T> getTransformationModel() {
        int idx = 0;
        List<ColumnTransformation<T>> transformations = new ArrayList<>();
        for (ColumnTransformation<T> t : m_union) {
            if (t == m_newColTransformationPlaceholder) {
                // the placeholder is not added as transformation
                continue;
            }
            transformations.add(new ImmutableColumnTransformation<>(t.getExternalSpec(), t.getProductionPath(),
                t.keep(), idx, t.getName()));
            idx++;
        }
        return new DefaultTableTransformation<>(m_rawSpec, transformations, getColumnFilterMode(), keepUnknownColumns(),
            getPositionForUnknownColumns());
    }

}
