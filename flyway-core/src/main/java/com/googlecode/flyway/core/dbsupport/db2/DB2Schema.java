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
import com.googlecode.flyway.core.dbsupport.JdbcTemplate;
import com.googlecode.flyway.core.dbsupport.Schema;
import com.googlecode.flyway.core.dbsupport.Table;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB2 implementation of Schema.
 */
public class DB2Schema extends Schema {
    /**
     * Creates a new DB2 schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param dbSupport    The database-specific support.
     * @param name         The name of the schema.
     */
    public DB2Schema(JdbcTemplate jdbcTemplate, DbSupport dbSupport, String name) {
        super(jdbcTemplate, dbSupport, name);
    }

    public boolean exists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM syscat.schemata WHERE schemaname=?", name) > 0;
    }

    public boolean empty() throws SQLException {
        int objectCount = jdbcTemplate.queryForInt("select count(*) from syscat.tables where tabschema = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.views where viewschema = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.sequences where seqschema = ?", name);
        objectCount += jdbcTemplate.queryForInt("select count(*) from syscat.indexes where indschema = ?", name);
        return objectCount == 0;
    }

    public void create() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA ?", name);
    }

    public void drop() throws SQLException {
        clean();
        jdbcTemplate.execute("DROP SCHEMA ? RESTRICT", name);
    }

    public void clean() throws SQLException {
        // MQTs are dropped when the backing views or tables are dropped
        // Indexes in DB2 are dropped when the corresponding table is dropped

        // views
        for (String dropStatement : generateDropStatements(name, "V", "VIEW")) {
            jdbcTemplate.execute(dropStatement);
        }

        // aliases
        for (String dropStatement : generateDropStatements(name, "A", "ALIAS")) {
            jdbcTemplate.execute(dropStatement);
        }

        // tables
        for (Table table : allTables()) {
            table.drop();
        }

        // sequences
        for (String dropStatement : generateDropStatementsForSequences(name)) {
            jdbcTemplate.execute(dropStatement);
        }
    }

    /**
     * Generates DROP statements for the sequences in this schema.
     *
     * @param schema The schema of the objects.
     * @return The drop statements.
     * @throws SQLException when the statements could not be generated.
     */
    private List<String> generateDropStatementsForSequences(String schema) throws SQLException {
        String dropSeqGenQuery = "select rtrim(SEQNAME) from SYSCAT.SEQUENCES where SEQSCHEMA = '" + schema
                + "' and SEQTYPE='S'";
        return buildDropStatements("drop sequence", dropSeqGenQuery, schema);
    }

    /**
     * Generates DROP statements for this type of table, representing this type of object in this schema.
     *
     * @param schema     The schema of the objects.
     * @param tableType  The type of table (Can be T, V, S, ...).
     * @param objectType The type of object.
     * @return The drop statements.
     * @throws SQLException when the statements could not be generated.
     */
    private List<String> generateDropStatements(String schema, String tableType, String objectType) throws SQLException {
        String dropTablesGenQuery = "select rtrim(TABNAME) from SYSCAT.TABLES where TYPE='" + tableType + "' and TABSCHEMA = '"
                + schema + "'";
        return buildDropStatements("DROP " + objectType, dropTablesGenQuery, schema);
    }

    /**
     * Builds the drop statements for database objects in this schema.
     *
     * @param dropPrefix The drop command for the database object (e.g. 'drop table').
     * @param query      The query to get all present database objects
     * @param schema     The schema for which to build the statements.
     * @return The statements.
     * @throws SQLException when the drop statements could not be built.
     */
    private List<String> buildDropStatements(final String dropPrefix, final String query, String schema) throws SQLException {
        List<String> dropStatements = new ArrayList<String>();
        List<String> dbObjects = jdbcTemplate.queryForStringList(query);
        for (String dbObject : dbObjects) {
            dropStatements.add(dropPrefix + " " + dbSupport.quote(schema, dbObject));
        }
        return dropStatements;
    }

    @Override
    public Table[] allTables() throws SQLException {
        List<String> tableNames = jdbcTemplate.queryForStringList(
                "select rtrim(TABNAME) from SYSCAT.TABLES where TYPE='T' and TABSCHEMA = ?", name);
        Table[] tables = new Table[tableNames.size()];
        for (int i = 0; i < tableNames.size(); i++) {
            tables[i] = new DB2Table(jdbcTemplate, dbSupport, this, tableNames.get(i));
        }
        return tables;
    }
}
