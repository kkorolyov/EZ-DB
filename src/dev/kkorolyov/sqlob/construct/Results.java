package dev.kkorolyov.sqlob.construct;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.sqlob.connection.ClosedException;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Results obtained from a SQL query.
 * Wraps a {@code ResultSet} object and converts its results to a list of {@code RowEntry[]} objects with values converted to the appropriate Java type.
 * Maintains a cursor initially positioned directly before the first row of results.
 * @see ResultSet
 * @see RowEntry
 */
public class Results implements AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(Results.class.getName());
	
	private ResultSet rs;
	private List<Column> columns;
	
	/**
	 * Constructs a new {@code Results} object from a {@code ResultSet}.
	 * @param resultSet result set to wrap
	 */
	public Results(ResultSet resultSet) {
		this.rs = resultSet;
	}
	
	/**
	 * Closes this results object and releases all resources.
	 * Does nothing if called on a closed results.
	 */
	public void close() {
		if (isClosed())
			return;
		
		try {
			rs.close();
			rs = null;
		} catch (SQLException e) {
			log.exception(e);
		}
	}
	/** @return	{@code true} if this resource is closed, {@code false} if otherwise */
	public boolean isClosed() {
		try {
			rs.getMetaData();
		} catch (SQLException e) {
			close();
		}
		return (rs == null);
	}
	
	/**
	 * Retrieves the column information (name and type) associated with these results.
	 * @return all columns
	 * @throws ClosedException if called on a closed resource
	 */
	public List<Column> getColumns() {
		if (isClosed())
			throw new ClosedException();
		
		if (columns == null)	// Not yet cached
			columns = extractColumns(rs);
		
		return columns;
	}
	private static List<Column> extractColumns(ResultSet rs) {
		List<Column> columns = new LinkedList<>();
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				int rsmdColumn = i;	// ResultSet columns start from 1
				
				String columnName = rsmd.getColumnName(rsmdColumn);
				SqlType columnType = SqlType.get(rsmd.getColumnType(rsmdColumn));

				log.debug("Column name: " + columnName + " type: " + rsmd.getColumnType(rsmdColumn));
				
				columns.add(new Column(columnName, columnType));
			}
		} catch (SQLException e) {
			log.exception(e);	// Exception in private API, log instead of throw
		}
		return columns;
	}
	
	/** 
	 * Returns the number of columns in this results.
	 * @return total number of columns
	 * @throws ClosedException if called on a closed resource
	 */
	public int getNumColumns() {
		if (isClosed())
			throw new ClosedException();
		
		return getColumns().size();
	}
	
	/**
	 * Retrieves the next row of results.
	 * @return next row of results, or {@code null} if no more rows
	 * @throws ClosedException if called on a closed resource
	 */
	public List<RowEntry> getNextRow() {
		if (isClosed())
			throw new ClosedException();
			
		List<RowEntry> row = null;
		try {
			if (rs.next()) {
				row = new LinkedList<>();
				
				int rsCounter = 1;	// rs value index
				for (Column column : getColumns())
					row.add(new RowEntry(column, extractValue(rs, rsCounter++, column.getType())));
			}
		} catch (SQLException | MismatchedTypeException e) {
			log.exception(e);
		}
		return row;
	}
	private static Object extractValue(ResultSet rs, int rsIndex, SqlType valueType) {
		Object value = null;
		try {
			switch (valueType) {
			case BOOLEAN:
				value = rs.getBoolean(rsIndex);
				break;
			case SMALLINT:
				value = rs.getShort(rsIndex);
				break;
			case INTEGER:
				value = rs.getInt(rsIndex);
				break;
			case BIGINT:
				value = rs.getLong(rsIndex);
				break;
			case REAL:
				value = rs.getFloat(rsIndex);
				break;
			case DOUBLE:
				value = rs.getDouble(rsIndex);
				break;
			case CHAR:
				value = rs.getString(rsIndex).charAt(0);
				break;
			case VARCHAR:
				value = rs.getString(rsIndex);
				break;
			}
		} catch (SQLException e) {
			log.exception(e);
		}
		return value;
	}
}
