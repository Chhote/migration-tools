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
package com.nuodb.migration.jdbc.query;

import com.google.common.collect.Maps;
import com.nuodb.migration.jdbc.dialect.DatabaseDialect;
import com.nuodb.migration.jdbc.metamodel.Column;
import com.nuodb.migration.jdbc.metamodel.Table;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Sergey Bushik
 */
public class InsertQuery implements Query {

    private DatabaseDialect databaseDialect;
    private Table table;
    private boolean qualifyNames;
    private Map<Column, String> columns = Maps.newLinkedHashMap();

    public DatabaseDialect getDatabaseDialect() {
        return databaseDialect;
    }

    public void setDatabaseDialect(DatabaseDialect databaseDialect) {
        this.databaseDialect = databaseDialect;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public boolean isQualifyNames() {
        return qualifyNames;
    }

    public void setQualifyNames(boolean qualifyNames) {
        this.qualifyNames = qualifyNames;
    }

    public Map<Column, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<Column, String> columns) {
        this.columns = columns;
    }

    public void addColumn(Column column) {
        addColumn(column, "?");
    }

    public void addColumn(Column column, String value) {
        columns.put(column, value);
    }

    @Override
    public String toQuery() {
        StringBuilder query = new StringBuilder();
        query.append("insert into ")
                .append(qualifyNames ? table.getQualifiedName(databaseDialect) : table.getQuotedName(databaseDialect));
        if (columns.size() == 0) {
            query.append(' ').append(databaseDialect.getNoColumnsInsertString());
        } else {
            query.append(" (");
            Iterator<Column> names = columns.keySet().iterator();
            while (names.hasNext()) {
                Column column = names.next();
                query.append(column.getQuotedName(databaseDialect));
                if (names.hasNext()) {
                    query.append(", ");
                }
            }
            query.append(") values (");
            Iterator<String> values = columns.values().iterator();
            while (values.hasNext()) {
                query.append(values.next());
                if (values.hasNext()) {
                    query.append(", ");
                }
            }
            query.append(')');
        }
        return query.toString();
    }

    @Override
    public String toString() {
        return toQuery();
    }
}