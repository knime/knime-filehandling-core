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
 */
package org.knime.filehandling.core.fs.knime.local.relativeto.testing;

import java.io.IOException;
import java.util.Map;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.RelativeToFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.fs.knime.local.relativeto.fs.LocalRelativeToWorkflowDataFSConnection;
import org.knime.filehandling.core.fs.knime.local.workflowaware.LocalWorkflowAwareFileSystem;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;

/**
 * {@link FSTestInitializerProvider} for testing the workflow data area. It will create a
 * {@link FSCategory#CONNECTED} file system with a randomized working directory.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class RelativeToWorkflowDataFSTestInitializerProvider extends LocalRelativeToFSTestInitializerProvider {

    /**
     * Constructor.
     */
    public RelativeToWorkflowDataFSTestInitializerProvider() {
        super(FSType.RELATIVE_TO_WORKFLOW_DATA_AREA, //
            RelativeToFSConnectionConfig.CONNECTED_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC);
    }

    @SuppressWarnings("resource")
    @Override
    protected LocalRelativeToFSTestInitializer createTestInitializer(final Map<String, String> configuration)
        throws IOException {

        final var workingDir = generateRandomizedWorkingDir(LocalWorkflowAwareFileSystem.PATH_SEPARATOR,
            LocalWorkflowAwareFileSystem.PATH_SEPARATOR);
        final var config = new RelativeToFSConnectionConfig(workingDir, RelativeTo.WORKFLOW_DATA);
        return new RelativeToWorkflowDataFSTestInitializer(new LocalRelativeToWorkflowDataFSConnection(config));
    }
}
