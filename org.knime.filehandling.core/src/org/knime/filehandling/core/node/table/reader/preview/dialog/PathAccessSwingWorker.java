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
 *   Aug 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;

/**
 * A {@link SwingWorkerWithContext} that retrieves the paths of a {@link ReadPathAccessor} asynchronously.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PathAccessSwingWorker extends SwingWorkerWithContext<Pair<Path, List<Path>>, Void> {

    private static final int DELAY = 200;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathAccessSwingWorker.class);

    private final ReadPathAccessor m_pathAccessor;

    private final Consumer<Pair<Path, List<Path>>> m_pathsConsumer;

    private final Consumer<ExecutionException> m_exceptionConsumer;

    PathAccessSwingWorker(final ReadPathAccessor pathAccessor, final Consumer<Pair<Path, List<Path>>> pathsConsumer, final Consumer<ExecutionException> exceptionConsumer) {
        m_pathAccessor = pathAccessor;
        m_pathsConsumer = pathsConsumer;
        m_exceptionConsumer = exceptionConsumer;
    }

    @Override
    protected Pair<Path, List<Path>> doInBackgroundWithContext() throws Exception {
        Thread.sleep(DELAY);
        final Path rootPath = m_pathAccessor.getRootPath(s -> {});
        final List<Path> paths = m_pathAccessor.getPaths(s -> {});
        return new Pair<>(rootPath, paths);
    }

    @Override
    protected void doneWithContext() {
        if (!isCancelled()) {
            try {
                final Pair<Path, List<Path>> rootPathAndPaths = get();
                m_pathsConsumer.accept(rootPathAndPaths);
            } catch (InterruptedException ex) {// NOSONAR
                // shouldn't happen because doneWithContext is only called when doInBackgroundWithContext is done
                // therefore get() doesn't block and we can't be interrupted
                LOGGER.error("InterruptedException encountered even though isCancelled() returned false.", ex);
            } catch (ExecutionException ex) {
                m_exceptionConsumer.accept(ex);
            }
        }
    }

}
