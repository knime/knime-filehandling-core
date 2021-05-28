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
 *   Sep 11, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellEditor;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;

/**
 * {@link TableCellEditor} that allows to pick a {@link ProductionPath} from a combo box.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ProductionPathCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;

    private final JComboBox<ProductionPathOrDataType> m_items = new JComboBox<>();

    private final List<ProductionPathOrDataType> m_unknownColumnsItems = new ArrayList<>();

    ProductionPathCellEditor(final ListCellRenderer<ProductionPathOrDataType> renderer,
        final TableTransformationTableModel<?> model) {
        m_items.setRenderer(renderer);
        m_items.addActionListener(e -> stopCellEditing());
        m_unknownColumnsItems.add(ProductionPathOrDataType.DEFAULT);
        model.getAvailableDataTypes().stream()//
            .sorted(ProductionPathCellEditor::compareDataTypes)//
            .map(ProductionPathOrDataType::new)//
            .forEach(m_unknownColumnsItems::add);// NOSONAR
    }

    @Override
    public Object getCellEditorValue() {
        return m_items.getSelectedItem();
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        TableTransformationTableModel<?> model = (TableTransformationTableModel<?>)table.getModel();
        m_items.removeAllItems();
        if (model.isUnknownColumnsRow(row)) {
            fillItemsWithAvailableTypes();
        } else {
            fillItemsWithPathsForCurrentRow(row, model);
        }
        m_items.setSelectedItem(value);
        return m_items;
    }

    private void fillItemsWithAvailableTypes() {
        m_unknownColumnsItems.forEach(m_items::addItem);
    }

    private void fillItemsWithPathsForCurrentRow(final int row, final TableTransformationTableModel<?> model) {
        final List<ProductionPath> availablePaths = model.getProductionPaths(row);
        availablePaths.sort(ProductionPathCellEditor::compare);
        availablePaths.stream()//
            .map(ProductionPathOrDataType::new)//
            .forEach(m_items::addItem);
    }

    private static int compare(final ProductionPath p1, final ProductionPath p2) {
        return compareDataTypes(p1.getDestinationType(), p2.getDestinationType());
    }

    private static int compareDataTypes(final DataType t1, final DataType t2) {
        return t1.toPrettyString().compareTo(t2.toPrettyString());
    }

}
