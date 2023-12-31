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
package org.knime.filehandling.core.defaultnodesettings.status;

/**
 * Combines a status message with a {@link MessageType type} e.g. error.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface StatusMessage extends Comparable<StatusMessage> {

    /**
     * An instance of type {@link MessageType#INFO} and an empty String as message.
     */
    static final StatusMessage EMPTY = new StatusMessage() {

        @Override
        public MessageType getType() {
            return MessageType.INFO;
        }

        @Override
        public String getMessage() {
            return "";
        }

    };

    /**
     * The type of status message.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum MessageType {
        INFO(0),
        WARNING(1),
        ERROR(2);

        private final int m_priority;

        private MessageType(final int priority) {
            m_priority = priority;
        }

    }

    @Override
    default int compareTo(final StatusMessage o) {
        return -Integer.compare(getType().m_priority, o.getType().m_priority);
    }

    /**
     * Returns the type of this message.
     *
     * @return the type of this message
     */
    MessageType getType();

    /**
     * Returns the actual message.
     *
     * @return the message
     */
    String getMessage();

    /**
     * @param message returned by {@link #getMessage()}
     * @return an instance with {@link MessageType#ERROR}
     */
    static StatusMessage error(final String message) {
        return new StatusMessage() {

            @Override
            public MessageType getType() {
                return MessageType.ERROR;
            }

            @Override
            public String getMessage() {
                return message;
            }

        };
    }

}
