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
 *   2021-08-04 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.fs.knime.mountpoint.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.MountpointFileSystemDialog;
import org.knime.filehandling.core.fs.knime.mountpoint.node.MountpointConnectorNodeSettings.MountpointMode;

/**
 * Mountpoint Connector node dialog
 *
 * @author Alexander Bondaletov
 */
public class MountpointConnectorNodeDialog extends NodeDialogPane {

    private final MountpointConnectorNodeSettings m_settings;

    private final WorkingDirectoryChooser m_workingDirChooser;

    private final MountpointFileSystemDialog m_mountpointSelector;

    private final JRadioButton m_radioCurrentMountpoint;

    private final JRadioButton m_radioOtherMountpoint;

    private boolean m_ignoreChangeEvents;

    private DefaultWorkingDirectoryFetcher m_defaultWorkingDirFetcher = null;

    /**
     * Creates new instance.
     */
    public MountpointConnectorNodeDialog() {
        m_settings = new MountpointConnectorNodeSettings();

        m_settings.getMountpointModeModel().addChangeListener(e -> updateComponentsEnabled());
        m_settings.getWorkflowRelativeWorkingDirModel().addChangeListener(e -> updateComponentsEnabled());

        var group = new ButtonGroup();
        m_radioCurrentMountpoint = createRadioButton(group, MountpointMode.CURRENT);
        m_radioOtherMountpoint = createRadioButton(group, MountpointMode.OTHER);

        m_workingDirChooser = new WorkingDirectoryChooser("mountpoint.workingDir", this::createFSConnectionForWorkingDirChooser);
        m_workingDirChooser.addListener(e -> {
            if (!m_ignoreChangeEvents) {
                final var workDir = m_workingDirChooser.getSelectedWorkingDirectory();
                m_settings.getWorkingDirectoryModel().setStringValue(workDir);
            }
        });

        m_mountpointSelector = new MountpointFileSystemDialog(m_settings.getMountpoint());


        m_settings.getMountpoint().addChangeListener(this::onMountpointChanged);
        m_settings.getMountpointModeModel().addChangeListener(this::onMountpointChanged);
        m_ignoreChangeEvents = false;

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private void onMountpointChanged(@SuppressWarnings("unused") final ChangeEvent e) {
        if (m_ignoreChangeEvents) {
            return;
        }

        if (m_defaultWorkingDirFetcher != null) {
            m_defaultWorkingDirFetcher.cancel(true);
            m_defaultWorkingDirFetcher = null;
        }

        m_defaultWorkingDirFetcher = new DefaultWorkingDirectoryFetcher();
        m_defaultWorkingDirFetcher.execute();
    }

    private JRadioButton createRadioButton(final ButtonGroup group, final MountpointMode mode) {
        var rb = new JRadioButton(mode.getLabel());
        rb.addActionListener(e -> m_settings.setMountpointMode(mode));
        group.add(rb);
        return rb;
    }

    private FSConnection createFSConnectionForWorkingDirChooser() {
        return MountpointConnectorFSConnectionFactory.create(m_settings).createFSConnectionForWorkingDirectoryChooser();
    }

    private JComponent createSettingsPanel() {
        var box = new Box(BoxLayout.Y_AXIS);
        box.add(createMountpointSelectionPanel());
        m_workingDirChooser.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5),
            BorderFactory.createTitledBorder("Working directory")));
        box.add(m_workingDirChooser);
        return box;
    }

    private JComponent createMountpointSelectionPanel() {
        var currentWorkflowAsWorkdir = new DialogComponentBoolean(m_settings.getWorkflowRelativeWorkingDirModel(),
            "Set working directory to the current workflow");

        var panel = new JPanel(new GridBagLayout());
        var outerInsets = new Insets(0, 10, 0, 0);
        var innerInsets = new Insets(0, 30, 0, 0);

        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = outerInsets;
        panel.add(m_radioCurrentMountpoint, c);

        c.gridy += 1;
        c.insets = innerInsets;
        panel.add(currentWorkflowAsWorkdir.getComponentPanel(), c);

        c.gridy += 1;
        c.insets = outerInsets;
        panel.add(m_radioOtherMountpoint, c);

        c.gridy += 1;
        c.insets = innerInsets;
        panel.add(createMountpointSelector(), c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5),
            BorderFactory.createTitledBorder("Mountpoint selection")));
        return panel;
    }

    private JComponent createMountpointSelector() {
        var panel = new JPanel();
        panel.add(new JLabel("Mountpoint: "));
        panel.add(m_mountpointSelector.getSpecifierComponent());
        return panel;
    }

    private JComponent createAdvancedPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 10, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createAdvancedConnectionSettingsPanel(), gbc);

        gbc.gridy++;

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private Component createAdvancedConnectionSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Connection settings"));

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;

        panel.add(new JLabel("Connection timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;

        panel.add(new JLabel("Read timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_workingDirChooser.addCurrentSelectionToHistory();
        m_settings.validate();
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        try {
            m_ignoreChangeEvents = true;
            m_settings.loadInDialog(settings, specs);
            settingsLoaded();
        } finally {
            m_ignoreChangeEvents = false;
        }

    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        updateComponentsEnabled();
    }

    private void updateComponentsEnabled() {
        var mode = m_settings.getMountpointMode();

        m_radioCurrentMountpoint.setSelected(mode == MountpointMode.CURRENT);
        m_radioOtherMountpoint.setSelected(mode == MountpointMode.OTHER);

        m_settings.getWorkflowRelativeWorkingDirModel().setEnabled(mode == MountpointMode.CURRENT);
        m_mountpointSelector.setEnabled(mode == MountpointMode.OTHER);

        var workdirDisabled = mode == MountpointMode.CURRENT && m_settings.getWorkflowRelativeWorkingDirModel().getBooleanValue();
        m_workingDirChooser.setEnabled(!workdirDisabled);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }

    private class DefaultWorkingDirectoryFetcher extends SwingWorkerWithContext<String, Void> {

        @Override
        @SuppressWarnings("resource")
        protected String doInBackgroundWithContext() throws Exception {
            try (var connection = createFSConnectionForWorkingDirChooser()) {
                return connection.getFileSystem().getDefaultWorkingDirectory().toAbsolutePath().normalize().toString();
            }
        }

        @Override
        protected void doneWithContext() {
            var defaultWorkingDir = "/";

            try {
                defaultWorkingDir = get();
            } catch (Exception e) {//NOSONAR ignore
                e.printStackTrace();
            }

            m_settings.getWorkingDirectoryModel().setStringValue(defaultWorkingDir);
            m_workingDirChooser.setSelectedWorkingDirectory(defaultWorkingDir);
        }
    }
}
