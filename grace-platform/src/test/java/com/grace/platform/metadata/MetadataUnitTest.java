package com.grace.platform.metadata;

import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Metadata domain.
 * <p>
 * Covers:
 * - Error 2001: Invalid metadata (tag count out of 5-15 range, title/description validation)
 * - Error 2003: Cannot update confirmed metadata
 *
 * Note: Error 9001 (LLM service unavailable after 3 retries) is covered in
 * QwenLlmServiceAdapter integration tests as it requires proper Spring context setup.
 *
 * @author Grace Platform Team
 */
class MetadataUnitTest {

    // ========== Error Code 2001: Invalid Metadata Tag Count ==========

    @Test
    @DisplayName("标签数量少于5个时validate()应抛出错误码2001")
    void shouldThrowError2001ForLessThan5Tags() {
        // Given: 创建元数据但标签数量只有4个
        List<String> fourTags = List.of("美食", "烹饪", "教程", "家常菜");

        // When/Then: 创建时应抛出异常，错误码2001
        assertThatThrownBy(() -> VideoMetadata.create(
                null,  // videoId will be set later
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                fourTags,
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA))
                .hasMessageContaining("标签")
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("标签数量超过15个时validate()应抛出错误码2001")
    void shouldThrowError2001ForMoreThan15Tags() {
        // Given: 创建元数据但标签数量有16个
        List<String> sixteenTags = List.of(
                "美食", "烹饪", "教程", "家常菜", "红烧肉",
                "川菜", "湘菜", "粤菜", "鲁菜", "苏菜",
                "闽菜", "浙菜", "徽菜", "京菜", "沪菜",
                "额外标签"
        );

        // When/Then: 创建时应抛出异常，错误码2001
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                sixteenTags,
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA))
                .hasMessageContaining("标签")
                .hasMessageContaining("15");
    }

    @Test
    @DisplayName("标签数量恰好为5个时应被接受")
    void shouldAcceptExactly5Tags() {
        // Given: 恰好5个标签
        List<String> fiveTags = List.of("美食", "烹饪", "教程", "家常菜", "红烧肉");

        // When/Then: 应成功创建
        assertThatCode(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                fiveTags,
                MetadataSource.MANUAL
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("标签数量恰好为15个时应被接受")
    void shouldAcceptExactly15Tags() {
        // Given: 恰好15个标签
        List<String> fifteenTags = List.of(
                "美食", "烹饪", "教程", "家常菜", "红烧肉",
                "川菜", "湘菜", "粤菜", "鲁菜", "苏菜",
                "闽菜", "浙菜", "徽菜", "京菜", "沪菜"
        );

        // When/Then: 应成功创建
        assertThatCode(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                fifteenTags,
                MetadataSource.MANUAL
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("update()时标签数量变为少于5个应抛出错误码2001")
    void shouldThrowError2001WhenUpdatingToLessThan5Tags() {
        // Given: 创建有效的元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );

        // When/Then: 更新时标签数量变为4个应抛出异常
        assertThatThrownBy(() -> metadata.update(
                null,
                null,
                List.of("美食", "烹饪", "教程", "家常菜")
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA));
    }

    @Test
    @DisplayName("update()时标签数量变为超过15个应抛出错误码2001")
    void shouldThrowError2001WhenUpdatingToMoreThan15Tags() {
        // Given: 创建有效的元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );

        // When/Then: 更新时标签数量变为16个应抛出异常
        List<String> sixteenTags = List.of(
                "美食", "烹饪", "教程", "家常菜", "红烧肉",
                "川菜", "湘菜", "粤菜", "鲁菜", "苏菜",
                "闽菜", "浙菜", "徽菜", "京菜", "沪菜",
                "额外标签"
        );

        assertThatThrownBy(() -> metadata.update(null, null, sixteenTags))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA));
    }

    // ========== Error Code 2003: Cannot Update Confirmed Metadata ==========

    @Test
    @DisplayName("confirmed=true时调用update()应抛出错误码2003")
    void shouldThrowError2003WhenUpdatingConfirmedMetadata() {
        // Given: 创建并确认元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );
        metadata.confirm();

        // Verify: 已确认状态
        assertThat(metadata.isConfirmed()).isTrue();

        // When/Then: 尝试更新应抛出异常，错误码2003
        assertThatThrownBy(() -> metadata.update(
                "新标题",
                "新描述",
                List.of("新标签1", "新标签2", "新标签3", "新标签4", "新标签5")
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.METADATA_ALREADY_CONFIRMED))
                .hasMessageContaining("已确认");
    }

    @Test
    @DisplayName("confirmed=true时调用update()只更新标题应抛出错误码2003")
    void shouldThrowError2003WhenUpdatingOnlyTitleOfConfirmedMetadata() {
        // Given: 创建并确认元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );
        metadata.confirm();

        // When/Then: 尝试只更新标题也应抛出异常
        assertThatThrownBy(() -> metadata.update("新标题", null, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.METADATA_ALREADY_CONFIRMED));
    }

    @Test
    @DisplayName("confirmed=true时调用update()只更新描述应抛出错误码2003")
    void shouldThrowError2003WhenUpdatingOnlyDescriptionOfConfirmedMetadata() {
        // Given: 创建并确认元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );
        metadata.confirm();

        // When/Then: 尝试只更新描述也应抛出异常
        assertThatThrownBy(() -> metadata.update(null, "新描述", null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.METADATA_ALREADY_CONFIRMED));
    }

    @Test
    @DisplayName("confirmed=true时调用update()只更新标签应抛出错误码2003")
    void shouldThrowError2003WhenUpdatingOnlyTagsOfConfirmedMetadata() {
        // Given: 创建并确认元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );
        metadata.confirm();

        // When/Then: 尝试只更新标签也应抛出异常
        assertThatThrownBy(() -> metadata.update(
                null,
                null,
                List.of("新标签1", "新标签2", "新标签3", "新标签4", "新标签5")
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.METADATA_ALREADY_CONFIRMED));
    }

    @Test
    @DisplayName("未确认的元数据应可以正常更新")
    void shouldAllowUpdateForUnconfirmedMetadata() {
        // Given: 创建未确认的元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );

        // Verify: 未确认状态
        assertThat(metadata.isConfirmed()).isFalse();

        // When: 执行更新
        assertThatCode(() -> metadata.update(
                "新标题",
                "新描述",
                List.of("新标签1", "新标签2", "新标签3", "新标签4", "新标签5")
        )).doesNotThrowAnyException();

        // Then: 更新应成功应用
        assertThat(metadata.getTitle()).isEqualTo("新标题");
        assertThat(metadata.getDescription()).isEqualTo("新描述");
        assertThat(metadata.getTags()).containsExactly("新标签1", "新标签2", "新标签3", "新标签4", "新标签5");
    }

    // ========== Additional Boundary Tests ==========

    @Test
    @DisplayName("validate()时标题为空应抛出错误码2001")
    void shouldThrowError2001ForBlankTitle() {
        // When/Then: 创建时标题为空
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                "",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA))
                .hasMessageContaining("标题");
    }

    @Test
    @DisplayName("validate()时标题超过100字符应抛出错误码2001")
    void shouldThrowError2001ForTitleOver100Chars() {
        // Given: 101个字符的标题
        String longTitle = "a".repeat(101);

        // When/Then: 创建时应抛出异常
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                longTitle,
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA))
                .hasMessageContaining("标题");
    }

    @Test
    @DisplayName("validate()时标题恰好100字符应被接受")
    void shouldAcceptTitleExactly100Chars() {
        // Given: 100个字符的标题
        String exactTitle = "a".repeat(100);

        // When/Then: 应成功创建
        assertThatCode(() -> VideoMetadata.create(
                null,
                exactTitle,
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate()时描述超过5000字符应抛出错误码2001")
    void shouldThrowError2001ForDescriptionOver5000Chars() {
        // Given: 5001个字符的描述
        String longDescription = "a".repeat(5001);

        // When/Then: 创建时应抛出异常
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                longDescription,
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA))
                .hasMessageContaining("描述");
    }

    @Test
    @DisplayName("validate()时描述恰好5000字符应被接受")
    void shouldAcceptDescriptionExactly5000Chars() {
        // Given: 5000个字符的描述
        String exactDescription = "a".repeat(5000);

        // When/Then: 应成功创建
        assertThatCode(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                exactDescription,
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        )).doesNotThrowAnyException();
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("空标签列表应抛出错误码2001")
    void shouldThrowError2001ForEmptyTags() {
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of(),
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA));
    }

    @Test
    @DisplayName("null标签列表应抛出错误码2001")
    void shouldThrowError2001ForNullTags() {
        assertThatThrownBy(() -> VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                null,
                MetadataSource.MANUAL
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METADATA));
    }

    @Test
    @DisplayName("已确认的元数据再次confirm()应抛出错误码2003")
    void shouldThrowError2003WhenConfirmingAlreadyConfirmedMetadata() {
        // Given: 创建并确认元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );
        metadata.confirm();

        // When/Then: 再次确认应抛出异常
        assertThatThrownBy(() -> metadata.confirm())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.METADATA_ALREADY_CONFIRMED));
    }

    @Test
    @DisplayName("AI_GENERATED源更新后应变为AI_EDITED")
    void shouldChangeSourceToAiEditedAfterUpdate() {
        // Given: AI生成的元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.AI_GENERATED
        );

        // When: 执行更新
        metadata.update("新标题", null, null);

        // Then: 源应变为AI_EDITED
        assertThat(metadata.getSource()).isEqualTo(MetadataSource.AI_EDITED);
    }

    @Test
    @DisplayName("MANUAL源更新后应保持MANUAL")
    void shouldKeepManualSourceAfterUpdate() {
        // Given: 手动创建的元数据
        VideoMetadata metadata = VideoMetadata.create(
                null,
                "红烧肉做法",
                "详细的红烧肉烹饪教程",
                List.of("美食", "烹饪", "教程", "家常菜", "红烧肉"),
                MetadataSource.MANUAL
        );

        // When: 执行更新
        metadata.update("新标题", null, null);

        // Then: 源应保持MANUAL
        assertThat(metadata.getSource()).isEqualTo(MetadataSource.MANUAL);
    }
}
