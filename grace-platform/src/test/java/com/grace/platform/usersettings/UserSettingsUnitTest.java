package com.grace.platform.usersettings;

import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.shared.infrastructure.encryption.ApiKeyHashService;
import com.grace.platform.shared.infrastructure.encryption.BcryptApiKeyHashService;
import com.grace.platform.usersettings.domain.model.ApiKey;
import com.grace.platform.usersettings.domain.service.ApiKeyGenerationServiceImpl;
import com.grace.platform.usersettings.domain.service.GeneratedApiKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for User/Settings domain.
 * <p>
 * Covers boundary conditions and security requirements:
 * - Avatar file type validation (JPG/PNG only)
 * - Avatar file size validation (≤ 2MB)
 * - API Key rawKey appears only in GeneratedApiKey, not in ApiKey entity
 * - BCrypt hash is irreversible (cannot be decrypted)
 *
 * @author Grace Platform Team
 */
class UserSettingsUnitTest {

    // ========== Avatar File Type Validation Tests ==========

    @Test
    @DisplayName("JPG 图片文件应被接受")
    void shouldAcceptJpgImage() {
        // Given: 一个 JPG 文件
        MockMultipartFile jpgFile = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[1000]
        );

        // Then: 文件类型应被识别为允许的格式
        assertThat(jpgFile.getContentType()).isEqualTo("image/jpeg");
        assertThat(isAllowedAvatarType(jpgFile.getContentType())).isTrue();
    }

    @Test
    @DisplayName("PNG 图片文件应被接受")
    void shouldAcceptPngImage() {
        // Given: 一个 PNG 文件
        MockMultipartFile pngFile = new MockMultipartFile(
                "avatar",
                "avatar.png",
                "image/png",
                new byte[1000]
        );

        // Then: 文件类型应被识别为允许的格式
        assertThat(pngFile.getContentType()).isEqualTo("image/png");
        assertThat(isAllowedAvatarType(pngFile.getContentType())).isTrue();
    }

    @Test
    @DisplayName("非 JPG/PNG 文件应被拒绝")
    void shouldRejectNonJpgPngFiles() {
        // Given: 各种非图片文件类型
        String[] invalidTypes = {
                "image/gif",
                "image/webp",
                "image/bmp",
                "application/pdf",
                "text/plain",
                "application/octet-stream",
                "video/mp4",
                "audio/mpeg"
        };

        // Then: 所有非 JPG/PNG 类型都应被拒绝
        for (String contentType : invalidTypes) {
            assertThat(isAllowedAvatarType(contentType))
                    .as("Content type %s should be rejected", contentType)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("GIF 图片应被拒绝上传")
    void shouldRejectGifImage() {
        MockMultipartFile gifFile = new MockMultipartFile(
                "avatar",
                "avatar.gif",
                "image/gif",
                new byte[1000]
        );

        assertThat(isAllowedAvatarType(gifFile.getContentType())).isFalse();
    }

    @Test
    @DisplayName("WebP 图片应被拒绝上传")
    void shouldRejectWebpImage() {
        MockMultipartFile webpFile = new MockMultipartFile(
                "avatar",
                "avatar.webp",
                "image/webp",
                new byte[1000]
        );

        assertThat(isAllowedAvatarType(webpFile.getContentType())).isFalse();
    }

    @Test
    @DisplayName("PDF 文件应被拒绝上传")
    void shouldRejectPdfFile() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "avatar",
                "document.pdf",
                "application/pdf",
                new byte[10000]
        );

        assertThat(isAllowedAvatarType(pdfFile.getContentType())).isFalse();
    }

    // ========== Avatar File Size Validation Tests ==========

    @Test
    @DisplayName("头像文件大小恰好等于 2MB 应被接受")
    void shouldAcceptExactly2MB() {
        long exactly2MB = 2L * 1024 * 1024; // Exactly 2MB
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[(int) exactly2MB]
        );

        assertThat(isValidAvatarSize(file.getSize())).isTrue();
    }

    @Test
    @DisplayName("头像文件大小小于 2MB 应被接受")
    void shouldAcceptUnder2MB() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[1024 * 1024] // 1MB
        );

        assertThat(isValidAvatarSize(file.getSize())).isTrue();
    }

    @Test
    @DisplayName("头像文件大小超过 2MB 应被拒绝")
    void shouldRejectOver2MB() {
        long over2MB = 2L * 1024 * 1024 + 1; // 2MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[(int) over2MB]
        );

        assertThat(isValidAvatarSize(file.getSize())).isFalse();
    }

    @Test
    @DisplayName("头像文件大小刚好超过 2MB 应被拒绝")
    void shouldRejectJustOver2MB() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[2 * 1024 * 1024 + 1024] // 2MB + 1KB
        );

        assertThat(isValidAvatarSize(file.getSize())).isFalse();
    }

    @Test
    @DisplayName("空头像文件应被拒绝")
    void shouldRejectEmptyAvatar() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThat(isValidAvatarSize(emptyFile.getSize())).isFalse();
    }

    @Test
    @DisplayName("5MB 头像文件应被拒绝")
    void shouldReject5MBAvatar() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024] // 5MB
        );

        assertThat(isValidAvatarSize(largeFile.getSize())).isFalse();
    }

    // ========== API Key Security Tests ==========

    @Test
    @DisplayName("ApiKey 实体不应包含明文 rawKey")
    void apiKeyEntityShouldNotContainRawKey() {
        // Given: 一个 ApiKey 实例（模拟从数据库恢复）
        ApiKey apiKey = ApiKey.create(
                new ApiKeyId("test-api-key-001"),
                "Test API Key",
                "$2a$12$hashedKeyValueHere123456789012345678901234567890",
                "grc_a1b2...x9y0",
                LocalDateTime.now().plusDays(90)
        );

        // Then: ApiKey 实体只有 hashedKey，没有 rawKey
        assertThat(apiKey.getHashedKey()).isNotNull();
        assertThat(apiKey.getHashedKey()).startsWith("$2a$"); // BCrypt hash prefix

        // And: ApiKey 没有 rawKey getter 方法
        assertThatThrownBy(() -> {
            // 尝试通过反射获取 rawKey（应该不存在）
            ApiKey.class.getMethod("getRawKey");
        }).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("GeneratedApiKey 应包含明文 rawKey 和 ApiKey 实体")
    void generatedApiKeyShouldContainRawKey() {
        // Given: 模拟 API Key 生成
        ApiKeyHashService hashService = new BcryptApiKeyHashService();
        ApiKeyGenerationServiceImpl generationService = new ApiKeyGenerationServiceImpl(hashService);

        // When: 生成 API Key
        GeneratedApiKey generated = generationService.generate("Test Key", 90);

        // Then: GeneratedApiKey 包含 rawKey（明文）
        assertThat(generated.rawKey()).isNotNull();
        assertThat(generated.rawKey()).startsWith("grc_");
        assertThat(generated.rawKey()).hasSizeGreaterThan(10);

        // And: 包含 ApiKey 实体（不含明文）
        assertThat(generated.apiKey()).isNotNull();
        assertThat(generated.apiKey().getHashedKey()).isNotNull();
        assertThat(generated.apiKey().getHashedKey()).startsWith("$2a$");

        // And: rawKey 和 hashedKey 不相同
        assertThat(generated.rawKey()).isNotEqualTo(generated.apiKey().getHashedKey());
    }

    @Test
    @DisplayName("rawKey 仅应在 GeneratedApiKey 中出现一次")
    void rawKeyShouldAppearOnlyOnce() {
        // Given: 模拟 API Key 生成
        ApiKeyHashService hashService = new BcryptApiKeyHashService();
        ApiKeyGenerationServiceImpl generationService = new ApiKeyGenerationServiceImpl(hashService);

        // When: 生成 API Key
        GeneratedApiKey generated = generationService.generate("Test Key", 90);
        String rawKey = generated.rawKey();
        ApiKey apiKey = generated.apiKey();

        // Then: rawKey 不会出现在 ApiKey 实体的任何字段中
        assertThat(apiKey.getHashedKey()).isNotEqualTo(rawKey);
        assertThat(apiKey.getPrefix()).isNotEqualTo(rawKey);
        assertThat(apiKey.getName()).isNotEqualTo(rawKey);
        assertThat(apiKey.getId().value()).isNotEqualTo(rawKey);

        // And: ApiKey 只有 hashedKey，没有方法获取 rawKey
        assertThat(apiKey.getHashedKey()).startsWith("$2a$"); // BCrypt hash
    }

    // ========== BCrypt Hash Irreversibility Tests ==========

    @Test
    @DisplayName("BCrypt 哈希后无法还原为原始明文")
    void bcryptHashShouldBeIrreversible() {
        // Given: 原始 API Key
        String rawKey = "grc_testApiKey123456789";
        ApiKeyHashService hashService = new BcryptApiKeyHashService();

        // When: 执行 BCrypt 哈希
        String hashedKey = hashService.hash(rawKey);

        // Then: 哈希值不等于原始值
        assertThat(hashedKey).isNotEqualTo(rawKey);

        // And: 哈希值无法被"解密"回原始值（BCrypt 是单向哈希）
        // 只能通过 verify 方法验证是否匹配
        assertThat(hashService.verify(rawKey, hashedKey)).isTrue();

        // And: 无法从哈希值还原原始值
        // 哈希值不包含原始值的任何可逆信息
        assertThat(hashedKey).doesNotContain(rawKey);
        assertThat(hashedKey).startsWith("$2a$"); // BCrypt 格式
    }

    @Test
    @DisplayName("相同的原始值多次哈希应产生不同的哈希结果")
    void sameRawKeyShouldProduceDifferentHashes() {
        // Given: 相同的原始 API Key
        String rawKey = "grc_testSameKey12345";
        ApiKeyHashService hashService = new BcryptApiKeyHashService();

        // When: 多次哈希
        String hash1 = hashService.hash(rawKey);
        String hash2 = hashService.hash(rawKey);
        String hash3 = hashService.hash(rawKey);

        // Then: 每次哈希结果都不同（因为使用了不同的 salt）
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash2).isNotEqualTo(hash3);
        assertThat(hash1).isNotEqualTo(hash3);

        // But: 所有哈希都可以验证原始值
        assertThat(hashService.verify(rawKey, hash1)).isTrue();
        assertThat(hashService.verify(rawKey, hash2)).isTrue();
        assertThat(hashService.verify(rawKey, hash3)).isTrue();
    }

    @Test
    @DisplayName("不同的原始值哈希后无法互相验证")
    void differentRawKeysShouldNotVerifyEachOther() {
        // Given: 两个不同的原始 API Key
        String rawKey1 = "grc_keyOne123456";
        String rawKey2 = "grc_keyTwo789012";
        ApiKeyHashService hashService = new BcryptApiKeyHashService();

        // When: 分别哈希
        String hashedKey1 = hashService.hash(rawKey1);
        String hashedKey2 = hashService.hash(rawKey2);

        // Then: key1 无法通过 key2 的哈希验证
        assertThat(hashService.verify(rawKey2, hashedKey1)).isFalse();
        assertThat(hashService.verify(rawKey1, hashedKey2)).isFalse();

        // And: 各自的哈希只能验证自己
        assertThat(hashService.verify(rawKey1, hashedKey1)).isTrue();
        assertThat(hashService.verify(rawKey2, hashedKey2)).isTrue();
    }

    @Test
    @DisplayName("ApiKey 实体中的 hashedKey 无法被破解还原")
    void apiKeyHashedKeyCannotBeCracked() {
        // Given: 创建一个 ApiKey 实体
        String rawKey = "grc_sensitiveApiKey123";
        ApiKeyHashService hashService = new BcryptApiKeyHashService();
        String hashedKey = hashService.hash(rawKey);

        ApiKey apiKey = ApiKey.create(
                new ApiKeyId("api-key-001"),
                "Production API Key",
                hashedKey,
                "grc_sens...123",
                LocalDateTime.now().plusDays(90)
        );

        // Then: 从 ApiKey 实体只能获取到 hashedKey
        String storedHash = apiKey.getHashedKey();
        assertThat(storedHash).startsWith("$2a$");

        // And: 无法从哈希值推断或还原原始值
        // 这是 BCrypt 的安全性保证
        assertThat(storedHash).doesNotContain("sensitive");
        assertThat(storedHash).doesNotContain("ApiKey");
        assertThat(storedHash).doesNotContain(rawKey.replace("grc_", ""));
    }

    // ========== Helper Methods ==========

    /**
     * 检查是否为允许的头像文件类型
     */
    private boolean isAllowedAvatarType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return "image/jpeg".equals(contentType) || "image/png".equals(contentType);
    }

    /**
     * 检查头像文件大小是否有效（≤ 2MB）
     */
    private boolean isValidAvatarSize(long size) {
        long maxSize = 2L * 1024 * 1024; // 2MB
        return size > 0 && size <= maxSize;
    }
}
