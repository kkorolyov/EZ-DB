package dev.kkorolyov.dbbrowser.connection.concrete;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.dbbrowser.browser.DBLogger;
import dev.kkorolyov.dbbrowser.connection.DBConnection;
import dev.kkorolyov.dbbrowser.connection.PGColumn;
import dev.kkorolyov.dbbrowser.connection.TableConnection;
import dev.kkorolyov.dbbrowser.exceptions.NullTableException;

/**
 * A simple {@code TableConnection} implementation.
 * @see TableConnection
 * @see DBConnection
 */
public class SimpleTableConnection implements TableConnection {
	private static final DBLogger log = DBLogger.getLogger(SimpleTableConnection.class.getName());
	
	private DBConnection conn;
	private String tableName;
	private List<Statement> openStatements = new LinkedList<>();

	private final String metaDataStatement = "SELECT * FROM " + tableName;	// Metadata statement for this table
	
	/**
	 * Opens a new connection to a specified table on a database.
	 * @param conn database connection
	 * @param tableName name of table to connect to
	 * @throws NullTableException if such a table does not exist on the specified database
	 */
	public SimpleTableConnection(DBConnection conn, String tableName) throws NullTableException {		
		if (!conn.containsTable(tableName))
			throw new NullTableException(conn.getDBName(), tableName);
		
		this.conn = conn;
		this.tableName = tableName;
	}
	
	@Override
	public void close() {
		if (conn == null && openStatements == null)	// Already closed
			return;
		
		conn.close();
		conn = null;
		openStatements = null;
	}
	
	@Override
	public ResultSet select(String[] columns) throws SQLException {
		return select(columns, null);
	}
	@Override
	public ResultSet select(String[] columns, PGColumn[] criteria) throws SQLException {
		String selectStatement = "SELECT ";	// Initial select statement
		Object[] selectParameters = null;	// Parameters to use in execute call

		selectStatement += buildSelectColumns(columns);	// Add columns to statement
		
		selectStatement += " FROM " + tableName;	// Add table to statement

		if (criteria != null && criteria.length > 0) {
			selectStatement += " " + buildSelectCriteriaMarkers(criteria);	// Add criteria to statement
			
			selectParameters = new Object[criteria.length];
			for (int i = 0; i < selectParameters.length; i++) {
				selectParameters[i] = criteria[i].getValue();	// Build parameters to use in execute call
			}
		}
		
		return conn.execute(selectStatement, selectParameters);
	}
	private String buildSelectColumns(String[] columns) {
		String selectColumns = "";
		String wildcard = "*";
		
		if (columns[0].equals(wildcard))
			return wildcard;
		
		for (int i = 0; i < columns.length - 1; i++)
			selectColumns += columns[i] + ", ";
		selectColumns += columns[columns.length - 1];	// Add final column without a ", "
		
		return selectColumns;
	}
	private String buildSelectCriteriaMarkers(PGColumn[] criteria) {
		String selectCriteria = "WHERE ";
		
		selectCriteria += criteria[0].getName() + "=?";	// Mark for a PreparedStatement to set values later
		if (criteria.length > 1) {
			for (int i = 1; i < criteria.length; i++) {
				selectCriteria += " AND " + criteria[i].getName() + "=?";	// Mark
			}
		}
		return selectCriteria;
	}
	
	@Override
	public void flush() {
		conn.flush();
	}
	
	@Override
	public ResultSetMetaData getMetaData() {
		ResultSetMetaData rsmd = null;
		try {
			rsmd = conn.execute(metaDataStatement).getMetaData();
		} catch (SQLException e) {
			log.exceptionSevere(e);	// Metadata statement should not cause exception
		}
		return rsmd;
	}
	
	@Override
	public String getTableName() {
		return tableName;
	}
	@Override
	public String getDBName() {
		return conn.getDBName();
	}
	
	@Override
	public PGColumn[] getColumns() {	// TODO Reorganize into try
		ResultSetMetaData rsmd = getMetaData();
		int columnCount = 0;
		try {
			columnCount = rsmd.getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		PGColumn[] columns = new PGColumn[columnCount];
		
		for (int i = 0; i < columns.length; i++) {	// Build columns
			try {
				String columnName = rsmd.getColumnName(i + 1);	// RSMD column names start from 1
				PGColumn.Type columnType = null;
				
				switch (rsmd.getColumnType(i + 1)) {	// Set correct column type
				case (java.sql.Types.BOOLEAN):
					columnType = PGColumn.Type.BOOLEAN;
					break;
				case (java.sql.Types.CHAR):
					columnType = PGColumn.Type.CHAR;
					break;
				case (java.sql.Types.DOUBLE):
					columnType = PGColumn.Type.DOUBLE;
					break;
				case (java.sql.Types.INTEGER):
					columnType = PGColumn.Type.INTEGER;
					break;
				case (java.sql.Types.REAL):
					columnType = PGColumn.Type.REAL;
					break;
				case (java.sql.Types.VARCHAR):
					columnType = PGColumn.Type.VARCHAR;
					break;
				}
				columns[i] = new PGColumn(columnName, columnType);
			} catch (SQLException e) {
				log.exceptionSevere(e);
			}
		}
		return columns;
	}
	
	@Override
	public int getNumColumns() {
		int numColumns = 0;
		try {
			numColumns = getMetaData().getColumnCount();
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		return numColumns;
	}
	/**
	 * May take a while for large tables.
	 */
	@Override
	public int getNumRows() {
		int numRows = 0;
		try {
			ResultSet rs = conn.execute(metaDataStatement);
			while (rs.next())	// Counts rows
				numRows++;
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		return numRows;
	}
}
