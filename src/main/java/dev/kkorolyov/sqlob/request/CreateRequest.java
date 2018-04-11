package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Request to create a table for a specific class.
 */
public class CreateRequest<T> extends Request<T> {
	/**
	 * Constructs a new create request.
	 * @see Request#Request(Class)
	 */
	public CreateRequest(Class<T> type) {
		super(type);
	}

	@Override
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		List<String> sql = ColumnHandlerFactory.stream()
				.flatMap(columnHandler -> columnHandler.expandCreates(this))
				.map(createRequest -> getCreateStatement(context))
				.collect(Collectors.toList());

		logStatements(sql);

		Statement statement = context.getStatement();
		for (String s : sql) statement.addBatch(s);
		statement.executeBatch();

		return new ConfigurableResult<>();
	}

	private String getCreateStatement(ExecutionContext context) {
		return Stream.concat(
				Stream.of(KeyColumn.ID),
				getColumns().stream()
		).map(column -> column.getSql(context))
				.collect(Collectors.joining(", ",
						"CREATE TABLE IF NOT EXISTS " + getName() + " (",
						")"));
	}
}
