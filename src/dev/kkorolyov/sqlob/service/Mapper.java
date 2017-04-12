package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;
import static dev.kkorolyov.sqlob.service.Constants.sanitize;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.Extractor;

/**
 * Provides for data mapping between Java and SQL.
 */
public final class Mapper {
	private static final Logger log = Logger.getLogger(Mapper.class.getName());

	private final Map<Class<?>, String> typeMap = new HashMap<>();
	private final Map<Class<?>, Extractor> extractorMap = new HashMap<>();

	/**
	 * Constructs a new mapper with default mappings.
	 */
	public Mapper() {
		put(Byte.class, "TINYINT", ResultSet::getByte);
		put(Short.class, "SMALLINT", ResultSet::getShort);
		put(Integer.class, "INTEGER", ResultSet::getInt);
		put(Long.class, "BIGINT", ResultSet::getLong);
		put(Float.class, "REAL", ResultSet::getFloat);
		put(Double.class, "DOUBLE", ResultSet::getDouble);
		put(BigDecimal.class, "NUMERIC", ResultSet::getBigDecimal);

		put(Boolean.class, "BOOLEAN", ResultSet::getBoolean);

		put(Character.class, "CHAR(1)", (rs, column) -> {
			String string = rs.getString(column);
			return string == null ? null : string.charAt(0);
		});
		put(String.class, "VARCHAR(1024)", ResultSet::getString);

		put(byte[].class, "VARBINARY(1024)", ResultSet::getBytes);

		put(Date.class, "DATE", ResultSet::getDate);
		put(Time.class, "TIME(6)", ResultSet::getTime);
		put(Timestamp.class, "TIMESTAMP(6)", ResultSet::getTimestamp);

		put(UUID.class, ID_TYPE, (rs, column) -> UUID.fromString(rs.getString(column)));
	}

	/**
	 * Creates a new mapping, replacing any old mapping.
	 * @param c Java class
	 * @param sqlType associated SQL type
	 * @param extractor function transforming a SQL column of type {@code sqlType} to an instance of {@code c}
	 */
	public <T> void put(Class<T> c, String sqlType, Extractor<T> extractor) {
		String sanitizedSqlType = sanitize(sqlType);

		typeMap.put(c, sanitizedSqlType);
		extractorMap.put(c, extractor);

		log.debug(() -> "Put mapping: " + c + "->" + sanitizedSqlType);
	}

	Iterable<Field> getPersistableFields(Class<?> c) {
		return StreamSupport.stream(Arrays.spliterator(c.getFields()), true)
												.filter(Mapper::isPersistable).collect(Collectors.toSet());
	}
	private static boolean isPersistable(Field f) {
		int modifiers = f.getModifiers();
		return !(Modifier.isStatic(modifiers) ||
						 Modifier.isTransient(modifiers)) &&
					 f.getAnnotation(Transient.class) == null;
	}

	/** @return {@code c} and all persisted non-primitive classes used both directly and indirectly by {@code c} */
	Iterable<Class<?>> getAssociatedClasses(Class<?> c) {
		Set<Class<?>> dependencies = new HashSet<>();
		Stack<Class<?>> nonPrimitives = new Stack<>();

		dependencies.add(c);
		nonPrimitives.push(c);

		while (!nonPrimitives.isEmpty()) {	// BFS until primitive classes reached or all classes seen
			for (Field f : getPersistableFields(nonPrimitives.pop())) {
				if (!isPrimitive(f) && !dependencies.contains(f.getClass())) {
					Class<?> nonPrimitive = f.getClass();

					dependencies.add(nonPrimitive);
					nonPrimitives.push(nonPrimitive);
				}
			}
		}
		return dependencies;
	}

	/** @return {@code true} if a primitive SQL type is associated with {@code f}'s class */
	boolean isPrimitive(Field f) {
		return getSql(f) == null;
	}

	/** @return SQL type associated with {@code f}'s class */
	String getSql(Field f) {
		return getSql(f.getClass());
	}
	/** @return extractor associated with {@code f}'s class */
	Extractor<?> getExtractor(Field f) {
		return getExtractor(f.getClass());
	}

	/** @return SQL type associated with {@code c} */
	String getSql(Class<?> c) {
		return typeMap.get(c);
	}
	/** @return extractor associated with {@code c} */
	<T> Extractor<T> getExtractor(Class<T> c) {
		return extractorMap.get(c);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Mapper mapper = (Mapper) o;
		return Objects.equals(typeMap, mapper.typeMap) &&
					 Objects.equals(extractorMap, mapper.extractorMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeMap, extractorMap);
	}

	@Override
	public String toString() {
		return "Mapper{" +
					 "typeMap=" + typeMap +
					 ", extractorMap=" + extractorMap +
					 '}';
	}
}