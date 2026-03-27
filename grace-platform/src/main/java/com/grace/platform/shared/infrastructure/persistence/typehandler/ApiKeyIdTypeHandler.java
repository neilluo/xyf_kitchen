package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.ApiKeyId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(ApiKeyId.class)
public class ApiKeyIdTypeHandler extends BaseTypeHandler<ApiKeyId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiKeyId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public ApiKeyId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new ApiKeyId(value);
    }

    @Override
    public ApiKeyId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new ApiKeyId(value);
    }

    @Override
    public ApiKeyId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new ApiKeyId(value);
    }
}
