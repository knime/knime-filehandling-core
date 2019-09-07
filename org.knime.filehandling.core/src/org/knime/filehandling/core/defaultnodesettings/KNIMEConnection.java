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
 *   Aug 21, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Class encapsulating the different types of KNIME file system connections.
 *
 * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
 */
public class KNIMEConnection {

    /**
     * Enum stating the different types of KNIME file system connections
     *
     * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
     */
    public enum Type {
            /**
             * knime://knime.node/
             */
            NODE_RELATIVE,

            /**
             * knime://knime.workflow/
             */
            WORKFLOW_RELATIVE,

            /**
             * knime://knime.mountpoint/
             */
            MOUNTPOINT_RELATIVE,

            /**
             * knime://<mount-ID>>/
             */
            MOUNTPOINT_ABSOLUTE;
    }

    /** KNIME node relative connection */
    public static final KNIMEConnection NODE_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.NODE_RELATIVE, "Node-relative", "knime.node");

    /** KNIME workflow relative connection */
    public static final KNIMEConnection WORKFLOW_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.WORKFLOW_RELATIVE, "Workflow-relative", "knime.workflow");

    /** KNIME mount point relative connection */
    public static final KNIMEConnection MOUNTPOINT_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.MOUNTPOINT_RELATIVE, "Mountpoint-relative", "knime.mountpoint");

    /** Map of all available connections */
    private static final Map<String, KNIMEConnection> CONNECTIONS = new HashMap<>();
    static {
        CONNECTIONS.put(NODE_RELATIVE_CONNECTION.getId(), NODE_RELATIVE_CONNECTION);
        CONNECTIONS.put(WORKFLOW_RELATIVE_CONNECTION.getId(), WORKFLOW_RELATIVE_CONNECTION);
        CONNECTIONS.put(MOUNTPOINT_RELATIVE_CONNECTION.getId(), MOUNTPOINT_RELATIVE_CONNECTION);
    }

    /** Type of connection */
    private final Type m_type;

    /** Display name of the connection */
    private final String m_displayName;

    /** Identifier of the connection */
    private final String m_key;

    /**
     * Creates a new instance of {@code KNIMEConnection}.
     *
     * @param type type of connection
     * @param displayName the display name of the connection
     * @param id the identifier
     */
    private KNIMEConnection(final Type type, final String displayName, final String id) {
        m_type = type;
        m_displayName = displayName;
        m_key = id;
    }

    /**
     * Returns the id of the connection.
     *
     * @return the id of the connection
     */
    public final String getId() {
        return m_key;
    }

    /**
     * Returns the type of the connection.
     *
     * @return the type of the connection
     */
    public final Type getType() {
        return m_type;
    }

    @Override
    public String toString() {
        return m_displayName;
    }

    /**
     * Gets or creates and a new mount point absolute connection based on given mount id.
     *
     * @param mountId the mount point identifier
     * @return a KNIMEConnection instance
     */
    public static final synchronized KNIMEConnection getOrCreateMountpointAbsoluteConnection(final String mountId) {
        if (!CONNECTIONS.containsKey(mountId)) {
            CONNECTIONS.put(mountId, new KNIMEConnection(Type.MOUNTPOINT_ABSOLUTE,
                String.format("Mountpoint (%s)", mountId), mountId));
        }

        return CONNECTIONS.get(mountId);
    }

    /**
     * Returns all available KNIMEConnections as an array.
     *
     * @return all available KNIMEConnections as an array
     */
    public static final synchronized KNIMEConnection[] getAll() {
        return CONNECTIONS.values().stream().toArray(KNIMEConnection[]::new);
    }
}