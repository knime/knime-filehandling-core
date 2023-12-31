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
 *   Mar 12, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knime.local.relativeto.fs;

import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.base.BaseFSConnection;
import org.knime.filehandling.core.connections.config.RelativeToFSConnectionConfig;
import org.knime.filehandling.core.filechooser.AbstractFileChooserBrowser;
import org.knime.filehandling.core.fs.knime.relativeto.export.RelativeToFileSystemBrowser;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * {@link FSConnection} for the Relative-to mountpoint file system. It is possible to create a connected or convenience
 * file system, also the working directory is configurable. The location of the mountpoint in the underyling local file
 * system is determined using the KNIME {@link WorkflowContext}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public class LocalRelativeToMountpointFSConnection extends BaseFSConnection {

    private final LocalRelativeToFileSystem m_fileSystem;

    private final boolean m_browserShouldRelativizeSelectedPath;

    /**
     * Creates a new connection using the given config.
     *
     * @param config The config to use.
     */
    public LocalRelativeToMountpointFSConnection(final RelativeToFSConnectionConfig config) {

        final var workflowContext = WorkflowContextUtil.getWorkflowContext();
        if (WorkflowContextUtil.isServerContext(workflowContext)) {
            throw new UnsupportedOperationException(
                "Unsupported temporary copy of workflow detected. Relative to does not support server execution.");
        }

        final var localMountId = workflowContext.getMountpointURI()
            .orElseThrow(() -> new IllegalStateException("Cannot determine ID of local mountpoint")).getAuthority();
        final var localMountpointRoot = workflowContext.getMountpointRoot().toPath().toAbsolutePath().normalize();

        m_fileSystem = new LocalRelativeToFileSystem(localMountId, //
            localMountpointRoot, //
            RelativeTo.MOUNTPOINT, //
            config.getWorkingDirectory(), //
            config.getFSLocationSpec());

        m_browserShouldRelativizeSelectedPath = config.browserShouldRelativizeSelectedPath();
    }

    @Override
    public LocalRelativeToFileSystem getFileSystem() {
        return m_fileSystem;
    }

    @Override
    protected AbstractFileChooserBrowser createFileSystemBrowser() {
        final var browsingHomeAndDefault = m_fileSystem.getWorkingDirectory();
        return new RelativeToFileSystemBrowser(m_fileSystem,//
            browsingHomeAndDefault,//
            browsingHomeAndDefault,//
            m_browserShouldRelativizeSelectedPath);
    }
}
