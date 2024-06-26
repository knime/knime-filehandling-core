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

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.ValidationUtils;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.FileFilterStatistic;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FilterOptionsSettings;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Allows access to the {@link FSPath FSPaths}. The paths are also validated and respective exceptions are thrown if the
 * settings yield invalid paths.
 *
 * @author Paul Bärnreuther
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public abstract class AbstractFileChooserPathAccessor implements ReadPathAccessor, WritePathAccessor {

    /**
     * Common interface used to construct a {@link AbstractFileChooserPathAccessor}
     *
     * @param location the currently configured {@link FSLocation}
     * @param filterSettings the {@link FilterSettings}, @see
     *            {@link AbstractSettingsModelFileChooser#getFilterModeModel}
     */
    public record FileChooserPathAccessorSettings(FSLocation location, FilterSettings filterSettings) {
    }

    /**
     * Settings defining the filter configuration of the file chooser.
     *
     * @param filterMode the filterMode
     * @param includeSubfolders whether to include sub folders
     * @param filterOptionsSettings the filter options settings
     * @param followLinks true if links should be followed when walking the file tree
     */
    public record FilterSettings(FilterMode filterMode, boolean includeSubfolders,
        FilterOptionsSettings filterOptionsSettings, boolean followLinks) {
    }

    /**
     * The root location i.e. the location to start scanning the file tree from
     */
    private final FSLocation m_rootLocation;

    private final Optional<FSConnection> m_portObjectConnection;

    private final FileChooserPathAccessorSettings m_settings;

    private final FilterMode m_filterMode;

    private FileFilterStatistic m_fileFilterStatistic;

    /**
     * The connection used to create the {@link FSFileSystem} used to create the {@link FSPath paths}.</br>
     * If m_rootLocation isn't empty, this will be initialized with m_rootLocation.get().
     */
    private FSConnection m_connection;

    private FSFileSystem<?> m_fileSystem;

    /**
     * Creates a new FileChooserAccessor for the provided {@link AbstractSettingsModelFileChooser} and
     * {@link FSConnection connection} (if provided).</br>
     * The settings are not validated in this constructor but instead if {@link #getOutputPath(Consumer)} or
     * {@link #getFSPaths(Consumer)} are called.
     *
     * @param settings {@link AbstractSettingsModelFileChooser} provided by the user
     * @param portObjectConnection connection retrieved from the file system port object (if the node has one)
     */
    protected AbstractFileChooserPathAccessor(final FileChooserPathAccessorSettings settings,
        final Optional<FSConnection> portObjectConnection) { //NOSONAR
        m_rootLocation = settings.location();
        m_portObjectConnection = portObjectConnection;
        m_settings = settings;
        m_filterMode = m_settings.filterSettings().filterMode();
    }

    private FSConnection getConnection() {
        if (m_portObjectConnection.isPresent()) {
            // if we have a file system port, we always use the provided connection
            return m_portObjectConnection.get();
        } else {
            // otherwise we retrieve the connection from the location
            return FileSystemHelper.retrieveFSConnection(m_portObjectConnection, m_rootLocation).orElseThrow(
                () -> new IllegalStateException("No file system connection available. Execute connector node."));
        }
    }

    private void initializeFileSystem() {
        if (m_connection == null) {
            m_connection = getConnection();
            m_fileSystem = m_connection.getFileSystem();
        }
    }

    /**
     * Retrieves the output/root {@link FSPath} specified in the settings provided to the constructor.</br>
     * Writer nodes should make use of this method.
     *
     * @param statusMessageConsumer used to communicating non-fatal erros and warnings
     * @return the output path
     * @throws InvalidSettingsException if the settings provided in the constructor are invalid
     */
    @Override
    public FSPath getOutputPath(final Consumer<StatusMessage> statusMessageConsumer) throws InvalidSettingsException {
        CheckUtils.checkArgumentNotNull(statusMessageConsumer, "The statusMessageConsumer must not be null.");

        initializeFileSystem();

        try {
            m_fileSystem.checkCompatibility(m_rootLocation);
        } catch (Exception ex) { // NOSONAR
            statusMessageConsumer.accept(new DefaultStatusMessage(MessageType.ERROR, ex.getMessage()));
        }

        if (m_portObjectConnection.isPresent()) {
            // if present the port object fs always takes precedence
            return m_fileSystem.getPath(m_rootLocation);
        } else {
            return getPathFromConvenienceFs();
        }
    }

    private FSPath getPathFromConvenienceFs() throws InvalidSettingsException {
        switch (m_rootLocation.getFSCategory()) {
            case CONNECTED:
                throw new IllegalStateException("The file system is not connected.");
            case CUSTOM_URL:
                ValidationUtils.validateCustomURLLocation(m_rootLocation);
                return m_fileSystem.getPath(m_rootLocation);
            case RELATIVE:
                final FSPath path = m_fileSystem.getPath(m_rootLocation);
                ValidationUtils.validateKnimeFSPath(path);
                return path;
            case MOUNTPOINT:
                return m_fileSystem.getPath(m_rootLocation);
            case LOCAL:
                ValidationUtils.validateLocalFsAccess();
                return m_fileSystem.getPath(m_rootLocation);
            case HUB_SPACE:
                return m_fileSystem.getPath(m_rootLocation);
            default:
                throw new IllegalStateException("Unsupported file system category: " + m_rootLocation.getFSCategory());
        }
    }

    @Override
    public final List<FSPath> getFSPaths(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException {
        final FSPath rootPath = getRootPath(statusMessageConsumer);

        if (m_filterMode == FilterMode.FILE || m_filterMode == FilterMode.FOLDER
            || m_filterMode == FilterMode.WORKFLOW) {
            return handleSinglePath(rootPath);
        } else {
            List<FSPath> fsPaths = walkFileTree(rootPath);
            FSFiles.sortPathsLexicographically(fsPaths);
            return fsPaths;
        }
    }

    private List<FSPath> handleSinglePath(final FSPath rootPath) throws IOException, InvalidSettingsException {
        final BasicFileAttributes attr = Files.readAttributes(rootPath, BasicFileAttributes.class);
        if (m_filterMode == FilterMode.FILE || m_filterMode == FilterMode.WORKFLOW) {
            String workflowOrFile = m_filterMode.getTextLabel();
            CheckUtils.checkSetting(!rootPath.toString().trim().isEmpty(), "Please specify a " + workflowOrFile + ".");
            CheckUtils.checkSetting(!attr.isDirectory(), "%s is a folder. Please specify a " + workflowOrFile + ".",
                rootPath);
            m_fileFilterStatistic = new FileFilterStatistic(0, 0, 0, 1, 0, 0, 0);
        } else if (m_filterMode == FilterMode.FOLDER) {
            checkIsFolder(rootPath, attr);
            m_fileFilterStatistic = new FileFilterStatistic(0, 0, 0, 0, 0, 0, 1);
        } else {
            throw new IllegalStateException("Unexpected filter mode in handleSingleCase: " + m_filterMode);
        }
        return Collections.singletonList(rootPath);
    }

    @Override
    public FileFilterStatistic getFileFilterStatistic() {
        CheckUtils.checkState(m_fileFilterStatistic != null,
            "No statistic available. Call getFSPaths() or getPaths() first.");
        return m_fileFilterStatistic;
    }

    private List<FSPath> walkFileTree(final Path rootPath) throws IOException, InvalidSettingsException {
        final BasicFileAttributes attrs = Files.readAttributes(rootPath, BasicFileAttributes.class);
        checkIsFolder(rootPath, attrs);
        final FilterVisitor visitor = createVisitor(rootPath);
        final boolean includeSubfolders = m_settings.filterSettings().includeSubfolders();
        final boolean followLinks = m_settings.filterSettings().followLinks();
        final Set<FileVisitOption> linkOptions =
            followLinks ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class);
        Files.walkFileTree(rootPath, linkOptions, includeSubfolders ? Integer.MAX_VALUE : 1, visitor);
        m_fileFilterStatistic = visitor.getFileFilterStatistic();
        final List<?> paths = visitor.getPaths();
        @SuppressWarnings("unchecked") // we know it better
        final List<FSPath> fsPaths = (List<FSPath>)paths;
        return fsPaths;
    }

    private static void checkIsFolder(final Path rootPath, final BasicFileAttributes attrs)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(attrs.isDirectory(), "%s is not a folder. Please specify a folder.", rootPath);
    }

    private FilterVisitor createVisitor(final Path rootPath) {
        final FilterSettings settings = m_settings.filterSettings();
        final boolean includeSubfolders = settings.includeSubfolders();
        final FileAndFolderFilter filter = new FileAndFolderFilter(rootPath, settings.filterOptionsSettings());
        switch (m_filterMode) {
            case FILES_AND_FOLDERS:
                return new FilterVisitor(filter, true, true, includeSubfolders);
            case FILES_IN_FOLDERS:
                return new FilterVisitor(filter, true, false, includeSubfolders);
            case FOLDERS:
                return new FilterVisitor(filter, false, true, includeSubfolders);
            case FOLDER:
            case FILE:
                throw new IllegalStateException(
                    "Encountered file/folder filter mode when walking the file tree. This is a coding error.");
            default:
                throw new IllegalStateException("Unknown filter mode: " + m_filterMode);
        }
    }

    @Override
    public void close() throws IOException {
        if (m_fileSystem != null) {
            m_fileSystem.close();
            m_connection.close();
        }
    }

    @Override
    public FSPath getRootPath(final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException {
        final String errorSuffix = m_filterMode.getTextLabel();
        CheckUtils.checkSetting(!m_rootLocation.getPath().trim().isEmpty(),
            String.format(AbstractSettingsModelFileChooser.NO_LOCATION_ERROR, errorSuffix));
        final FSPath rootPath = getOutputPath(statusMessageConsumer);

        CheckUtils.checkSetting(FSFiles.exists(rootPath), "The specified %s %s does not exist.", errorSuffix, rootPath);
        if (!Files.isReadable(rootPath)) {
            throw ExceptionUtil.createAccessDeniedException(rootPath);
        }
        return rootPath;
    }

}
