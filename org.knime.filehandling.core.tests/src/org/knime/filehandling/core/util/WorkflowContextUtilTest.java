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
 *   Jul 16, 2021 (hornm): created
 */
package org.knime.filehandling.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Tests methods in {@link WorkflowContextUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class WorkflowContextUtilTest {

    @SuppressWarnings("javadoc")
    @Test
    public void testNoNodeContext() {
        String message =
            assertThrows(IllegalStateException.class, WorkflowContextUtil::getWorkflowContext).getMessage();
        assertThat(message, is("Node context required."));
        assertTrue(WorkflowContextUtil.getWorkflowContextOptional().isEmpty());
        assertFalse(WorkflowContextUtil.hasWorkflowContext());
        assertThrows(IllegalStateException.class, WorkflowContextUtil::isServerContext);
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testNoWorkflowManager() {
        NodeContext.pushContext(new Object());
        try {
            String message =
                assertThrows(IllegalStateException.class, WorkflowContextUtil::getWorkflowContext).getMessage();
            assertThat(message, is("Can't access workflow instance (is it a remotely edited workflow?)."));
            assertTrue(WorkflowContextUtil.getWorkflowContextOptional().isEmpty());
            assertFalse(WorkflowContextUtil.hasWorkflowContext());
            assertThrows(IllegalStateException.class, WorkflowContextUtil::isServerContext);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testNoWorkfowContext() {
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("blub", new WorkflowCreationHelper());
        NodeContext.pushContext(wfm);
        try {
            String message =
                assertThrows(IllegalStateException.class, WorkflowContextUtil::getWorkflowContext).getMessage();
            assertThat(message, is("Workflow context required."));
            assertTrue(WorkflowContextUtil.getWorkflowContextOptional().isEmpty());
            assertFalse(WorkflowContextUtil.hasWorkflowContext());
            assertThrows(IllegalStateException.class, WorkflowContextUtil::isServerContext);
        } finally {
            NodeContext.removeLastContext();
            WorkflowManager.ROOT.removeProject(wfm.getID());
        }
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testGetWorkflowContextSuccess() {
        WorkflowCreationHelper helper = new WorkflowCreationHelper();
        WorkflowContext ctx = new WorkflowContext.Factory(new File("tmp")).createContext();
        helper.setWorkflowContext(ctx);
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("blub", helper);
        NodeContext.pushContext(wfm);
        try {
            assertNotNull(WorkflowContextUtil.getWorkflowContext());
            assertTrue(WorkflowContextUtil.getWorkflowContextOptional().isPresent());
            assertTrue(WorkflowContextUtil.hasWorkflowContext());
            assertFalse(WorkflowContextUtil.isServerContext());
        } finally {
            NodeContext.removeLastContext();
            WorkflowManager.ROOT.removeProject(wfm.getID());
        }

    }

}
