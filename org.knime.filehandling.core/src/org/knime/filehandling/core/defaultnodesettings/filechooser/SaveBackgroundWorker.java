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
 *   May 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Computes the status message for dialogs of type "save".
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class SaveBackgroundWorker implements Callable<StatusMessage> {

    private static final DefaultStatusMessage SUCCESS_MSG = new DefaultStatusMessage(MessageType.INFO, "");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SaveBackgroundWorker.class);

    private final AbstractSettingsModelFileChooser m_settings;

    SaveBackgroundWorker(final AbstractSettingsModelFileChooser settings) {
        m_settings = settings;
    }

    @Override
    public StatusMessage call() throws Exception {
        try (final WritePathAccessor accessor = m_settings.createPathAccessor()) {
            final PriorityStatusConsumer consumer = new PriorityStatusConsumer();
            final FSPath path = accessor.getOutputPath(consumer);
            if (Files.exists(path)) {
                try {
                    return createAlreadyExistsMsg(path, Files.readAttributes(path, BasicFileAttributes.class));
                } catch (IOException ex) {
                    LOGGER.error("Can't access attributes of existing path", ex);
                    return new DefaultStatusMessage(MessageType.ERROR, "Can't access attributes of %s.", path);
                }
            } else {
                return SUCCESS_MSG;
            }
        }
    }

    private StatusMessage createAlreadyExistsMsg(final Path path, final BasicFileAttributes attrs) {
        switch (m_settings.getFilterModeModel().getFilterMode().getFileSelectionMode()) {
            case DIRECTORIES_ONLY:
                return attrs.isDirectory()
                    ? mkFolderOverwriteWarning(path)
                    : mkError("Output location '%s' is not a folder.", path);
            case FILES_AND_DIRECTORIES:
                return attrs.isDirectory() ? mkFolderOverwriteWarning(path) : mkFileOverwriteWarning(path);
            case FILES_ONLY:
                return !attrs.isRegularFile() ? mkError("Output location '%s' is not a file.", path)
                    : mkFileOverwriteWarning(path);
            default:
                throw new IllegalStateException("Unreachable case.");
        }
    }

    private static StatusMessage mkFileOverwriteWarning(final Path path) {
        return mkWarning("Output file '%s' already exists and might be overwritten", path);
    }

    private static StatusMessage mkFolderOverwriteWarning(final Path path) {
        return mkWarning("Output folder '%s' already exists and might be overwritten", path);
    }

    private static StatusMessage mkError(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.ERROR, format, args);
    }

    private static StatusMessage mkWarning(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.WARNING, format, args);
    }

}