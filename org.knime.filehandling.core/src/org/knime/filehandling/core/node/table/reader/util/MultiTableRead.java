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
 *   Nov 14, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;

/**
 * Encapsulates information necessary to read tables from multiple items.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external data types
 */
public interface MultiTableRead<T> {

    /**
     * Returns the {@link DataTableSpec} of the currently read table.
     *
     * @return the {@link DataTableSpec} of the currently read table
     */
    DataTableSpec getOutputSpec();

    /**
     * Allows to create the {@link TableSpecConfig}.
     *
     * @return the {@link TableSpecConfig}
     */
    TableSpecConfig<T> getTableSpecConfig();

    /**
     * Creates a {@link PreviewRowIterator} that is backed by this {@link MultiTableRead}.
     *
     * @return a {@link PreviewRowIterator} for use in the dialog
     */
    PreviewRowIterator createPreviewIterator();

    /**
     * Fills the provided {@link RowOutput} with the data from this {@link MultiTableRead}.
     *
     * @param output to push to
     * @param exec for progress monitoring and canceling
     * @param fsFactory the {@link FileStoreFactory} to use for cell creation
     * @throws Exception if something goes awry
     */
    // can't be specialized because the type mapping throws Exception
    void fillRowOutput(RowOutput output, ExecutionMonitor exec, FileStoreFactory fsFactory) throws Exception; // NOSONAR

    /**
     * Read the table.
     *
     * @param exec for table creation
     * @return the table
     * @throws Exception if something goes awry
     * @deprecated superseded by {@link #readTable(ExecutionContext, DataContainerSettings)}
     */
    @Deprecated(since = "5.3")
    default BufferedDataTable readTable(final ExecutionContext exec) throws Exception {
        return readTable(exec, DataContainerSettings.getDefault());
    }

    /**
     * Read the table, allow to specify settings for the data container (i.e. do not check row keys for uniqueness)
     *
     * @param exec for table creation
     * @param settings settings for the data container creation
     * @return the table
     * @throws Exception if something goes askew
     */
    default BufferedDataTable readTable(final ExecutionContext exec, final DataContainerSettings settings)
        throws Exception {
        final var output = new BufferedDataTableRowOutput(exec.createDataContainer(getOutputSpec(), settings));
        final var fsFactory = FileStoreFactory.createFileStoreFactory(exec);
        fillRowOutput(output, exec, fsFactory);
        return output.getDataTable();
    }

}