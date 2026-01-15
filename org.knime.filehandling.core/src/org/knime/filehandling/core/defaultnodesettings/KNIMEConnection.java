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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.net.URIBuilder;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.util.KnimeUrlType;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Class encapsulating the different types of KNIME file system connections.
 * <p>
 * This class does not perform any I/O. It mainly defines the mapping between
 * {@link KnimeUrlType KNIME-specific URL authorities/paths} and their
 * corresponding file systems (FS) - to which we are able to connect in the AP.
 *
 * The methods in this class are utilities for resolving relative vs. absolute
 * FS specifiers, look up known mountpoints, and construct {@link URI URIs}.
 * </p>
 *
 * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class KNIMEConnection {

    /**
     * Enum stating the different types of KNIME file system connections
     *
     * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
     */
    public enum Type {
            /**
             * knime://knime.workflow/
             */
            WORKFLOW_RELATIVE(KnimeUrlType.WORKFLOW_RELATIVE),

            /**
             * knime://knime.space/
             *
             * <p>
             * NOTE: Internally, this is handled as a synonym of {@link #MOUNTPOINT_RELATIVE}.
             * This works for the local space as well, not only Hub spaces.
             * </p>
             * @since 5.10
             */
            HUB_SPACE_RELATIVE(KnimeUrlType.HUB_SPACE_RELATIVE),

            /**
             * knime://knime.mountpoint/
             */
            MOUNTPOINT_RELATIVE(KnimeUrlType.MOUNTPOINT_RELATIVE),

            /**
             * knime://knime.workflow/data/
             *
             * <p>
             * NOTE: While the {@link FSType#RELATIVE_TO_WORKFLOW_DATA_AREA} and the actual
             * settings value {@link RelativeTo#WORKFLOW_DATA} use {@code "knime.workflow.data"},
             * the actual {@link KnimeUrlType} has *NO* such identifier/type of the workflow data.
             *
             * This is in contrast to the other {@link #Type}s in this class, where their identifier
             * can be used directly as {@link KnimeUrlType}, as well.
             * </p>
             */
            WORKFLOW_DATA_RELATIVE(KnimeUrlType.WORKFLOW_RELATIVE, "data"),

            /**
             * knime://<mount-ID>>/
             */
            MOUNTPOINT_ABSOLUTE(KnimeUrlType.MOUNTPOINT_ABSOLUTE);

        private final String m_authority;

        private final String m_path;

        Type(final KnimeUrlType type) {
            this(type, null);
        }

        Type(final KnimeUrlType type, final String path) {
            m_authority = type.getAuthority();
            m_path = StringUtils.defaultIfBlank(path, null);
        }

        /**
         * Returns the scheme and host of the connection type.
         *
         * @return the scheme and host
         * @deprecated use {@link #toKnimeURI()} instead
         */
        @Deprecated(since = "5.10", forRemoval = true)
        public String getSchemeAndHost() {
            final var authority = Objects.requireNonNullElse(m_authority, "www.example.com");
            return String.format("%s://%s", KnimeUrlType.SCHEME, authority);
        }

        /**
         * Starts building a {@link URI} using the {@link URIBuilder}. At this point,
         * we do not return a fully-built {@link URI} yet, since mountpoint-absolute
         * KNIME URL/URIs do not have a static authority: it's the dynamic mount ID.
         *
         * @return the {@link URIBuilder} for the static KNIME URL/URIs properties
         * @throws URISyntaxException if building the {@link URI} goes wrong
         * @since 5.10
         */
        public URIBuilder buildURI() throws URISyntaxException {
            final var builder = new URIBuilder().setScheme(KnimeUrlType.SCHEME);
            if (m_authority != null) {
                // #setAuthority requires a non-null argument
                builder.setAuthority(URIAuthority.create(m_authority));
            }
            // #setPath can handle nullable arguments
            return builder.setPath(m_path);
        }
    }

    /** KNIME workflow relative connection */
    public static final KNIMEConnection WORKFLOW_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.WORKFLOW_RELATIVE, "Current workflow", RelativeTo.WORKFLOW);

    /** KNIME mount point relative connection */
    public static final KNIMEConnection HUB_SPACE_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.HUB_SPACE_RELATIVE, "Current space", RelativeTo.SPACE);

    /** KNIME mount point relative connection */
    public static final KNIMEConnection MOUNTPOINT_RELATIVE_CONNECTION =
        new KNIMEConnection(Type.MOUNTPOINT_RELATIVE, "Current mountpoint", RelativeTo.MOUNTPOINT);

    /** KNIME workflow data area relative connection */
    public static final KNIMEConnection WORKFLOW_DATA_RELATIVE_CONNECTION =
            new KNIMEConnection(Type.WORKFLOW_DATA_RELATIVE, "Current workflow data area", RelativeTo.WORKFLOW_DATA);

    /** Map of all available connections */
    private static final Map<String, KNIMEConnection> CONNECTIONS = new HashMap<>();
    static {
        CONNECTIONS.put(WORKFLOW_RELATIVE_CONNECTION.getId(), WORKFLOW_RELATIVE_CONNECTION);
        CONNECTIONS.put(HUB_SPACE_RELATIVE_CONNECTION.getId(), HUB_SPACE_RELATIVE_CONNECTION);
        CONNECTIONS.put(MOUNTPOINT_RELATIVE_CONNECTION.getId(), MOUNTPOINT_RELATIVE_CONNECTION);
        CONNECTIONS.put(WORKFLOW_DATA_RELATIVE_CONNECTION.getId(), WORKFLOW_DATA_RELATIVE_CONNECTION);
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
     * @param setting the {@link RelativeTo} settings option
     */
    private KNIMEConnection(final Type type, final String displayName, final RelativeTo setting) {
        this(type, displayName, setting.getSettingsValue());
    }

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

    private static KNIMEConnection createMountpointAbsoluteConnection(final String mountId) {
        return new KNIMEConnection(Type.MOUNTPOINT_ABSOLUTE, //
            String.format("%s", StringUtils.abbreviate(mountId, 30)), mountId);
    }

    /**
     * Gets or creates and a new mount point absolute connection based on given mount id.
     *
     * @param mountId the mount point identifier
     * @return a KNIMEConnection instance
     */
    public static final synchronized KNIMEConnection getOrCreateMountpointAbsoluteConnection(final String mountId) {
        if (mountId == null) {
            return null;
        }
        return CONNECTIONS.computeIfAbsent(mountId, KNIMEConnection::createMountpointAbsoluteConnection);
    }

    /**
     * Gets connection based on given mount id or null if connection does not exist.
     *
     * @param mountId the mount point identifier
     * @return a KNIMEConnection instance
     */
    public static final synchronized KNIMEConnection getConnection(final String mountId) {
        return CONNECTIONS.get(mountId);
    }

    /**
     * Returns whether a connection exists based on the mount id.
     *
     * @param id the mount point identifier
     * @return true if connection exists false otherwise
     */
    public static final synchronized boolean connectionExists(final String id) {
        return CONNECTIONS.containsKey(id);
    }

    /**
     * Returns all available KNIMEConnections as an array.
     *
     * @return all available KNIMEConnections as an array
     */
    public static final synchronized KNIMEConnection[] getAll() {
        return CONNECTIONS.values().stream().toArray(KNIMEConnection[]::new);
    }

    /**
     * Returns a connection type for a knime URL host.
     *
     * @param host the knime URL host
     * @return the corresponding connection type
     */
    public static KNIMEConnection.Type connectionTypeForHost(final String host) {
        return switch (host) {
            case "knime.workflow" -> Type.WORKFLOW_RELATIVE;
            case "knime.space" -> Type.HUB_SPACE_RELATIVE;
            case "knime.mountpoint" -> Type.MOUNTPOINT_RELATIVE;
            case "knime.workflow.data" -> Type.WORKFLOW_DATA_RELATIVE;
            default -> Type.MOUNTPOINT_ABSOLUTE;
        };
    }

    /**
     * Attempts to create a KNIME {@link URI} from the given file system specifier. It uses
     * the map of cached {@link KNIMEConnection}s internally, but does not modify it.
     *
     * All {@link KNIMEConnection}s created in the process are for having a representation
     * to build the {@link URI} out of. They are not cached/persisted.
     *
     * @param specifier the file system specifier, used as authority in the {@link URI}
     * @return the created {@link URI}, via {@link #toKnimeURI()}
     * @throws URISyntaxException if building the {@link URI} goes wrong
     */
    public static URI createURIFromFileSystemSpecifier(final String specifier) throws URISyntaxException {
        KNIMEConnection representation;
        if (connectionExists(specifier)) {
            representation = getConnection(specifier);
        } else {
            final var type = connectionTypeForHost(specifier);
            representation = new KNIMEConnection(type, specifier, specifier);
        }
        return representation.toKnimeURI();
    }

    /**
     * Returns the scheme and host of the connection type.
     *
     * @return the scheme and host of the connection type
     * @deprecated use {@link #toKnimeURI()} instead
     */
    @Deprecated(since = "5.10", forRemoval = true)
    public String getSchemeAndHost() {
        if (m_type == Type.MOUNTPOINT_ABSOLUTE) {
            return "knime://" + m_key;
        } else {
            return m_type.getSchemeAndHost();
        }
    }

    /**
     * Returns the {@link URI} built from the file system specifier and the mount ID.
     * Prefer this over {@link #getSchemeAndHost()}, as {@link Type#WORKFLOW_DATA_RELATIVE}
     * has a {@code "data"} path that is not returned in the scheme-host-string.
     *
     * @return the scheme and host of the connection type
     * @throws URISyntaxException if building the {@link URI} goes wrong
     */
    public URI toKnimeURI() throws URISyntaxException {
        final var builder = m_type.buildURI();
        if (m_type == Type.MOUNTPOINT_ABSOLUTE) {
            builder.setAuthority(URIAuthority.create(m_key));
        }
        return builder.build();
    }

    /**
     * @return whether the mountpoint is connected
     */
    public boolean isConnected() {
        try {
            return getType() != Type.MOUNTPOINT_ABSOLUTE || checkMountpointConnected();
        } catch (URISyntaxException ex) { // NOSONAR
            return false;
        }
    }

    private boolean checkMountpointConnected() throws URISyntaxException {
        CheckUtils.checkState(getType() == Type.MOUNTPOINT_ABSOLUTE, "Mountpoint-absolute required");

        if (WorkflowContextUtil.hasWorkflowContext()&& WorkflowContextUtil.isServerContext()) {
            final WorkflowContext workflowContext = WorkflowContextUtil.getWorkflowContext();
            final boolean isRemoteMountID = workflowContext.getRemoteMountId() //
                    .orElseThrow(() -> new IllegalStateException("No remote mount ID")) //
                    .equals(getId());

            if (isRemoteMountID && workflowContext.getRemoteRepositoryAddress().isPresent()
                && workflowContext.getServerAuthenticator().isPresent()) {
                return true;
            }
        }

        return MountPointFileSystemAccessService.instance().isAuthenticated(toKnimeURI());
    }

    /**
     * Determines if a mountpoint is valid. Three cases for validity are possible:
     *   1. Not an absolute-URL mountpoint.
     *   2. We are an executor operating on the same server as was configured via mount ID.
     *      Important: this assumes that the user has not changed the mount ID
     *      as we match on the default ID here!
     *   3. The mount point is registered as mounted in the global mountpoint table.
     *
     * @return whether the mountpoint is valid
     */
    public boolean isValid() {
        final var contextV2 = WorkflowContextUtil.getWorkflowContextV2Optional();
        final var isExecutor = contextV2 //
                .filter(cxt -> cxt.getExecutorInfo() instanceof JobExecutorInfo) //
                .isPresent();
        final var isMyDefaultMountId = contextV2 //
                .filter(cxt -> cxt.getLocationInfo() instanceof RestLocationInfo) //
                .map(cxt -> ((RestLocationInfo)cxt.getLocationInfo()).getDefaultMountId().equals(getId())) //
                .orElse(false);

        return getType() != Type.MOUNTPOINT_ABSOLUTE || (isExecutor && isMyDefaultMountId)
            || MountPointFileSystemAccessService.instance().getAllMountedIDs().contains(m_key);
    }

}
