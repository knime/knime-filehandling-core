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
 *   May 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Config specific to the "relative to" file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class RelativeToSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig {

    private static final String CFG_RELATIVE_TO = "relative_to";

    private static final RelativeTo DEFAULT = RelativeTo.WORKFLOW_DATA;

    /** @see #getAllowedValues() */
    private final Set<RelativeTo> m_allowedValues;

    private RelativeTo m_relativeTo = DEFAULT;

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     */
    public RelativeToSpecificConfig(final boolean active) {
        super(active);
        m_allowedValues = Collections.unmodifiableSet(EnumSet.allOf(RelativeTo.class));
    }

    /**
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     * @param defaultValue the pre-selected value of a fresh instance
     * @param allowedValues the values that can be selected by the user
     */
    public RelativeToSpecificConfig(final boolean active, final RelativeTo defaultValue,
        final Set<RelativeTo> allowedValues) {
        super(active);
        m_relativeTo = defaultValue;
        m_allowedValues = Collections.unmodifiableSet(EnumSet.copyOf(allowedValues));
    }

    /**
     * Copy constructor.
     *
     * @param toCopy instance to copy
     */
    private RelativeToSpecificConfig(final RelativeToSpecificConfig toCopy) {
        super(toCopy.isActive());
        m_relativeTo = toCopy.m_relativeTo;
        m_allowedValues = Collections.unmodifiableSet(EnumSet.copyOf(toCopy.m_allowedValues));
    }

    /**
     * Returns the currently selected {@link RelativeTo} option.
     *
     * @return the {@link RelativeTo} option
     */
    public RelativeTo getRelativeTo() {
        return m_relativeTo;
    }

    /**
     * Sets the provided {@link RelativeTo} option and notifies the listeners if the value changed.
     *
     * @param relativeTo to set
     */
    public void setRelativeTo(final RelativeTo relativeTo) {
        CheckUtils.checkArgument(m_allowedValues.contains(relativeTo),
            "Cannot configure this file system configuration to relative to " + relativeTo + ", it only allows "
                + m_allowedValues);
        if (m_relativeTo != relativeTo) {
            m_relativeTo = CheckUtils.checkArgumentNotNull(relativeTo, "The relativeTo argument must not be null.");
            notifyListeners();
        }
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.RELATIVE, m_relativeTo.getSettingsValue());
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setRelativeTo(RelativeTo.fromSettingsValue(settings.getString(CFG_RELATIVE_TO, DEFAULT.getSettingsValue())));
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        readRelativeTo(settings);
    }

    private static RelativeTo readRelativeTo(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            return RelativeTo.fromSettingsValue(settings.getString(CFG_RELATIVE_TO));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Can't load relative to: " + iae.getMessage(), iae);
        }
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        // always valid
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_relativeTo = readRelativeTo(settings);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_RELATIVE_TO, m_relativeTo.getSettingsValue());
    }

    @Override
    public void updateSpecifier(final FSLocationSpec locationSpec) {
        setRelativeTo(
            RelativeTo.fromSettingsValue(locationSpec.getFileSystemSpecifier().orElseThrow(() -> new IllegalArgumentException(
                String.format("The provided FSLocation '%s' does not provide a relative-to option.", locationSpec)))));
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new RelativeToSpecificConfig(this);
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        final Optional<String> specifier = location.getFileSystemSpecifier();
        CheckUtils.checkSetting(specifier.isPresent(),
            "No relative to option specified for the relative to file system.");
        try {
            RelativeTo.fromSettingsValue(specifier.get());
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(
                String.format("Unsupported relative to option '%s' encountered.", specifier.get()), iae);
        }
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

    @Override
    public boolean canConnect() {
        // It's always possible to connect to the relative to file systems
        return true;
    }

    @Override
    public String getFileSystemName() {
        return "Relative to";
    }

    /**
     * @return an unmodifiable set containing the valid {@link RelativeTo} specifiers for this file system
     *         configuration. {@link #setRelativeTo(RelativeTo)} will throw an exception if the value is not in this
     *         set.
     */
    public Set<RelativeTo> getAllowedValues() {
        return m_allowedValues;
    }

}
