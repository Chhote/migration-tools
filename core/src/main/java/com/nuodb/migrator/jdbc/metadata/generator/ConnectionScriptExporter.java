/**
 * Copyright (c) 2014, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migrator.jdbc.metadata.generator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import com.nuodb.migrator.utils.StringUtils;

import static com.nuodb.migrator.jdbc.JdbcUtils.closeQuietly;
import static java.lang.String.format;

/**
 * @author Sergey Bushik
 */
public class ConnectionScriptExporter extends ScriptExporterBase {

    protected Statement statement;
    protected Connection connection;

    public ConnectionScriptExporter(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected void doOpen() throws Exception {
        statement = getConnection().createStatement();
    }

    @Override
    public void doExportScript(String script) throws Exception {
        if (statement == null) {
            throw new GeneratorException("Connection is not opened");
        }
        if (!StringUtils.isEmpty(script)) {
            statement.executeUpdate(script);
        }
        processWarnings(statement.getWarnings());
    }

    protected void processWarnings(SQLWarning warning) throws SQLException {
        while (warning != null) {
            if (logger.isWarnEnabled()) {
                logger.warn(format("Warning code: %d, state: %s", warning.getErrorCode(), warning.getSQLState()));
            }
            warning = warning.getNextWarning();
        }
        connection.clearWarnings();
    }

    @Override
    protected void doClose() throws Exception {
        closeQuietly(statement);
        closeQuietly(connection);
    }

    public Connection getConnection() {
        return connection;
    }
}
