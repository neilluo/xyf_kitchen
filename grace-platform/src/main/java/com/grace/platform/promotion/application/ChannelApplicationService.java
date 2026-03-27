package com.grace.platform.promotion.application;

import com.grace.platform.promotion.application.command.CreateChannelCommand;
import com.grace.platform.promotion.application.command.UpdateChannelCommand;
import com.grace.platform.promotion.application.dto.ChannelDTO;
import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.PromotionChannel;
import com.grace.platform.promotion.domain.PromotionChannelRepository;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道应用服务
 * <p>
 * 负责编排渠道管理相关的用例流程，包括：
 * <ul>
 *   <li>创建渠道（支持 API Key 加密存储）</li>
 *   <li>更新渠道配置</li>
 *   <li>删除渠道（软删除/硬删除策略）</li>
 *   <li>查询渠道列表</li>
 *   <li>获取单个渠道详情</li>
 * </ul>
 * </p>
 */
@Service
@Transactional
public class ChannelApplicationService {

    private final PromotionChannelRepository channelRepository;
    private final EncryptionService encryptionService;

    /**
     * 创建渠道应用服务
     *
     * @param channelRepository  渠道仓储
     * @param encryptionService  加密服务
     */
    public ChannelApplicationService(
            PromotionChannelRepository channelRepository,
            EncryptionService encryptionService) {
        this.channelRepository = channelRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * 创建推广渠道
     * <p>
     * 流程：
     * 1. 构建 Channel 实体
     * 2. 如有 API Key 则加密存储
     * 3. 保存到数据库
     * </p>
     *
     * @param command 创建渠道命令
     * @return 创建的渠道 DTO
     * @throws IllegalArgumentException 当参数校验失败时
     */
    public ChannelDTO createChannel(CreateChannelCommand command) {
        // 1. 构建 Channel 实体
        PromotionChannel channel = PromotionChannel.create(
                command.name(),
                command.type(),
                command.channelUrl(),
                command.priorityOrDefault()
        );

        // 2. 如有 API Key 则加密存储
        if (command.apiKey() != null && !command.apiKey().isBlank()) {
            channel.setApiKey(command.apiKey(), encryptionService);
        }

        // 3. 保存到数据库
        channel = channelRepository.save(channel);

        return toDto(channel);
    }

    /**
     * 更新推广渠道
     * <p>
     * 流程：
     * 1. 查询渠道
     * 2. 更新字段（支持部分更新）
     * 3. 如有新 API Key 则重新加密
     * 4. 保存到数据库
     * </p>
     *
     * @param id      渠道 ID
     * @param command 更新渠道命令
     * @return 更新后的渠道 DTO
     * @throws EntityNotFoundException 当渠道不存在时
     */
    public ChannelDTO updateChannel(ChannelId id, UpdateChannelCommand command) {
        // 1. 查询渠道
        PromotionChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CHANNEL_NOT_FOUND,
                        "PromotionChannel",
                        id.value()
                ));

        // 2. 更新字段（支持部分更新）
        channel.updateInfo(
                command.name(),
                command.type(),
                command.channelUrl(),
                command.priority()
        );

        // 3. 如有新 API Key 则重新加密
        if (command.apiKey() != null && !command.apiKey().isBlank()) {
            channel.setApiKey(command.apiKey(), encryptionService);
        }

        // 4. 处理状态变更（启用/禁用）
        if (command.status() != null) {
            if (command.status() == ChannelStatus.ENABLED) {
                channel.enable();
            } else if (command.status() == ChannelStatus.DISABLED) {
                channel.disable();
            }
        }

        // 5. 保存到数据库
        channel = channelRepository.save(channel);

        return toDto(channel);
    }

    /**
     * 删除推广渠道
     * <p>
     * 删除策略：
     * - 如果该渠道有关联的推广记录，则执行软删除（标记为 DISABLED）
     * - 如果没有关联记录，则执行硬删除
     * </p>
     *
     * @param id 渠道 ID
     * @throws EntityNotFoundException 当渠道不存在时
     */
    public void deleteChannel(ChannelId id) {
        // 1. 查询渠道
        PromotionChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CHANNEL_NOT_FOUND,
                        "PromotionChannel",
                        id.value()
                ));

        // 2. 检查是否有关联的推广记录
        boolean hasPromotionRecords = channelRepository.existsPromotionRecordByChannelId(id);

        if (hasPromotionRecords) {
            // 软删除：标记为 DISABLED
            channel.disable();
            channelRepository.save(channel);
        } else {
            // 硬删除
            channelRepository.deleteById(id);
        }
    }

    /**
     * 获取所有推广渠道列表
     * <p>
     * 支持按状态筛选。
     * </p>
     *
     * @param status 状态筛选条件（可选，null 表示返回所有）
     * @return 渠道 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<ChannelDTO> listChannels(ChannelStatus status) {
        List<PromotionChannel> channels;
        
        if (status != null) {
            channels = channelRepository.findByStatus(status);
        } else {
            channels = channelRepository.findAll();
        }

        return channels.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 获取单个渠道详情
     *
     * @param id 渠道 ID
     * @return 渠道 DTO
     * @throws EntityNotFoundException 当渠道不存在时
     */
    @Transactional(readOnly = true)
    public ChannelDTO getChannel(ChannelId id) {
        PromotionChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CHANNEL_NOT_FOUND,
                        "PromotionChannel",
                        id.value()
                ));

        return toDto(channel);
    }

    /**
     * 将领域实体转换为 DTO
     *
     * @param channel 渠道实体
     * @return 渠道 DTO
     */
    private ChannelDTO toDto(PromotionChannel channel) {
        return new ChannelDTO(
                channel.getId().value(),
                channel.getName(),
                channel.getType(),
                channel.getChannelUrl(),
                channel.getEncryptedApiKey() != null && !channel.getEncryptedApiKey().isBlank(),
                channel.getPriority(),
                channel.getStatus(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
