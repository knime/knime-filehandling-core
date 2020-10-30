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
 *   Oct 21, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.Transformation;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Takes a {@link RawSpec} and a {@link MultiTableReadConfig} and creates a {@link TransformationModel} compatible with
 * the {@link RawSpec} by either incorporating an existing {@link TransformationModel} or generating a new one.</br>
 * </br>
 * NOTE: If the old TransformationModel has {@link ColumnFilterMode#INTERSECTION} then all columns that are new to the new intersection are considered to be new even if they were already part of the old union.
 * Example:
 * <pre>
 * Incoming TransformationModel:
 *      ColumnFilterMode: Intersection
 *      Old RawSpec: Union: [A, B, C, D], Intersection [B, C]
 *      Positions: [A:0, B:1,<unknown>, C:2, D:3]
 *      keep unknown: true
 *      unknown position: 2
 *
 * New RawSpec: Union: [A, B, C, D], Intersection: [A, B, C]
 *
 * New TransformationModel:
 *      Positions: [B:0, A:1, D:2, C:3]
 * </pre>
 * Note how all positions change.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TransformationModelFactory<T> {

    private final ProductionPathProvider<T> m_prodPathProvider;

    TransformationModelFactory(final ProductionPathProvider<T> productionPathProvider) {
        m_prodPathProvider = productionPathProvider;
    }

    TransformationModel<T> create(final RawSpec<T> rawSpec, final MultiTableReadConfig<?> config) {
        if (config.hasTableSpecConfig()) {
            final TransformationModel<T> configuredTransformationModel =
                config.getTableSpecConfig().getTransformationModel();
            return createFromExisting(rawSpec, configuredTransformationModel);
        } else {
            return createDefaultTransformationModel(rawSpec, config);
        }
    }

    private TransformationModel<T> createDefaultTransformationModel(final RawSpec<T> rawSpec,
        final MultiTableReadConfig<?> config) {
        // there is no TableSpecConfig (e.g. when the dialog was saved with a then invalid path)
        // so we need to fallback to the old SpecMergeMode if available or default to UNION
        @SuppressWarnings("deprecation")
        final ColumnFilterMode columnFilterMode = config.getSpecMergeMode() == SpecMergeMode.INTERSECTION
            ? ColumnFilterMode.INTERSECTION : ColumnFilterMode.UNION;
        if (columnFilterMode == ColumnFilterMode.INTERSECTION) {
            CheckUtils.checkArgument(rawSpec.getIntersection().size() > 0, "The intersection of all specs is empty.");
        }
        TypedReaderTableSpec<T> union = rawSpec.getUnion();
        final List<Transformation<T>> transformations = new ArrayList<>(union.size());
        int idx = 0;
        for (TypedReaderColumnSpec<T> column : union) {
            transformations.add(new ImmutableTransformation<>(column,
                m_prodPathProvider.getDefaultProductionPath(column.getType()), true, idx, getNameAfterInit(column)));
            idx++;
        }
        return new DefaultTransformationModel<>(rawSpec, transformations, columnFilterMode, true,
            transformations.size());
    }

    private int calculateNewPosForUnknown(final Collection<Transformation<T>> relevantTransformations,
        final int storedPositionForUnknown) {
        int idx = 0;
        for (Transformation<T> transformation : relevantTransformations) {
            if (transformation.getPosition() >= storedPositionForUnknown) {
                return idx;
            }
            idx++;
        }
        return idx;
    }

    private TransformationModel<T> createFromExisting(final RawSpec<T> newRawSpec,
        final TransformationModel<T> existingModel) {
        final ColumnFilterMode colFilterMode = existingModel.getColumnFilterMode();
        LinkedHashMap<String, TypedReaderColumnSpec<T>> relevantColumns =
            colFilterMode.getRelevantSpec(newRawSpec).stream().collect(//
                toMap(MultiTableUtils::getNameAfterInit, //
                    Function.identity(), //
                    (c1, c2) -> c1, // never used because the spec doesn't contain duplicates
                    LinkedHashMap::new));
        LinkedHashMap<String, Transformation<T>> relevantTransformations = colFilterMode
            .getRelevantSpec(existingModel.getRawSpec()).stream().map(existingModel::getTransformation)//
            .filter(t -> relevantColumns.containsKey(t.getOriginalName()))// only keep transformations that are relevant for the new spec
            .sorted()// sort by position in output
            .collect(toMap(Transformation::getOriginalName, Function.identity(), (t1, t2) -> t1, LinkedHashMap::new));

        final int unknownPos =
            calculateNewPosForUnknown(relevantTransformations.values(), existingModel.getPositionForUnknownColumns());

        final List<TypedReaderColumnSpec<T>> unknowns = newRawSpec.getUnion().stream()//
            .filter(e -> !relevantTransformations.containsKey(getNameAfterInit(e)))//
            .collect(toList());

        final List<Transformation<T>> newTransformations = new ArrayList<>();
        final boolean keepUnknownColumns = existingModel.keepUnknownColumns();
        int idx = 0;
        final Iterator<Transformation<T>> existingTransformationIterator = relevantTransformations.values().iterator();
        for (; idx < unknownPos; idx++) {
            assert existingTransformationIterator.hasNext();
            final Transformation<T> existingTransformation = existingTransformationIterator.next();
            final TypedReaderColumnSpec<T> newSpec = relevantColumns.get(existingTransformation.getOriginalName());
            newTransformations.add(createFromExisting(existingTransformation, newSpec, idx));
        }
        for (TypedReaderColumnSpec<T> unknownColumn : unknowns) {
            newTransformations.add(new ImmutableTransformation<>(unknownColumn,
                m_prodPathProvider.getDefaultProductionPath(unknownColumn.getType()), keepUnknownColumns, idx,
                getNameAfterInit(unknownColumn)));
            idx++;
        }
        for (; existingTransformationIterator.hasNext(); idx++) {
            final Transformation<T> existingTransformation = existingTransformationIterator.next();
            newTransformations.add(
                createFromExisting(existingTransformation, relevantColumns.get(existingTransformation.getOriginalName()), idx));
        }
        return new DefaultTransformationModel<>(newRawSpec, newTransformations, colFilterMode, keepUnknownColumns,
            unknownPos + unknowns.size());
    }

    private ImmutableTransformation<T> createFromExisting(final Transformation<T> existingTransformation,
        final TypedReaderColumnSpec<T> newSpec, final int newPos) {
        final ProductionPath prodPath = determineProductionPath(newSpec, existingTransformation);
        return new ImmutableTransformation<>(newSpec, prodPath, existingTransformation.keep(), newPos,
            existingTransformation.getName());
    }

    private ProductionPath determineProductionPath(final TypedReaderColumnSpec<T> column,
        final Transformation<T> transformation) {
        ProductionPath prodPath;
        final T externalType = column.getType();
        if (externalType.equals(transformation.getExternalSpec().getType())) {
            // same external type, so it's save to use the configured production path
            prodPath = transformation.getProductionPath();
        } else {
            final DataType configuredKnimeType =
                transformation.getProductionPath().getConverterFactory().getDestinationType();
            // check if we can convert from the new external type to the configured knime type
            // if that's not possible fall back onto the default
            prodPath = m_prodPathProvider.getAvailableProductionPaths(externalType).stream()//
                .filter(p -> p.getConverterFactory().getDestinationType().equals(configuredKnimeType))//
                .findFirst()//
                .orElseGet(() -> m_prodPathProvider.getDefaultProductionPath(externalType));
        }
        return prodPath;
    }

}
