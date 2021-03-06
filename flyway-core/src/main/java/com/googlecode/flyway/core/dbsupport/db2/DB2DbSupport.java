/**
 * Copyright (C) 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.flyway.core.dbsupport.db2;

import com.googlecode.flyway.core.dbsupport.DbSupport;
import com.googlecode.flyway.core.dbsupport.Schema;
import com.googlecode.flyway.core.dbsupport.SqlStatementBuilder;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DB2 Support.
 */
public class DB2DbSupport extends DbSupport {
    /**
     * Creates a new instance.
     *
     * @param connection The connection to use.
     */
    public DB2DbSupport(Connection connection) {
        super(new DB2JdbcTemplate(connection));
    }

    public SqlStatementBuilder createSqlStatementBuilder() {
        return new DB2SqlStatementBuilder();
    }

    public String getScriptLocation() {
        return "com/googlecode/flyway/core/dbsupport/db2/";
    }

    public boolean isSchemaEmpty(String schema) throws SQLException {
        int objectCount = jdbcTemplate.queryForInt("select count(*) from syscat.tables where tabschema = ?", schema);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.views where viewschema = ?", schema);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.sequences where seqschema = ?", schema);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.indexes where indschema = ?", schema);
        return objectCount == 0;
    }

    @Override
    public boolean schemaExists(String schema) throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM syscat.schemata WHERE schemaname=?", schema) > 0;
    }

    public boolean tableExistsNoQuotes(String schema, String table) throws SQLException {
        return tableExists(null, schema.toUpperCase(), table.toUpperCase());
    }

    public boolean tableExists(String schema, String table) throws SQLException {
        return tableExists(null, schema, table);
    }

    @Override
    public boolean primaryKeyExists(String schema, String table) throws SQLException {
        return primaryKeyExists(null, schema, table);
    }

    public boolean columnExists(String schema, String table, String column) throws SQLException {
        return columnExists(null, schema, table, column);
    }

    public String getCurrentSchema() throws SQLException {
        return jdbcTemplate.queryForString("select current_schema from sysibm.sysdummy1").trim();
    }

    @Override
    public void setCurrentSchema(String schema) throws SQLException {
        jdbcTemplate.execute("SET SCHEMA " + quote(schema));
    }

    public String getCurrentUserFunction() {
        return "CURRENT_USER";
    }

    public boolean supportsDdlTransactions() {
        return true;
    }

    public void lockTable(String schema, String table) throws SQLException {
        jdbcTemplate.update("lock table " + quote(schema) + "." + quote(table) + " in exclusive mode");
    }

    public String getBooleanTrue() {
        return "1";
    }

    public String getBooleanFalse() {
        return "0";
    }

    @Override
    public String doQuote(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public Schema getSchema(String name) {
        return new DB2Schema(jdbcTemplate, this, name);
    }
}
