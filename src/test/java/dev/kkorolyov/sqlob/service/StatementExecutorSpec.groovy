package dev.kkorolyov.sqlob.service

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class StatementExecutorSpec extends Specification {
  Mapper mapper = Mock()
	StatementGenerator generator = Mock()

	ResultSet rs = Mock()
	Statement s = Mock()
	PreparedStatement ps = Mock()
	Connection conn = Mock()

	StatementExecutor executor = new StatementExecutor(mapper)

	def setup() {
		rs.getObject(_ as String) >> null
		s.executeQuery(_) >> rs
		ps.executeQuery() >> rs
		conn.createStatement() >> s
		conn.prepareStatement(_) >> ps

		Field generatorField = executor.class.getDeclaredField("generator")
		generatorField.setAccessible(true)
		generatorField.set(executor, generator)

		executor.setConnection(conn)
	}

	def "create() creates as a batch"() {
		Class c = new Object() {}.class
		Iterable<String> creates = ["1", "2", "3", "4"]

		1 * generator.create(c) >> creates

		when:
		executor.create(c)

		then:
		creates.each {
			1 * s.addBatch(it)
		}
		1 * s.executeBatch()
	}

	def "setting new Connection commits current Connection"() {
		when:
		executor.setConnection(Mock(Connection))

		then:
		1 * conn.commit()
	}

	def "flush() commits underlying Connection"() {
		when:
		executor.flush()

		then:
		1 * conn.commit()
	}
	def "close() commits before closing"() {
		when:
		executor.close()

		then:
		executor.isClosed()
		1 * conn.commit()
	}
}
