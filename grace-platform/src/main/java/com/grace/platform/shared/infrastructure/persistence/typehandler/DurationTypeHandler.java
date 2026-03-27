package com.grace.platform.shared.infrastructure.persistence.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.Duration;

/**
 * Duration 类型处理器。
 * <p>
 * 将 {@link Duration} 与数据库 BIGINT（秒数）进行转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@MappedTypes(Duration.class)
public class DurationTypeHandler extends BaseTypeHandler<Duration> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Duration parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter.getSeconds());
    }

    @Override
    public Duration getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long seconds = rs.getLong(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    @Override
    public Duration getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        long seconds = rs.getLong(columnIndex);
        if (rs.wasNull()) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    @Override
    public Duration getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        long seconds = cs.getLong(columnIndex);
        if (cs.wasNull()) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }
}
