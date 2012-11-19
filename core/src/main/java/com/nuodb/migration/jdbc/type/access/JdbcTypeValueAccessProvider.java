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
package com.nuodb.migration.jdbc.type.access;

import com.nuodb.migration.jdbc.model.ColumnModel;
import com.nuodb.migration.jdbc.model.ColumnModelFactory;
import com.nuodb.migration.jdbc.type.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.nuodb.migration.jdbc.model.ColumnModelFactory.createColumnModel;

/**
 * @author Sergey Bushik
 */
@SuppressWarnings("unchecked")
public class JdbcTypeValueAccessProvider extends JdbcTypeRegistryBase {

    public JdbcTypeValueAccessProvider(JdbcTypeRegistry jdbcTypeRegistry) {
        super(jdbcTypeRegistry);
    }

    public <T> JdbcTypeValueGetter<T> getJdbcTypeValueGetter(int typeCode) {
        return getJdbcTypeValueGetter(new JdbcTypeDesc(typeCode));
    }

    public <T> JdbcTypeValueGetter<T> getJdbcTypeValueGetter(int typeCode, String typeName) {
        return getJdbcTypeValueGetter(new JdbcTypeDesc(typeCode, typeName));
    }

    public <T> JdbcTypeValueGetter<T> getJdbcTypeValueGetter(JdbcTypeDesc jdbcTypeDesc) {
        return getJdbcTypeValueGetter(getJdbcTypeRequired(jdbcTypeDesc));
    }

    public <T> JdbcTypeValueGetter<T> getJdbcTypeValueGetter(JdbcType<T> jdbcType) {
        return new JdbcTypeValueGetterImpl<T>(jdbcType);
    }

    public <T> JdbcTypeValueSetter<T> getJdbcTypeValueSetter(int typeCode) {
        return getJdbcTypeValueSetter(new JdbcTypeDesc(typeCode));
    }

    public <T> JdbcTypeValueSetter<T> getJdbcTypeValueSetter(int typeCode, String typeName) {
        return getJdbcTypeValueSetter(new JdbcTypeDesc(typeCode, typeName));
    }

    public <T> JdbcTypeValueSetter<T> getJdbcTypeValueSetter(JdbcTypeDesc jdbcTypeDesc) {
        return getJdbcTypeValueSetter(getJdbcTypeRequired(jdbcTypeDesc));
    }

    public <T> JdbcTypeValueSetter<T> getJdbcTypeValueSetter(JdbcType<T> jdbcType) {
        return new JdbcTypeValueSetterImpl<T>(jdbcType);
    }

    public <T> JdbcTypeValueAccess<T> getResultSetAccess(ResultSet resultSet, int column) {
        try {
            return getResultSetAccess(resultSet, column,
                    ColumnModelFactory.createColumnModel(resultSet, column));
        } catch (SQLException exception) {
            throw new JdbcTypeException(exception);
        }
    }

    public <T> JdbcTypeValueAccess<T> getResultSetAccess(ResultSet resultSet, int column,
                                                         ColumnModel columnModel) {
        return new JdbcTypeValueAccessImpl<T>(
                (JdbcTypeValueGetter<T>) getJdbcTypeValueGetter(
                        columnModel.getTypeCode(), columnModel.getTypeName()), resultSet, column, columnModel);
    }

    public <T> JdbcTypeValueAccess<T> getStatementAccess(PreparedStatement statement, int column) {
        try {
            return getStatementAccess(statement, column, createColumnModel(statement.getMetaData(), column));
        } catch (SQLException exception) {
            throw new JdbcTypeException(exception);
        }
    }

    public <T> JdbcTypeValueAccess<T> getStatementAccess(PreparedStatement statement, int column,
                                                         ColumnModel columnModel) {
        return new JdbcTypeValueAccessImpl<T>(
                (JdbcTypeValueSetter<T>) getJdbcTypeValueSetter(
                        columnModel.getTypeCode(), columnModel.getTypeName()), statement, column, columnModel);
    }

    protected <T> JdbcTypeAdapter<T> getJdbcTypeAdapter(Class valueClass, Class typeClass) {
        JdbcTypeAdapter jdbcTypeAdapter = null;
        if (valueClass != null && !valueClass.isAssignableFrom(typeClass)) {
            jdbcTypeAdapter = getJdbcTypeAdapter(typeClass);
            if (jdbcTypeAdapter == null) {
                throw new JdbcTypeException(
                        String.format("Jdbc type %s adapter not found to adapt %s type",
                                typeClass.getName(), valueClass.getName()));
            }
        }
        return jdbcTypeAdapter;
    }

    protected JdbcType getJdbcTypeRequired(JdbcTypeDesc jdbcTypeDesc) {
        JdbcType jdbcType = getJdbcType(jdbcTypeDesc);
        if (jdbcType == null) {
            throw new JdbcTypeException(
                    String.format("Jdbc type %s is not supported", jdbcTypeDesc.getTypeName()));
        }
        return jdbcType;
    }

    class JdbcTypeValueGetterImpl<T> implements JdbcTypeValueGetter<T> {

        private JdbcType<T> jdbcType;

        public JdbcTypeValueGetterImpl(JdbcType<T> jdbcType) {
            this.jdbcType = jdbcType;
        }

        @Override
        public JdbcType<T> getJdbcType() {
            return jdbcType;
        }

        @Override
        public T getValue(ResultSet resultSet, int column) throws SQLException {
            return jdbcType.getValue(resultSet, column);
        }

        @Override
        public <X> X getValue(ResultSet resultSet, int column, Class<X> valueClass) throws SQLException {
            X value = (X) jdbcType.getValue(resultSet, column);
            JdbcTypeAdapter<X> adapter = getJdbcTypeAdapter(valueClass, jdbcType.getTypeClass());
            if (adapter != null) {
                Statement statement = resultSet.getStatement();
                value = adapter.unwrap(value, valueClass, statement.getConnection());
            }
            return value;
        }
    }

    class JdbcTypeValueSetterImpl<T> implements JdbcTypeValueSetter<T> {

        private JdbcType<T> jdbcType;

        public JdbcTypeValueSetterImpl(JdbcType<T> jdbcType) {
            this.jdbcType = jdbcType;
        }

        @Override
        public JdbcType<T> getJdbcType() {
            return jdbcType;
        }

        @Override
        public <X> void setValue(PreparedStatement statement, int column, X value) throws SQLException {
            JdbcTypeAdapter<X> adapter = getJdbcTypeAdapter(value != null ? value.getClass() : null,
                    jdbcType.getTypeClass());
            if (adapter != null) {
                value = adapter.wrap(value, statement.getConnection());
            }
            jdbcType.setValue(statement, column, (T) value);
        }
    }

    class JdbcTypeValueAccessImpl<T> implements JdbcTypeValueAccess<T> {

        private JdbcTypeValueGetter<T> getter;
        private ResultSet resultSet;
        private JdbcTypeValueSetter<T> setter;
        private PreparedStatement statement;
        private ColumnModel columnModel;
        private int column;

        public JdbcTypeValueAccessImpl(JdbcTypeValueGetter<T> getter, ResultSet resultSet,
                                       int column, ColumnModel columnModel) {
            this.getter = getter;
            this.resultSet = resultSet;
            this.columnModel = columnModel;
            this.column = column;
        }

        public JdbcTypeValueAccessImpl(JdbcTypeValueSetter<T> setter, PreparedStatement statement,
                                       int column, ColumnModel columnModel) {
            this.setter = setter;
            this.statement = statement;
            this.columnModel = columnModel;
            this.column = column;
        }

        @Override
        public T getValue() throws SQLException {
            if (getter == null) {
                throw new JdbcTypeException("Get value is unsupported");
            }
            return getter.getValue(resultSet, column);
        }

        @Override
        public <X> X getValue(Class<X> valueClass) throws SQLException {
            if (getter == null) {
                throw new JdbcTypeException("Get value is unsupported");
            }
            return getter.getValue(resultSet, column, valueClass);
        }

        @Override
        public <X> void setValue(X value) throws SQLException {
            if (setter == null) {
                throw new JdbcTypeException("Set value is unsupported");
            }
            setter.setValue(statement, column, value);
        }

        @Override
        public ColumnModel getColumnModel() {
            return columnModel;
        }
    }
}
