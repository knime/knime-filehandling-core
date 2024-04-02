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
 *   May 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.util.Optional;
import java.util.function.Consumer;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Allows access to the {@link FSPath FSPaths} referred to by the {@link AbstractSettingsModelFileChooser} provided in
 * the constructor. The paths are also validated and respective exceptions are thrown if the settings yield invalid
 * paths.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FileChooserPathAccessor extends AbstractFileChooserPathAccessor {

    /**
     * Creates a new FileChooserAccessor for the provided {@link AbstractSettingsModelFileChooser} and
     * {@link FSConnection connection} (if provided).</br>
     * The settings are not validated in this constructor but instead if {@link #getOutputPath(Consumer)} or
     * {@link #getFSPaths(Consumer)} are called.
     *
     * @param settings {@link AbstractSettingsModelFileChooser} provided by the user
     * @param portObjectConnection connection retrieved from the file system port object (if the node has one)
     */
    public FileChooserPathAccessor(final AbstractSettingsModelFileChooser<?> settings,
        final Optional<FSConnection> portObjectConnection) { //NOSONAR
        super(new FileChooserPathAccessorSettings(settings.getLocation(), new FilterSettings(
            settings.getFilterModeModel().getFilterMode(), settings.getFilterModeModel().isIncludeSubfolders(),
            settings.getFilterModeModel().getFilterOptionsSettings(), settings.getFilterModeModel().isFollowLinks())),
            portObjectConnection);
    }

}
