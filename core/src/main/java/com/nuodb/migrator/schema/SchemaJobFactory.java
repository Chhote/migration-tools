/**
 * Copyright (c) 2012, NuoDB, Inc.
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
package com.nuodb.migrator.schema;

import com.nuodb.migrator.jdbc.connection.ConnectionProvider;
import com.nuodb.migrator.jdbc.connection.ConnectionProviderFactory;
import com.nuodb.migrator.jdbc.connection.DriverConnectionSpecProviderFactory;
import com.nuodb.migrator.jdbc.connection.QueryLoggingConnectionProviderFactory;
import com.nuodb.migrator.jdbc.dialect.DialectResolver;
import com.nuodb.migrator.jdbc.dialect.NuoDBDialect;
import com.nuodb.migrator.jdbc.dialect.SimpleDialectResolver;
import com.nuodb.migrator.jdbc.metadata.generator.*;
import com.nuodb.migrator.jdbc.type.JdbcTypeNameMap;
import com.nuodb.migrator.job.JobFactory;
import com.nuodb.migrator.spec.ConnectionSpec;
import com.nuodb.migrator.spec.JdbcTypeSpec;
import com.nuodb.migrator.spec.ResourceSpec;
import com.nuodb.migrator.spec.SchemaSpec;

import java.sql.SQLException;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static com.nuodb.migrator.jdbc.metadata.generator.HasTablesScriptGenerator.GROUP_SCRIPTS_BY;
import static com.nuodb.migrator.jdbc.metadata.generator.WriterScriptExporter.SYSTEM_OUT_SCRIPT_EXPORTER;
import static com.nuodb.migrator.jdbc.type.JdbcTypeSpecifiers.newSpecifiers;
import static com.nuodb.migrator.utils.ValidationUtils.isNotNull;

/**
 * @author Sergey Bushik
 */
public class SchemaJobFactory implements JobFactory<SchemaJob> {

    public static final boolean FAIL_ON_EMPTY_SCRIPTS = true;

    private SchemaSpec schemaSpec;
    private boolean failOnEmptyScripts = FAIL_ON_EMPTY_SCRIPTS;
    private DialectResolver dialectResolver = new SimpleDialectResolver();
    private ConnectionProviderFactory connectionProviderFactory = new QueryLoggingConnectionProviderFactory(
            new DriverConnectionSpecProviderFactory());

    @Override
    public SchemaJob createJob() {
        isNotNull(schemaSpec, "Schema spec is required");

        SchemaSpec schemaSpec = getSchemaSpec();
        SchemaJob schemaJob = new SchemaJob();
        schemaJob.setTableTypes(schemaSpec.getTableTypes());
        schemaJob.setConnectionProvider(createConnectionProvider(schemaSpec.getSourceConnectionSpec()));
        schemaJob.setDialectResolver(getDialectResolver());
        schemaJob.setFailOnEmptyScripts(isFailOnEmptyScripts());
        schemaJob.setScriptGeneratorContext(createScriptGeneratorContext());
        schemaJob.setScriptExporter(createScriptExporter());
        return schemaJob;
    }

    protected ConnectionProvider createConnectionProvider(ConnectionSpec connectionSpec) {
        return getConnectionProviderFactory().createConnectionProvider(connectionSpec);
    }

    protected ScriptGeneratorContext createScriptGeneratorContext() {
        SchemaSpec schemaSpec = getSchemaSpec();
        ScriptGeneratorContext scriptGeneratorContext = new ScriptGeneratorContext();
        scriptGeneratorContext.getAttributes().put(GROUP_SCRIPTS_BY, schemaSpec.getGroupScriptsBy());
        scriptGeneratorContext.setObjectTypes(schemaSpec.getMetaDataTypes());
        scriptGeneratorContext.setScriptTypes(schemaSpec.getScriptTypes());

        NuoDBDialect dialect = new NuoDBDialect();
        JdbcTypeNameMap jdbcTypeNameMap = dialect.getJdbcTypeNameMap();
        for (JdbcTypeSpec jdbcTypeSpec : schemaSpec.getJdbcTypeSpecs()) {
            jdbcTypeNameMap.addJdbcTypeName(
                    jdbcTypeSpec.getTypeCode(), jdbcTypeSpec.getTypeName(),
                    newSpecifiers(
                            jdbcTypeSpec.getSize(), jdbcTypeSpec.getPrecision(), jdbcTypeSpec.getScale()));
        }
        dialect.setIdentifierQuoting(schemaSpec.getIdentifierQuoting());
        dialect.setIdentifierNormalizer(schemaSpec.getIdentifierNormalizer());
        scriptGeneratorContext.setDialect(dialect);

        ConnectionSpec sourceConnectionSpec = schemaSpec.getSourceConnectionSpec();
        scriptGeneratorContext.setSourceCatalog(sourceConnectionSpec.getCatalog());
        scriptGeneratorContext.setSourceSchema(sourceConnectionSpec.getSchema());

        ConnectionSpec targetConnectionSpec = schemaSpec.getTargetConnectionSpec();
        if (targetConnectionSpec != null) {
            scriptGeneratorContext.setTargetCatalog(targetConnectionSpec.getCatalog());
            scriptGeneratorContext.setTargetSchema(targetConnectionSpec.getSchema());
        }
        return scriptGeneratorContext;
    }

    protected ScriptExporter createScriptExporter() {
        Collection<ScriptExporter> exporters = newArrayList();
        ConnectionSpec targetConnectionSpec = schemaSpec.getTargetConnectionSpec();
        if (targetConnectionSpec != null) {
            try {
                ConnectionProvider connectionProvider = createConnectionProvider(targetConnectionSpec);
                exporters.add(new ConnectionScriptExporter(connectionProvider.getConnectionServices()));
            } catch (SQLException exception) {
                throw new SchemaJobException("Failed creating connection script exporter", exception);
            }
        }
        ResourceSpec outputSpec = schemaSpec.getOutputSpec();
        if (outputSpec != null) {
            exporters.add(new FileScriptExporter(outputSpec.getPath()));
        }
        // Fallback to the standard output if neither target connection nor target file were specified
        if (exporters.isEmpty()) {
            exporters.add(SYSTEM_OUT_SCRIPT_EXPORTER);
        }
        return new CompositeScriptExporter(exporters);
    }

    public SchemaSpec getSchemaSpec() {
        return schemaSpec;
    }

    public void setSchemaSpec(SchemaSpec schemaSpec) {
        this.schemaSpec = schemaSpec;
    }

    public boolean isFailOnEmptyScripts() {
        return failOnEmptyScripts;
    }

    public void setFailOnEmptyScripts(boolean failOnEmptyScripts) {
        this.failOnEmptyScripts = failOnEmptyScripts;
    }

    public DialectResolver getDialectResolver() {
        return dialectResolver;
    }

    public void setDialectResolver(DialectResolver dialectResolver) {
        this.dialectResolver = dialectResolver;
    }

    public ConnectionProviderFactory getConnectionProviderFactory() {
        return connectionProviderFactory;
    }

    public void setConnectionProviderFactory(ConnectionProviderFactory connectionProviderFactory) {
        this.connectionProviderFactory = connectionProviderFactory;
    }
}