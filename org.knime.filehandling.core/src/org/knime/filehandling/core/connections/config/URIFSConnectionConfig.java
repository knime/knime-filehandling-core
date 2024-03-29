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
 *   Jun 2, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.config;

import java.net.URI;
import java.time.Duration;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig;

/**
 * {@link FSConnectionConfig} implementation for the Custom/KNIME URL file system.It is unlikely that you will have to
 * use this class directly. To create a configured Custom/KNIME URL file system, please use
 * {@link DefaultFSConnectionFactory}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public class URIFSConnectionConfig extends BaseFSConnectionConfig {

    /**
     * Default connect/read timeout for URI filesystem in ms.
     */
    public static final int DEFAULT_TIMEOUT_MILLIS = 10000; //10s

    private URI m_uri;

    private Duration m_timeout = Duration.ofMillis(DEFAULT_TIMEOUT_MILLIS);

    /**
     * Constructor.
     */
    public URIFSConnectionConfig() {
        super("/", false);
    }

    /**
     * @return the connect/read timeout
     */
    public Duration getTimeout() {
        return m_timeout;
    }

    /**
     * @param timeout the connect/read timeout to use
     */
    public void setTimeout(final Duration timeout) {
        m_timeout = timeout;
    }

    /**
     * @return the URI
     */
    public URI getURI() {
        return m_uri;
    }

    /**
     * @param uri the URI to set
     */
    public void setURI(final URI uri) {
        m_uri = uri;
    }
}
