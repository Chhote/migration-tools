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
package com.nuodb.migration.jdbc.metadata.generator;

import com.nuodb.migration.jdbc.dialect.Dialect;
import com.nuodb.migration.jdbc.metadata.Column;
import com.nuodb.migration.jdbc.metadata.ForeignKey;
import com.nuodb.migration.jdbc.metadata.Schema;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collection;
import java.util.Iterator;

import static java.util.Collections.singleton;

/**
 * @author Sergey Bushik
 */
public class ForeignKeyScriptGenerator extends ScriptGeneratorBase<ForeignKey> implements ConstraintScriptGenerator<ForeignKey> {

    public ForeignKeyScriptGenerator() {
        super(ForeignKey.class);
    }

    /**
     * Fix for DB-953 generates:
     * <pre>ALTER TABLE "table1" ADD FOREIGN KEY ("column1") REFERENCES "table2" ("column2");</pre>
     * instead of
     * <pre>ALTER TABLE "table1" ADD CONSTRAINT "fk1" FOREIGN KEY ("column1") REFERENCES "table2" ("column2");</pre>
     *
     * @param foreignKey
     * @param scriptGeneratorContext
     * @return
     */
    @Override
    public Collection<String> getCreateScripts(ForeignKey foreignKey,
                                               ScriptGeneratorContext scriptGeneratorContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ALTER TABLE ");
        buffer.append(scriptGeneratorContext.getQualifiedName(foreignKey.getPrimaryTable()));
        buffer.append(" ADD ");
        buffer.append(getConstraintScript(foreignKey, scriptGeneratorContext));
        return singleton(buffer.toString());
    }

    /**
     * Fix for DB-953 generates
     * <pre>ALTER TABLE "table1" DROP FOREIGN KEY ("column1") REFERENCES "table2";</pre>
     *
     * @param foreignKey
     * @param scriptGeneratorContext
     * @return
     */
    @Override
    public Collection<String> getDropScripts(ForeignKey foreignKey, ScriptGeneratorContext scriptGeneratorContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ALTER TABLE ");
        Dialect dialect = scriptGeneratorContext.getDialect();
        buffer.append(scriptGeneratorContext.getQualifiedName(foreignKey.getPrimaryTable()));
        buffer.append(' ');
        buffer.append(dialect.getDropForeignKey());
        buffer.append(' ');
        for (Iterator<Column> iterator = foreignKey.getSourceColumns().iterator(); iterator.hasNext(); ) {
            Column column = iterator.next();
            buffer.append(scriptGeneratorContext.getName(column));
            if (iterator.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        buffer.append(" REFERENCES ");
        buffer.append(getForeignScriptGeneratorContext(foreignKey, scriptGeneratorContext).getQualifiedName(
                foreignKey.getForeignTable()));
        return singleton(buffer.toString());
    }

    @Override
    public String getConstraintScript(ForeignKey foreignKey, ScriptGeneratorContext scriptGeneratorContext) {
        Dialect dialect = scriptGeneratorContext.getDialect();
        StringBuilder buffer = new StringBuilder();
        buffer.append("FOREIGN KEY (");
        for (Iterator<Column> iterator = foreignKey.getSourceColumns().iterator(); iterator.hasNext(); ) {
            Column column = iterator.next();
            buffer.append(scriptGeneratorContext.getName(column));
            if (iterator.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        buffer.append(" REFERENCES ");
        buffer.append(getForeignScriptGeneratorContext(foreignKey, scriptGeneratorContext).getQualifiedName(
                foreignKey.getForeignTable()));
        buffer.append(" (");
        for (Iterator<Column> iterator = foreignKey.getTargetColumns().iterator(); iterator.hasNext(); ) {
            Column column = iterator.next();
            buffer.append(scriptGeneratorContext.getName(column));
            if (iterator.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        String updateAction = dialect.getUpdateAction(foreignKey.getUpdateAction());
        if (updateAction != null) {
            buffer.append(" ON UPDATE ");
            buffer.append(updateAction);
        }
        String deleteAction = dialect.getDeleteAction(foreignKey.getDeleteAction());
        if (deleteAction != null) {
            buffer.append(" ON DELETE ");
            buffer.append(deleteAction);
        }
        return buffer.toString();
    }

    /**
     * Fixes cross database foreign keys generation https://github.com/nuodb/migration-tools/issues/3
     *
     * @param foreignKey             to create foreign script generator context
     * @param scriptGeneratorContext current script generator context used for the session
     * @return source context if primary and foreign schemas are equals or changes target schema & catalog name to
     *         foreign table schema & catalog.
     */
    protected ScriptGeneratorContext getForeignScriptGeneratorContext(ForeignKey foreignKey,
                                                                      ScriptGeneratorContext scriptGeneratorContext) {
        Schema primarySchema = foreignKey.getPrimaryTable().getSchema();
        Schema foreignSchema = foreignKey.getForeignTable().getSchema();
        if (ObjectUtils.equals(primarySchema, foreignSchema)) {
            return scriptGeneratorContext;
        } else {
            ScriptGeneratorContext foreignScriptGeneratorContext = new ScriptGeneratorContext(scriptGeneratorContext);
            foreignScriptGeneratorContext.setTargetCatalog(foreignSchema.getCatalog().getName());
            foreignScriptGeneratorContext.setTargetSchema(foreignSchema.getName());
            return foreignScriptGeneratorContext;
        }
    }
}