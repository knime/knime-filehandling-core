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
 *   Jan 26, 2026 (paulbaernreuther): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDSerializer.EmptyConfigID;

/**
 * Utility methods for saving and loading ConfigIDs.
 *
 * @author Paul Baernreuther, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
public class ConfigIDSerializationUtil {

    private ConfigIDSerializationUtil() {
        // prevent instantiation
    }

    /**
     * Save the given {@link ConfigID} to the provided {@link NodeSettingsWO}.
     *
     * @param key the key under which the ConfigID is stored
     * @param id the ConfigID to save
     * @param nodeSettings the NodeSettingsWO to save to
     */
    public static void saveID(final String key, final ConfigID id, final NodeSettingsWO nodeSettings) {
        if (id == EmptyConfigID.INSTANCE) {
            // the TableSpecConfig being serialized was loaded from an old workflow and therefore has no ConfigID
            // this is done for backwards compatibility
        } else {
            id.save(nodeSettings.addNodeSettings(key));
        }
    }

    /**
     * Load the {@link ConfigID} from the provided {@link NodeSettingsRO}.
     *
     * @param key the key under which the ConfigID is stored
     * @param configIdLoader the loader to create the ConfigID from settings
     * @param settings the NodeSettingsRO to load from
     * @return the loaded ConfigID
     * @throws InvalidSettingsException if loading fails
     */
    public static ConfigID loadID(final String key, final ConfigIDLoader configIdLoader, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey(key)) {
            return configIdLoader.createFromSettings(settings.getNodeSettings(key));
        } else {
            // the node was last saved before 4.4 -> no config id is available therefore we return the empty config id
            return EmptyConfigID.INSTANCE;
        }
    }

}
