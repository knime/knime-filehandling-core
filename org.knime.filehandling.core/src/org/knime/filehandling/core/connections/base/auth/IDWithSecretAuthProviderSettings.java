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
 *   Dec 1, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.base.auth;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * {@link AuthProviderSettings} implementation for authentication schemes that require an ID and secret. The settings
 * keys under which to save the values are customizable. User/password authentication is a special case of this, please
 * see {@link UserPasswordAuthProviderSettings}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public class IDWithSecretAuthProviderSettings implements AuthProviderSettings {

    private static final String KEY_USE_CREDENTIALS = "use_credentials";

    private static final String KEY_CREDENTIALS = "credentials";

    private static final String DEFAULT_KEY_ID = "id";

    private static final String DEFAULT_KEY_SECRET = "secret";

    private static final String SECRET_ENCRYPTION_KEY = "laig9eeyeix:ae$Lo6lu";

    private final AuthType m_authType;

    private final boolean m_allowBlankSecret;

    private final SettingsModelBoolean m_useCredentials;

    private final SettingsModelString m_credentialsName;

    private final SettingsModelString m_id;

    private final SettingsModelPassword m_secret;

    private boolean m_enabled;

    /**
     * Creates a new instance.
     *
     * @param authType
     */
    public IDWithSecretAuthProviderSettings(final AuthType authType) {
        this(authType, false, DEFAULT_KEY_ID, DEFAULT_KEY_SECRET);
    }

    /**
     * Creates a new instance.
     *
     * @param authType
     * @param allowBlankPassword
     * @param settingsKeyID
     * @param settingsKeySecret
     */
    public IDWithSecretAuthProviderSettings(final AuthType authType, final boolean allowBlankPassword,
        final String settingsKeyID, final String settingsKeySecret) {

        m_authType = authType;
        m_allowBlankSecret = allowBlankPassword;

        m_useCredentials = new SettingsModelBoolean(KEY_USE_CREDENTIALS, false);
        m_credentialsName = new SettingsModelString(KEY_CREDENTIALS, "");
        m_id = new SettingsModelString(settingsKeyID, System.getProperty("user.name"));
        m_secret = new SettingsModelPassword(settingsKeySecret, SECRET_ENCRYPTION_KEY, "");

        m_useCredentials.addChangeListener(e -> updateEnabledness());
        m_enabled = true;

        updateEnabledness();
    }

    /**
     * @return true, if a blank secret is considered valid, false otherwise.
     */
    public boolean allowsBlankSecret() {
        return m_allowBlankSecret;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        m_enabled = enabled;
        updateEnabledness();
    }

    @Override
    public boolean isEnabled() {
        return m_enabled;
    }

    private void updateEnabledness() {
        m_useCredentials.setEnabled(m_enabled);
        m_credentialsName.setEnabled(m_enabled && useCredentials());
        m_id.setEnabled(m_enabled && !useCredentials());
        m_secret.setEnabled(m_enabled && !useCredentials());
    }

    @Override
    public AuthType getAuthType() {
        return m_authType;
    }

    /**
     * @return secret settings model.
     */
    public SettingsModelPassword getSecretModel() {
        return m_secret;
    }

    /**
     * @return ID settings model.
     */
    public SettingsModelString getIDModel() {
        return m_id;
    }

    /**
     * @return settings model for whether or not to use a credentials flow variable for ID/secret
     *         authentication.
     */
    public SettingsModelBoolean getUseCredentialsModel() {
        return m_useCredentials;
    }

    /**
     * @return whether or not to use a credentials flow variable for ID/secret authentication.
     */
    public boolean useCredentials() {
        return m_useCredentials.getBooleanValue();
    }

    /**
     * @return settings model for the name of the credentials flow variable for ID/secret authentication.
     */
    public SettingsModelString getCredentialsNameModel() {
        return m_credentialsName;
    }

    /**
     * @return the name of the credentials flow variable for ID/secret authentication (or null, if not set).
     */
    public String getCredentialsName() {
        final var creds = m_credentialsName.getStringValue();
        return StringUtils.isBlank(creds) ? null : creds;
    }

    /**
     * @param cp credentials provider if credentials should be used
     * @return user to use
     */
    public String getID(final Function<String, ICredentials> cp) {
        if (useCredentials() && cp == null) {
            throw new IllegalStateException("Credential provider is not available");
        } else if (useCredentials()) {
            return cp.apply(getCredentialsName()).getLogin();
        } else {
            return m_id.getStringValue();
        }
    }

    /**
     * @param cp credentials provider if credentials should be used
     * @return password to use
     */
    public String getSecret(final Function<String, ICredentials> cp) {
        if (useCredentials() && cp == null) {
            throw new IllegalStateException("Credential provider is not available");
        } else if (useCredentials()) {
            return cp.apply(getCredentialsName()).getPassword();
        } else {
            return m_secret.getStringValue();
        }
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer,
        final CredentialsProvider credentialsProvider) throws InvalidSettingsException {

        if (useCredentials()) {
            if (credentialsProvider == null) {
                throw new InvalidSettingsException("Credentials flow variables not available");
            } else if (!credentialsProvider.listNames().contains(getCredentialsName())) {
                throw new InvalidSettingsException(
                    String.format("Required credentials flow variable '%s' is missing.", getCredentialsName()));
            }
        }
    }

    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            load(settings);
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
    }

    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_useCredentials.loadSettingsFrom(settings);
        m_credentialsName.loadSettingsFrom(settings);
        m_id.loadSettingsFrom(settings);
        m_secret.loadSettingsFrom(settings);

        updateEnabledness();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_useCredentials.validateSettings(settings);
        m_credentialsName.validateSettings(settings);
        m_id.validateSettings(settings);
        m_secret.validateSettings(settings);
    }

    /**
     * Validates the ID. Can be overriden by subclasses to implement custom validation and error messages.
     *
     * @throws InvalidSettingsException If ID is invalid.
     */
    protected void validateID() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_id.getStringValue())) {
            throw new InvalidSettingsException("Please provide valid ID.");
        }
    }

    /**
     * Validates the secret. Can be overriden by subclasses to implement custom validation and error messages.
     *
     * @throws InvalidSettingsException If secret is invalid.
     */
    protected void validateSecret() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_secret.getStringValue()) && !m_allowBlankSecret) {
            throw new InvalidSettingsException("Please provide a valid secret.");
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (useCredentials()) {
            if (StringUtils.isBlank(getCredentialsName())) {
                throw new InvalidSettingsException(String
                    .format("Please choose a credentials flow variable for %s authentication.", m_authType.getText()));
            }
        } else {
            validateID();
            validateSecret();
        }
    }

    @Override
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     */
    private void save(final NodeSettingsWO settings) {
        if (!isEnabled()) {
            // don't save and persist credentials if they are not selected
            // see ticket AP-21749
            clear();
        } else if (m_useCredentials.getBooleanValue()) {
            m_id.setStringValue("");
            m_secret.setStringValue("");
        } else {
            m_credentialsName.setStringValue("");
        }
        m_useCredentials.saveSettingsTo(settings);
        m_credentialsName.saveSettingsTo(settings);
        m_id.saveSettingsTo(settings);
        m_secret.saveSettingsTo(settings);
    }

    @Override
    public void clear() {
        m_credentialsName.setStringValue("");
        m_id.setStringValue("");
        m_secret.setStringValue("");
    }

    @Override
    public String toString() {
        if (useCredentials()) {
            return String.format("%s(credentials=%s)", m_authType.getSettingsKey(), m_credentialsName.getStringValue());
        } else {
            return String.format("%s(user=%s)", m_authType.getSettingsKey(), m_id.getStringValue());
        }
    }

    @Override
    public AuthProviderSettings createClone() {
        final var tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final var clone =
            new IDWithSecretAuthProviderSettings(m_authType, m_allowBlankSecret, m_id.getKey(), m_secret.getKey());
        try {
            clone.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return clone;
    }
}
