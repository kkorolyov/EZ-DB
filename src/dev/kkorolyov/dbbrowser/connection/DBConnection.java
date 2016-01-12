package dev.kkorolyov.dbbrowser.connection;

import java.sql.ResultSet;
import java.sql.SQLException;

import dev.kkorolyov.dbbrowser.exceptions.DuplicateTableException;
import dev.kkorolyov.dbbrowser.exceptions.NullParameterException;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;

/**
 * Opens a connection to a single database and allows for SQL statement execution.
 */
public interface DBConnection {
	
	/**
	 * Connects to a table on this database, if it exists.
	 * @param table name of table to connect to
	 * @return connection to the table, if it exists, {@code null} if otherwise
	 */
	TableConnection connect(String table);
	
	/**
	 * Closes the connection and releases all resources.
	 * Has no effect if called on a closed connection.
	 */
	void close();
	
	/**
	 * Executes a complete SQL statement.
	 * @param statement statement to execute
	 * @return results from statement execution
	 * @throws SQLException if attempting to execute an invalid statement
	 */
	ResultSet execute(String statement) throws SQLException;
	/**
	 * Executes a partial SQL statement with parameters declared separately.
	 * @param baseStatement statement without parameters, with {@code ?} denoting an area where a parameter should be substituted in
	 * @param parameters parameters to use, will be substituted into the base statement in the order of appearance
	 * @return results from statement execution
	 * @throws SQLException if attempting to execute an invalid statement
	 */
	ResultSet execute(String baseStatement, Object[] parameters) throws SQLException;
	
	/**
	 * Closes all opened statements.
	 */
	void flush();
	
	/**
	 * Creates a table with the specifed name and columns.
	 * @param table new table name
	 * @param columns new table columns (name and type)
	 */
	void createTable(String table, PGColumn[] columns) throws DuplicateTableException, NullParameterException;
	/**
	 * Drops a table from the database.
	 * @param table name of table to drop
	 */
	void dropTable(String table) throws NullTableException;
	
	/**
	 * @param table name of table to search for
	 * @return {@code true} if this database contains a table of the specified name (ignoring case), {@code false} if otherwise 
	 */
	boolean containsTable(String table);
	
	/**
	 * @return names of all tables in this database.
	 */
	String[] getTables();
	
	/**
	 * @return name of this database
	 */
	String getDBName();
}
