package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.MetadataId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(MetadataId.class)
public class MetadataIdTypeHandler extends BaseTypeHandler<MetadataId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MetadataId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public MetadataId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new MetadataId(value);
    }

    @Override
    public MetadataId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new MetadataId(value);
    }

    @Override
    public MetadataId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new MetadataId(value);
    }
}
