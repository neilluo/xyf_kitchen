package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.PublishRecordId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(PublishRecordId.class)
public class PublishRecordIdTypeHandler extends BaseTypeHandler<PublishRecordId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PublishRecordId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public PublishRecordId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new PublishRecordId(value);
    }

    @Override
    public PublishRecordId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new PublishRecordId(value);
    }

    @Override
    public PublishRecordId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new PublishRecordId(value);
    }
}
