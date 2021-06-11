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
 *   Jun 3, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.local.LocalFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.url.URIFSConnectionConfig;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class DefaultFSConnectionFactory {

    private DefaultFSConnectionFactory() {
    }

    public static FSConnection createLocalFSConnection() {
        try {
            return FSDescriptorRegistry.getFSDescriptor(FSType.LOCAL_FS) // NOSONAR connection closed later
                .orElseThrow(() -> new IllegalStateException("Local file system is not registered")) //
                .<LocalFSConnectionConfig> getConnectionFactory() //
                .createConnection(new LocalFSConnectionConfig());
        } catch (IOException ex) {
            throw new IllegalStateException("IOException thrown where it should never happen", ex);
        }
    }

    public static FSConnection createLocalFSConnection(final String workingDir) {
        try {
            return FSDescriptorRegistry.getFSDescriptor(FSType.LOCAL_FS) // NOSONAR connection closed later
                .orElseThrow(() -> new IllegalStateException("Local file system is not registered")) //
                .<LocalFSConnectionConfig> getConnectionFactory() //
                .createConnection(new LocalFSConnectionConfig(workingDir));
        } catch (IOException ex) {
            throw new IllegalStateException("IOException thrown where it should never happen", ex);
        }
    }

    public static FSConnection createCustomURLConnection(final FSLocation fsLocation) {
        CheckUtils.checkArgument(fsLocation.getFSCategory() == FSCategory.CUSTOM_URL,
            "FSLocation must have category CUSTOM_URL");
        return createCustomURLConnection(fsLocation.getPath(), extractCustomURLTimeout(fsLocation));
    }

    private static int extractCustomURLTimeout(final FSLocationSpec location) {
        final String timeoutString = location.getFileSystemSpecifier()
            .orElseThrow(() -> new IllegalArgumentException("A custom URL location must always specify a timeout."));
        try {
            return Integer.parseInt(timeoutString);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                String.format("The provided specifier for the URL location '%s' is not a valid timeout.", location),
                ex);
        }
    }

    public static FSConnection createCustomURLConnection(final String url) {
        return createCustomURLConnection(url, FileUtil.getDefaultURLTimeoutMillis());
    }

    public static FSConnection createCustomURLConnection(final String url, final int timeoutInMillis) {
        final URIFSConnectionConfig config = new URIFSConnectionConfig();
        config.setTimeout(Duration.ofMillis(timeoutInMillis));
        config.setURI(URI.create(url.replace(" ", "%20")));

        try {
            return FSDescriptorRegistry.getFSDescriptor(FSType.CUSTOM_URL) // NOSONAR connection closed later
                .orElseThrow(() -> new IllegalStateException("Custom/KNIME URL file system is not registered")) //
                .<URIFSConnectionConfig> getConnectionFactory() //
                .createConnection(config);
        } catch (IOException ex) {
            throw new IllegalStateException("IOException thrown where it should never happen", ex);
        }
    }

    public static FSConnection createRelativeToConnection(final RelativeTo type) {
        try {
            return FSDescriptorRegistry.getFSDescriptor(type.toFSType()) //
                .orElseThrow(() -> new IllegalStateException(type.toFSType().getName() + " file system is not registered"))
                .getConnectionFactory() //
                .createConnection(null);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
