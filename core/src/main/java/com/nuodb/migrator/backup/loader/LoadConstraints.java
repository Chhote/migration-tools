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
package com.nuodb.migrator.backup.loader;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.nuodb.migrator.jdbc.metadata.ForeignKey;
import com.nuodb.migrator.jdbc.metadata.Index;
import com.nuodb.migrator.jdbc.metadata.MetaDataType;
import com.nuodb.migrator.jdbc.metadata.PrimaryKey;
import com.nuodb.migrator.jdbc.metadata.Table;

import java.util.Iterator;

import static com.google.common.collect.ArrayListMultimap.create;
import static com.google.common.collect.Multimaps.filterValues;
import static org.apache.commons.lang3.ArrayUtils.contains;

/**
 * @author Sergey Bushik
 */
public class LoadConstraints implements Iterable<LoadConstraint> {

    private Multimap<Table, LoadConstraint> loadConstraints = create();

    public LoadConstraints() {
    }

    public LoadConstraints(Multimap<Table, LoadConstraint> loadConstraints) {
        putLoadConstraints(loadConstraints);
    }

    @Override
    public Iterator<LoadConstraint> iterator() {
        return loadConstraints.values().iterator();
    }

    public Multimap<Table, LoadConstraint> getLoadConstraints() {
        return loadConstraints;
    }

    public Multimap<Table, LoadConstraint> getLoadConstraints(final MetaDataType... objectTypes) {
        return filterValues(getLoadConstraints(), new Predicate<LoadConstraint>() {
            @Override
            public boolean apply(LoadConstraint loadConstraint) {
                return contains(objectTypes, loadConstraint.getConstraint().getObjectType());
            }
        });
    }

    public void putIndex(Index index) {
        putLoadConstraint(new LoadConstraint(index));
    }

    public void putPrimaryKey(PrimaryKey primaryKey) {
        putLoadConstraint(new LoadConstraint(primaryKey));
    }

    public void putForeignKey(ForeignKey foreignKey) {
        putLoadConstraint(new LoadConstraint(foreignKey));
    }

    public void putLoadConstraint(LoadConstraint loadConstraint) {
        this.loadConstraints.put(loadConstraint.getTable(), loadConstraint);
    }

    public void putLoadConstraints(Multimap<Table, LoadConstraint> loadConstraints) {
        this.loadConstraints.putAll(loadConstraints);
    }

    public void setLoadConstraints(Multimap<Table, LoadConstraint> loadConstraints) {
        this.loadConstraints = loadConstraints;
    }
}
