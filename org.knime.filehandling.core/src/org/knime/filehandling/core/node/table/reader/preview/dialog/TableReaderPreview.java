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
 *   May 9, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.table.TableColumn;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableRowHeaderView;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.defaultnodesettings.filechooser.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.preview.PreviewDataTable;
import org.knime.filehandling.core.node.table.reader.preview.PreviewExecutionMonitor;

/**
 * This panel can be embedded by node dialogs for table reader that want to load and display a preview of the table that
 * should be read. Note that {@link #onClose()} should be called when closing the dialog.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of the {@link ReaderSpecificConfig}
 * @param <V> the type of tokens the reader produces
 */
public final class TableReaderPreview<C extends ReaderSpecificConfig<C>, V> extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color COLOR_ERROR = Color.RED;

    private static final String EMPTY_SPACE_RESERVING_LABEL_TEXT = " ";

    private static final int DELAY_ANALYSIS = 1000;

    private static final Consumer<StatusMessage> NO_OP_CONSUMER = w -> {
    };

    private final JButton m_quickScanButton = new JButton("Quick Scan");

    private final TableView m_previewTableView = new TableView(new TableContentView());

    private JLabel m_errorLabel = new JLabel(EMPTY_SPACE_RESERVING_LABEL_TEXT);

    private JLabel m_analysisProgressLabel = new JLabel(EMPTY_SPACE_RESERVING_LABEL_TEXT);

    private JLabel m_analysisProgressPathLabel = new JLabel(EMPTY_SPACE_RESERVING_LABEL_TEXT);

    private JProgressBar m_analysisProgressBar = new JProgressBar();

    private boolean m_analyzing = false;

    private final transient MultiTableReader<C, ?, V> m_multiTableReader;

    private final transient Supplier<MultiTableReadConfig<C>> m_configSupplier;

    private final transient PathSettings m_pathSettings;

    private transient PreviewDataTable<C, V> m_previewTable = null;

    private transient AnalyzeSwingWorker m_analyzeThread;

    private transient PreviewExecutionMonitor m_execMonitor;

    private ReadPathAccessor m_accessor;

    /**
     * Constructor.
     *
     * @param multiTableReader the reader used to read in the table
     * @param pathSettings the path settings
     * @param configSupplier a {@link Supplier} that returns the {@link MultiTableReadConfig} used for reading
     */
    public TableReaderPreview(final MultiTableReader<C, ?, V> multiTableReader, final PathSettings pathSettings,
        final Supplier<MultiTableReadConfig<C>> configSupplier) {
        m_pathSettings = CheckUtils.checkArgumentNotNull(pathSettings, "The path settings must not be null.");
        m_configSupplier = CheckUtils.checkArgumentNotNull(configSupplier, "The config supplier must not be null.");
        m_multiTableReader =
            CheckUtils.checkArgumentNotNull(multiTableReader, "The multi table reader must not be null.");

        createPanel();
        setVisibleAnalysisComponents(false);

        m_quickScanButton.addActionListener(l -> {
            m_quickScanButton.setEnabled(false);
            m_execMonitor.getProgressMonitor().setExecuteCanceled();
        });
    }

    private void createPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Preview"));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 1, 1);
        gbc.ipadx = 50;
        add(m_analysisProgressBar, gbc);
        gbc.ipadx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(0, 5, 1, 5);
        add(m_quickScanButton, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 5, 1, 1);
        add(m_analysisProgressLabel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 4;
        gbc.insets = new Insets(5, 5, 5, 5);
        m_errorLabel.setForeground(COLOR_ERROR);
        add(m_errorLabel, gbc);
        add(m_analysisProgressPathLabel, gbc);
        gbc.insets = new Insets(1, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1;
        add(m_previewTableView, gbc);
    }

    private void refresh() {
        // cancel the thread if still running
        if (m_analyzeThread != null) {
            m_analyzeThread.cancel(true);
        }
        m_previewTableView.setDataTable(null);
        resetPreviewError();
        resetAnalysisComponents();
        if (m_execMonitor != null) {
            m_execMonitor.removeAllChangeListeners();
        }
        m_execMonitor = new PreviewExecutionMonitor();

        // add listener to the execution monitor that updates the progress during spec guessing
        m_execMonitor.getProgressMonitor().addProgressListener(this::updateSpecGuessingProgress);

        closeReadPathAccessor();
        m_accessor = m_pathSettings.createReadPathAccessor();

        // start the spec guessing in a separated thread, exceptions are swallowed and put into the error label.
        // afterwards, (in the same thread) the preview table view is filled with the created data table
        m_analyzeThread = new AnalyzeSwingWorker(m_accessor);
        m_analyzeThread.execute();
    }

    private synchronized void closeReadPathAccessor() {
        if (m_accessor != null) {
            new DeleteReadPathAccessorSwingWorker(m_accessor).execute();
            m_accessor = null;
        }
    }

    private void updateSpecGuessingProgress(final NodeProgressEvent pEvent) {
        if (m_analyzing && pEvent.getNodeProgress().getMessage() != null) {
            // set the current read path
            final Optional<Path> currentPath = m_execMonitor.getCurrentPath();
            m_analysisProgressPathLabel.setText("Reading file " + m_execMonitor.getCurrentlyReadingPathIdx() + " of "
                + m_execMonitor.getNumPathsToRead()
                + (currentPath.isPresent() ? (": " + currentPath.get().toString()) : ""));
            // set the progress bar
            if (!m_execMonitor.isSizeAssessable()) {
                m_analysisProgressBar.setIndeterminate(true);
            } else {
                m_analysisProgressBar.setIndeterminate(false);
                final Double progress = pEvent.getNodeProgress().getProgress();
                m_analysisProgressBar.setValue(progress == null ? 0 : (int)Math.round(100 * progress.doubleValue()));
            }
            // set the progress label containing additional info as the number of analyzed rows
            m_analysisProgressLabel.setText(createAnalysisProgressText(pEvent.getNodeProgress().getMessage()));
        }
    }

    /**
     * Cancels potentially running threads and disposes of the allocated resources.
     */
    public void onClose() {
        // the loaded preview is disposed in order to prevent a corrupt state when closing during the analysis
        if (m_analyzeThread != null) {
            m_analyzeThread.cancel(true);
            m_analyzeThread = null;
        }
        if (m_previewTable != null) {
            m_previewTable.getExecutionMonitor().removeAllChangeListeners();
            m_previewTableView.setDataTable(null);
            m_previewTable.dispose();
            m_previewTable = null;
        }
        resetAnalysisComponents();
        resetPreviewError();
        closeReadPathAccessor();
    }

    private static String createAnalysisProgressText(final String progress) {
        return "Detecting column types... " + (progress == null ? "" : "(" + progress + ")");
    }

    private void resetAnalysisComponents() {
        m_analysisProgressBar.setValue(0);
        m_analysisProgressLabel.setIcon(null);
        m_analysisProgressLabel.setText(EMPTY_SPACE_RESERVING_LABEL_TEXT);
        m_quickScanButton.setEnabled(true);
    }

    private void setVisibleAnalysisComponents(final boolean visible) {
        m_analysisProgressBar.setVisible(visible);
        m_quickScanButton.setVisible(visible);
    }

    private void resetPreviewError() {
        m_errorLabel.setIcon(null);
        m_errorLabel.setText(EMPTY_SPACE_RESERVING_LABEL_TEXT);
    }

    /**
     * Inform the preview panel that the config has changed. It will display a hint that the preview needs to be
     * refreshed.
     *
     * @param accessor the {@link ReadPathAccessor} providing the files to shown by the preview
     */
    public synchronized void configChanged() {
        if (isEnabled()) {
            refresh();
        }
    }

    private class AnalyzeSwingWorker extends SwingWorkerWithContext<Void, Void> {

        private static final String IO_ERROR = "An I/O error occurred. Select a valid file or folder.";

        private final ReadPathAccessor m_readPathAccessor;

        PreviewDataTable<C, V> m_table = null;

        private boolean m_errorOccurred = false;

        private boolean m_limitRowsForSpec;

        private long m_maxRowsForSpec;

        AnalyzeSwingWorker(final ReadPathAccessor accessor) {
            m_readPathAccessor = accessor;
        }

        @Override
        protected Void doInBackgroundWithContext() throws Exception {
            // wait a bit, the thread may be interrupted immediately when several config changes are made
            // we don't want to open unnecessary connections
            Thread.sleep(DELAY_ANALYSIS);
            m_analyzing = true;
            setVisibleAnalysisComponents(true);
            final List<Path> paths = m_readPathAccessor.getPaths(NO_OP_CONSUMER);
            m_execMonitor.setNumPathsToRead(paths.size());
            final MultiTableReadConfig<C> config = m_configSupplier.get();
            m_limitRowsForSpec = config.getTableReadConfig().limitRowsForSpec();
            m_maxRowsForSpec = config.getTableReadConfig().getMaxRowsForSpec();
            m_table = m_multiTableReader.createPreviewDataTable(
                m_readPathAccessor.getRootPath(NO_OP_CONSUMER).toString(), paths, config, m_execMonitor);
            return null;
        }

        @Override
        protected void doneWithContext() {
            boolean isInterrupted = checkForExceptions();
            m_analyzing = false;
            m_analysisProgressPathLabel.setText("");
            if (isInterrupted || isCancelled()) { // may happen if the user closes the dialog during the analysis
                resetPreviewError();
                if (m_table != null) {
                    m_table.getExecutionMonitor().removeAllChangeListeners();
                    m_table.dispose();
                }
            } else {
                final boolean specGuessingErrorOccurred = m_execMonitor.isSpecGuessingErrorOccurred();
                if (specGuessingErrorOccurred) {
                    setError(m_execMonitor.getSpecGuessingErrorRow(), m_execMonitor.getSpecGuessingErrorMsg());
                }
                setDataTable(m_table);
                try {
                    m_execMonitor.checkCanceled();
                    setAnalysisStatus(specGuessingErrorOccurred);
                    m_analysisProgressPathLabel.setText("");
                } catch (CanceledExecutionException ex) {
                    m_analysisProgressLabel.setIcon(SharedIcons.WARNING_YELLOW.get());
                    m_analysisProgressLabel
                        .setText("The suggested column types are based on a partial file analysis only!");
                }
            }
            setVisibleAnalysisComponents(false);
        }

        private void setAnalysisStatus(final boolean specGuessingErrorOccurred) {
            if (!specGuessingErrorOccurred && !m_errorOccurred) {
                if (m_limitRowsForSpec) {
                    m_analysisProgressLabel.setIcon(SharedIcons.INFO.get());
                    m_analysisProgressLabel.setText("The suggested column types are based on the first "
                        + m_maxRowsForSpec + " rows only. See 'Limit Rows' tab.");
                } else {
                    m_analysisProgressLabel.setIcon(SharedIcons.SUCCESS.get());
                    m_analysisProgressLabel.setText("File analysis successfully completed.");
                }
            } else {
                m_analysisProgressLabel.setIcon(null);
                m_analysisProgressLabel.setText(EMPTY_SPACE_RESERVING_LABEL_TEXT);
            }
        }

        private boolean checkForExceptions() {
            try {
                get();
            } catch (CancellationException | InterruptedException e) {
                return true;
            } catch (ExecutionException e) {
                m_errorOccurred = true;
                final Throwable cause = e.getCause();
                if (cause != null) {
                    // check for the exceptions that are thrown by PathSettings#getPaths
                    // and for IOExceptions during execution
                    if (cause instanceof IOException || cause instanceof InvalidSettingsException
                        || cause.getCause() instanceof IOException) {
                        setError(-1, IO_ERROR);
                    } else {
                        setError(-1, cause.getMessage());
                    }
                } else {
                    setError(-1, e.getMessage());
                }
            }
            return false;
        }

        private void setDataTable(final PreviewDataTable<C, V> previewTable) {
            final PreviewDataTable<C, V> oldTable = m_previewTable;
            m_previewTable = previewTable;

            // register a listener for error messages
            // (because of lazy loading an error does not occur until the user scrolls down to the respective row)
            if (previewTable != null) {
                final PreviewExecutionMonitor execMonitor = previewTable.getExecutionMonitor();
                if (execMonitor != null) {
                    execMonitor.addChangeListener(e -> {
                        if (execMonitor.isIteratorErrorOccurred()) {
                            setError(execMonitor.getIteratorErrorRow(), execMonitor.getIteratorErrorMsg());
                        }
                    });
                }
            }

            ViewUtils.invokeLaterInEDT(() -> {
                // set the new table in the view
                m_previewTableView.setDataTable(previewTable);
                if (previewTable != null) {
                    final TableColumn column = m_previewTableView.getHeaderTable().getColumnModel().getColumn(0);
                    // TODO The following is copied from the preview in the File Reader. The ticket numbers don't make
                    // sense so this fix might be very, very old and a new Java version might have fixed it? we should
                    // check if it is still required:

                    // bug fix 4418 and 4903 -- the row header column does not have a good width on windows.
                    // (due to some SWT_AWT bridging)
                    ViewUtils.invokeLaterInEDT(() -> {
                        final int width = 75;
                        column.setMinWidth(75);
                        TableRowHeaderView headerTable = m_previewTableView.getHeaderTable();
                        Dimension newSize = new Dimension(width, 0);
                        headerTable.setPreferredScrollableViewportSize(newSize);
                    });
                }
                // properly dispose of the old table
                if (oldTable != null) {
                    oldTable.getExecutionMonitor().removeAllChangeListeners();
                    oldTable.dispose();
                }
            });
        }

        private void setError(final long row, final String text) {
            // only set error if not another one is already set
            if (m_errorLabel.getText().equals(EMPTY_SPACE_RESERVING_LABEL_TEXT)) {
                m_errorLabel.setIcon(SharedIcons.ERROR.get());
                m_errorLabel.setText((row < 0 ? "" : "Row " + row + ": ")
                    + (text == null || text.isEmpty() ? EMPTY_SPACE_RESERVING_LABEL_TEXT : text));
            }
        }
    }

    private static class DeleteReadPathAccessorSwingWorker extends SwingWorkerWithContext<Void, Void> {

        private final ReadPathAccessor m_readPathAccessor;

        DeleteReadPathAccessorSwingWorker(final ReadPathAccessor accessor) {
            m_readPathAccessor = accessor;
        }

        @Override
        protected Void doInBackgroundWithContext() throws Exception {
            m_readPathAccessor.close();
            return null;
        }
    }
}
