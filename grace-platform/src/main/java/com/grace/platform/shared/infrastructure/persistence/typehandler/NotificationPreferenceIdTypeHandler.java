package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(NotificationPreferenceId.class)
public class NotificationPreferenceIdTypeHandler extends BaseTypeHandler<NotificationPreferenceId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, NotificationPreferenceId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public NotificationPreferenceId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new NotificationPreferenceId(value);
    }

    @Override
    public NotificationPreferenceId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new NotificationPreferenceId(value);
    }

    @Override
    public NotificationPreferenceId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new NotificationPreferenceId(value);
    }
}
