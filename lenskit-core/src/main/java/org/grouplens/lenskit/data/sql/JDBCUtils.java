/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2012 Regents of the University of Minnesota and contributors
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
package org.grouplens.lenskit.data.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class JDBCUtils {
    private static Logger logger = LoggerFactory.getLogger(JDBCUtils.class);

    public static void execute(Connection dbc, String sql) throws SQLException {
        logger.debug("Executing: {}", sql);
        Statement stmt = dbc.createStatement();
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.error("Error executing {}: {}", sql, e.getMessage());
            throw e;
        } finally {
            stmt.close();
        }
    }
    
    /**
     * Safely clsoe a group of statements. Throws the first SQLException thrown
     * in closing the statements. If no SQLExceptions are thrown but closing a
     * statement throws a runtime exception, that is thrown.
     * 
     * @param statements A set of statements to close.
     * @throws SQLException if one of the statements fails. The other statements
     *         are still closed if possible.
     */
    public static void close(Statement... statements) throws SQLException {
        Exception err = null;
        for (Statement s: statements) {
            if (s != null) {
                try {
                    s.close();
                } catch (SQLException e) {
                    logger.error("Error closing statement: " + e.getMessage(), e);
                    if (err == null || err instanceof RuntimeException) {
                        err = e;
                    }
                } catch (RuntimeException e) {
                    logger.error("Error closing statement: " + e.getMessage(), e);
                    if (err == null || err instanceof RuntimeException) {
                        err = e;
                    }
                }
            }
        }
        if (err instanceof SQLException) {
            throw (SQLException) err;
        } else if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
        } else if (err != null) {
            throw new RuntimeException("Unexpected error", err);
        }
    }
}
