package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.storage.domain.StorageProvider;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(StorageProvider.class)
public class StorageProviderTypeHandler extends BaseTypeHandler<StorageProvider> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, StorageProvider parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public StorageProvider getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : StorageProvider.valueOf(value);
    }

    @Override
    public StorageProvider getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : StorageProvider.valueOf(value);
    }

    @Override
    public StorageProvider getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : StorageProvider.valueOf(value);
    }
}