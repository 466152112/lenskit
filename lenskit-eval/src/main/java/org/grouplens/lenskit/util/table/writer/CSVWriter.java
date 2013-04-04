/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.util.table.writer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.grouplens.lenskit.util.io.CompressionMode;
import org.grouplens.lenskit.util.io.LKFileUtils;

import com.google.common.io.Files;
import org.grouplens.lenskit.util.table.TableLayout;

/**
 * Implementation of {@link TableWriter} for CSV files.
 *
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
public class CSVWriter implements TableWriter {
    private Writer writer;
    private TableLayout layout;

    /**
     * Construct a new CSV writer.
     *
     * @param w The underlying writer to output to.
     * @param l The table layout, or {@code null} if the table has no headers.
     * @throws IOException if there is an error writing the column headers.
     */
    public CSVWriter(@Nonnull Writer w, @Nullable TableLayout l) throws IOException {
        Preconditions.checkNotNull(w, "writer must not be null");
        layout = l;
        writer = w;
        if (layout != null) {
            writeRow(layout.getColumns().toArray(new String[l.getColumnCount()]));
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
        writer = null;
    }

    String quote(String e) {
        if (e == null) {
            return "";
        }

        if (e.matches("[\r\n,\"]")) {
            return "\"" + e.replaceAll("\"", "\"\"") + "\"";
        } else {
            return e;
        }
    }

    @Override
    public synchronized void writeRow(Object[] row) throws IOException {
        if (layout != null && row.length != layout.getColumnCount()) {
            throw new IllegalArgumentException("row too long");
        }

        final int n = layout == null ? row.length : layout.getColumnCount();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                writer.write(',');
            }
            if (i < row.length) {
                Object val = row[i];
                if (val != null) {
                    writer.write(quote(val.toString()));
                }
            }
        }
        writer.write('\n');
        writer.flush();
    }

    @Override
    public TableLayout getLayout() {
        return layout;
    }

    /**
     * Open a CSV writer to write to a file.
     *
     * @param file        The file to write to.
     * @param layout      The layout of the table.
     * @param compression What compression, if any, to use.
     * @return A CSV writer outputting to {@code file}.
     * @throws IOException if there is an error opening the file or writing the column header.
     */
    public static CSVWriter open(File file, TableLayout layout, CompressionMode compression) throws IOException {
        Files.createParentDirs(file);
        Writer writer = LKFileUtils.openOutput(file, Charset.defaultCharset(), compression);
        try {
            return new CSVWriter(writer, layout);
        } catch (RuntimeException e) {
            LKFileUtils.close(writer);
            throw e;
        } catch (IOException e) {
            LKFileUtils.close(writer);
            throw e;
        }
    }

    /**
     * Open a CSV writer to write to an auto-compressed file. The file will be compressed if its
     * name ends in ".gz".
     *
     * @param file   The file.
     * @param layout The table layout.
     * @return The CSV writer.
     * @throws IOException if there is an error opening the file or writing the column header.
     * @see #open(File, TableLayout, CompressionMode)
     */
    public static CSVWriter open(File file, @Nullable TableLayout layout) throws IOException {
        return open(file, layout, CompressionMode.AUTO);
    }
}
