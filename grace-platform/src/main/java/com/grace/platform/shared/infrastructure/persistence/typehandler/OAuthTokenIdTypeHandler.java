package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.OAuthTokenId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(OAuthTokenId.class)
public class OAuthTokenIdTypeHandler extends BaseTypeHandler<OAuthTokenId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OAuthTokenId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public OAuthTokenId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new OAuthTokenId(value);
    }

    @Override
    public OAuthTokenId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new OAuthTokenId(value);
    }

    @Override
    public OAuthTokenId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new OAuthTokenId(value);
    }
}
