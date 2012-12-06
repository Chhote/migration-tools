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


import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;

/**
 * @author Sergey Bushik
 */
public class FileSqlExporter implements SqlExporter {

    private static final String SEMICOLON = ";";
    private int lines = 0;
    private File file;
    private String encoding;
    private BufferedWriter writer;

    public FileSqlExporter(String file, String encoding) {
        this(new File(file), encoding);
    }

    public FileSqlExporter(File file, String encoding) {
        this.file = file;
        this.encoding = encoding;
    }

    @Override
    public void open() throws Exception {
        lines = 0;
        writer = Files.newWriter(file, Charset.forName(encoding));
    }

    @Override
    public void export(String[] queries) throws Exception {
        if (writer == null) {
            throw new SqlGeneratorException("File is not opened");
        }
        if (queries == null) {
            return;
        }
        for (int i = 0, length = queries.length; i < length; i++) {
            String query = queries[i];
            if (lines++ > 0) {
                writer.newLine();
            }
            writer.write(query);
            if (!query.endsWith(";")) {
                writer.write(SEMICOLON);
            }

        }
    }

    @Override
    public void close() throws Exception {
        writer.flush();
        writer.close();
    }
}