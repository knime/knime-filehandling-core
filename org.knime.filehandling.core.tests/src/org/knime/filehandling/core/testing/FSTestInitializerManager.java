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
 *   Dec 18, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.testing;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * Manager which knows all registered {@link FSTestInitializerProvider} instances and can instantiate a configured
 * {@link FSTestInitializer} for each registered {@link FSTestInitializerProvider}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSTestInitializerManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FSTestInitializerManager.class);

    /** The id of the FS test initializer provider extension point. */
    public static final String EXT_POINT_ID = "org.knime.filehandling.FSTestInitializerProvider";

    /** The attribute of the extension point. */
    public static final String EXT_POINT_ATTR_DF = "ProviderClass";

    private static FSTestInitializerManager instance;

    private final Map<String, FSTestInitializerProvider> m_providers = new HashMap<>();

    private FSTestInitializerManager() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                        + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }
                try {
                    final FSTestInitializerProvider extension =
                        (FSTestInitializerProvider)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    final String type = extension.getFSType();

                    if (m_providers.containsKey(type)) {
                        throw new IllegalStateException(
                            String.format("The fs-type '%s' has already been registered", type));
                    }

                    m_providers.put(type, extension);
                } catch (final Throwable t) {
                    LOGGER.error("Problems during initialization of provider operator (with id '" + operator + "'.)",
                        t);
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering aggregation operator extensions", e);
        }
    }

    /**
     * Returns the instance of this manager.
     *
     * @return get the sole instance of the {@link FSTestInitializerManager}
     */
    public static synchronized FSTestInitializerManager instance() {
        if (instance == null) {
            instance = new FSTestInitializerManager();
        }
        return instance;
    }

    /**
     * Creates a {@link FSTestInitializer} of the provided type, configured according to the configuration. This method
     * may perform I/O, such an opening network connections and therefore throws an {@link IOException}.
     *
     * @param fsType The type of the file system.
     * @param configuration The configuration of the file system.
     * @return a configured file system of the provided type
     * @throws IOException
     */
    public FSTestInitializer createInitializer(final String fsType, final Map<String, String> configuration) throws IOException {
        final FSTestInitializerProvider provider = m_providers.get(fsType);
        if (provider == null) {
            throw new IllegalArgumentException(
                String.format("The file system type '%s' has no registered test initializer provider", fsType));
        }
        return provider.setup(configuration);
    }

    /**
     * Provides the {@link FSLocationSpec} of a file system with the given type and configuration.
     *
     * @param fsType The type of the file system.
     * @param configuration The configuration of the file system.
     * @return a configured file system of the given type.
     */
    public FSLocationSpec createFSLocationSpec(final String fsType, final Map<String, String> configuration) {
        final FSTestInitializerProvider provider = m_providers.get(fsType);
        if (provider == null) {
            throw new IllegalArgumentException(
                String.format("The file system type '%s' has no registered test initializer provider", fsType));
        }
        return provider.createFSLocationSpec(configuration);
    }

    /**
     * Returns the keys for all registered test initializer providers.
     *
     * @return the keys for all registered test initializer providers
     */
    public List<String> getAllTestInitializerKeys() {
        return m_providers.keySet().stream().collect(Collectors.toList());
    }
}
