package com.grace.platform.shared.infrastructure.persistence.typehandler;

import com.grace.platform.shared.domain.id.PromotionRecordId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(PromotionRecordId.class)
public class PromotionRecordIdTypeHandler extends BaseTypeHandler<PromotionRecordId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PromotionRecordId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public PromotionRecordId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new PromotionRecordId(value);
    }

    @Override
    public PromotionRecordId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new PromotionRecordId(value);
    }

    @Override
    public PromotionRecordId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new PromotionRecordId(value);
    }
}
