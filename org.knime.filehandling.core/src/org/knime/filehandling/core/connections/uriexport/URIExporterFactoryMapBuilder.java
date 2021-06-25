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
 *   Nov 27, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.uriexport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.uriexport.base.PathURIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;

/**
 * Builder class to make it easier to correctly assemble a map from {@link URIExporterID} to {@link URIExporterFactory}.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @since 4.3
 * @noreference non-public API
 */
public class URIExporterFactoryMapBuilder {

    private final HashMap<URIExporterID, URIExporterFactory> m_exporters = new HashMap<>();

    /**
     * Creates a new instance.
     */
    public URIExporterFactoryMapBuilder() {
        m_exporters.put(URIExporterIDs.PATH, PathURIExporterFactory.getInstance());
    }

    /**
     * Fluent API method to add an exporter factory.
     *
     * @param id The ID under which to add the {@link URIExporter}.
     * @param exporterFactory The {@link URIExporterFactory} to add.
     * @return this builder object.
     */
    public URIExporterFactoryMapBuilder add(final URIExporterID id, final URIExporterFactory exporterFactory) {
        CheckUtils.checkArgumentNotNull(exporterFactory, "URIExporterFactory must not be null");

        final URIExporterFactory oldValue = m_exporters.putIfAbsent(id, exporterFactory);
        if (oldValue != null) {
            throw new IllegalStateException("There already is a URIExporter Factory with ID " + id.toString());
        }
        return this;
    }

    /**
     * Finalizes this build into an immutable map.
     *
     * @return an immutable map from {@link URIExporterID} to {@link URIExporterFactory}.
     */
    public Map<URIExporterID, URIExporterFactory> build() {
        CheckUtils.checkArgument(m_exporters.get(URIExporterIDs.DEFAULT) != null,
            "No default URIExporterFactory has been specified.");
        CheckUtils.checkArgument(m_exporters.get(URIExporterIDs.DEFAULT) instanceof NoConfigURIExporterFactory,
            "Default URIExporterFactory must be a NoConfigURIExporterFactory.");

        if (m_exporters.containsKey(URIExporterIDs.DEFAULT_HADOOP)) {
            CheckUtils.checkArgument(
                m_exporters.get(URIExporterIDs.DEFAULT_HADOOP) instanceof NoConfigURIExporterFactory,
                "Default Hadoop URIExporterFactory must be a NoConfigURIExporterFactory.");
        }

        return Collections.unmodifiableMap(m_exporters);
    }
}
