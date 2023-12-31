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
 *   Jun 4, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.uriexport.base;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Helper class to generate legacy knime:// URLs.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class LegacyKNIMEUriExporterHelper {

    private LegacyKNIMEUriExporterHelper() {
    }

    /**
     * Creates a workflow- or mountpoint-relative knime:// URL of the given type for this path.
     *
     * @param type The {@link RelativeTo} type to assume for the given path (workflow-relative, mountpoint-relative,
     *            ...)
     * @param path The path for which to generate a URL.
     * @return a <code>knime://</code> protocol URL
     */
    public static URI createRelativeKNIMEProtocolURI(final RelativeTo type, final FSPath path) {
        try {
            switch (type) {
                case MOUNTPOINT:
                    return toMountpointRelativeURI(path);
                case WORKFLOW:
                    return toWorkflowRelativeURI(path);
                case WORKFLOW_DATA:
                    return toWorkflowDataRelativeURI(path);
                case SPACE:
                    return toSpaceRelativeUri(path);
                default:
                    throw new IllegalArgumentException("Illegal type " + type);
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Creates a mountpoint-absolute knime:// URL for this path.
     *
     * @param mountID The name of the mountpoint
     * @param path The path
     * @return a <code>knime://</code> protocol URL
     */
    public static URI createAbsoluteKNIMEProtocolURI(final String mountID, final FSPath path) {
        try {
            return new URI("knime", mountID, path.getURICompatiblePath(), null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static URI toWorkflowDataRelativeURI(final FSPath path) throws URISyntaxException {
        final String uriPath = String.format("/data%s", path.getURICompatiblePath());
        return new URI("knime", "knime.workflow", uriPath, null);
    }

    private static URI toWorkflowRelativeURI(final FSPath path) throws URISyntaxException {
        @SuppressWarnings("resource")
        final Path workDir = path.getFileSystem().getWorkingDirectory().normalize();
        final FSPath relativized = (FSPath)workDir.relativize(path.toAbsolutePath().normalize());
        final String uriPath =
            FSPath.URI_SEPARATOR + String.join(FSPath.URI_SEPARATOR, relativized.stringStream().toArray(String[]::new));
        return new URI("knime", "knime.workflow", uriPath, null);
    }

    private static URI toMountpointRelativeURI(final FSPath path) throws URISyntaxException {
        return new URI("knime", "knime.mountpoint", path.getURICompatiblePath(), null);
    }

    private static URI toSpaceRelativeUri(final FSPath path) throws URISyntaxException{
        return new URI("knime", "knime.space", path.getURICompatiblePath(), null);
    }
}
