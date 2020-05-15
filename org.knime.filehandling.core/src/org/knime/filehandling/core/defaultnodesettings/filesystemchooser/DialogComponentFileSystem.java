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
 *   Apr 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser;

import java.awt.GridBagLayout;
import java.util.Optional;

import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.ConnectedFileSystemSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.CustomURLSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.MountpointSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.RelativeToSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.ConnectedFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.CustomURLFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.LocalFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.MountpointFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.RelativeToFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusView;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * {@link DialogComponent} for selecting a file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DialogComponentFileSystem extends DialogComponent {

    private final FileSystemChooser m_fileSystemChooser;

    private final StatusView m_statusView = new StatusView(300);

    private final PriorityStatusConsumer m_statusConsumer = new PriorityStatusConsumer();

    private boolean m_replacedByFlowVar = false;

    /**
     * Creates a file system chooser with an optional {@link FlowVariableModelButton} (if {@link FlowVariableModel fvm}
     * is not {@code null}).</br>
     * <b>NOTE:</b> A flow variable button is only added if convenience file systems are used. In the case of a
     * connected file system (provided via an input port), no flow variable button is displayed.
     *
     * @param model the {@link SettingsModelFileSystem} to display
     * @param fvm {@link FlowVariableModel} for the optional {@link FlowVariableModelButton} (may be {@code null} if no
     *            button should be displayed)
     */
    public DialogComponentFileSystem(final SettingsModelFileSystem model, final FlowVariableModel fvm) {
        super(model);
        CheckUtils.checkArgumentNotNull(model, "The model must not be null.");
        FileSystemConfiguration config = model.getFileSystemConfiguration();
        if (config.isConnectedFS()) {
            m_fileSystemChooser = new FileSystemChooser(config, new ConnectedFileSystemDialog(
                (ConnectedFileSystemSpecificConfig)config.getFileSystemSpecifcConfig(Choice.CONNECTED_FS)));
        } else {
            m_fileSystemChooser = createDefaultFileSystemChooser(config);
        }
        model.addChangeListener(e -> updateComponent());
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart();
        if (fvm != null) {
            fvm.addChangeListener(e -> handleFVMChange(fvm));
        }
        if (fvm != null && !config.isConnectedFS()) {
            FlowVariableModelButton fvButton = new FlowVariableModelButton(fvm);
            panel.add(fvButton, gbc.build());
            gbc.incX();
        }
        panel.add(m_fileSystemChooser.getPanel(), gbc.build());
        panel.add(m_statusView.getLabel(), gbc.resetX().incY().widthRemainder().build());
        // add empty panel to eat up all additional space
        panel.add(new JPanel(), gbc.resetX().incY().fillBoth().widthRemainder().setWeightX(1).setWeightY(1).build());
    }

    /**
     * Creates a file system chooser without a flow variable button.</br>
     * Use {@link DialogComponentFileSystem#DialogComponentFileSystem(SettingsModelFileSystem, FlowVariableModel)} if
     * you want to display a flow variable button.
     *
     * @param model the {@link SettingsModelFileSystem} to display
     */
    public DialogComponentFileSystem(final SettingsModelFileSystem model) {
        this(model, null);
    }

    private void handleFVMChange(final FlowVariableModel fvm) {
        m_replacedByFlowVar = fvm.isVariableReplacementEnabled();
        final Optional<FlowVariable> flowVar = fvm.getVariableValue();
        // we enable "by-hand" to avoid unnecessary events from being fired
        m_fileSystemChooser.setEnabled(!m_replacedByFlowVar);
        if (m_replacedByFlowVar && flowVar.isPresent()) {
            final FSLocationSpec fsLocationSpec = flowVar.get().getValue(FSLocationSpecVariableType.INSTANCE);
            getSM().getFileSystemConfiguration().setLocationSpec(fsLocationSpec);
        }
    }

    private static FileSystemChooser createDefaultFileSystemChooser(final FileSystemConfiguration config) {
        final LocalFileSystemDialog local = LocalFileSystemDialog.INSTANCE;
        final MountpointFileSystemDialog mountpoint = new MountpointFileSystemDialog(
            (MountpointSpecificConfig)config.getFileSystemSpecifcConfig(Choice.KNIME_MOUNTPOINT));
        final RelativeToFileSystemDialog relativeTo = new RelativeToFileSystemDialog(
            (RelativeToSpecificConfig)config.getFileSystemSpecifcConfig(Choice.KNIME_FS));
        final CustomURLFileSystemDialog customUrl = new CustomURLFileSystemDialog(
            (CustomURLSpecificConfig)config.getFileSystemSpecifcConfig(Choice.CUSTOM_URL_FS));
        return new FileSystemChooser(config, local, mountpoint, relativeTo, customUrl);
    }

    @Override
    protected void updateComponent() {
        final SettingsModelFileSystem sm = getSM();
        setEnabledComponents(sm.isEnabled());
        m_statusView.clearStatus();
        m_statusConsumer.clear();
        sm.getFileSystemConfiguration().report(m_statusConsumer);
        m_statusConsumer.get().ifPresent(m_statusView::setStatus);
    }

    private SettingsModelFileSystem getSM() {
        return (SettingsModelFileSystem)getModel();
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        getSM().getFileSystemConfiguration().validate();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing is checked here
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_fileSystemChooser.setEnabled(enabled && !m_replacedByFlowVar);
    }

    @Override
    public void setToolTipText(final String text) {
        m_fileSystemChooser.setTooltip(text);
    }

}
