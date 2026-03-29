package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.domain.ChunkMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 分片合并服务实现。
 * <p>
 * 将临时目录中的分片文件按索引顺序合并为完整视频文件。
 * 使用 FileChannel 进行高效文件合并。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Component
public class ChunkMergeServiceImpl implements ChunkMergeService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMergeServiceImpl.class);

    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    /**
     * 合并分片文件为完整视频文件。
     * <p>
     * 按分片索引 (0, 1, 2, ...) 顺序读取分片文件，使用 FileChannel 合并为目标文件。
     * 合并完成后删除临时分片文件。
     *
     * @param tempDirectory  临时分片存储目录
     * @param totalChunks    总分片数
     * @param targetFileName 目标文件名
     * @return 合并后的视频文件路径
     * @throws InfrastructureException 如果目录不存在、分片缺失或合并失败
     */
    @Override
    public Path mergeChunks(Path tempDirectory, int totalChunks, String targetFileName) {
        validateTempDirectory(tempDirectory);
        validateChunks(tempDirectory, totalChunks);

        Path targetFile = tempDirectory.resolve(targetFileName);

        logger.info("Starting chunk merge: {} chunks into {}", totalChunks, targetFile);

        try (FileChannel targetChannel = FileChannel.open(targetFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = tempDirectory.resolve("chunk_" + i);

            if (!Files.exists(chunkFile)) {
                logger.error("Chunk file not found: {}", chunkFile);
                throw new FileOperationException("Chunk file not found: " + chunkFile);
            }

                appendChunk(targetChannel, chunkFile, buffer);
                logger.debug("Merged chunk {}: {}", i, chunkFile);
            }

            targetChannel.force(true);

            long mergedSize = Files.size(targetFile);
            logger.info("Chunk merge completed: {} (size: {} bytes)", targetFile, mergedSize);

            deleteChunks(tempDirectory, totalChunks);
            logger.debug("Temporary chunks cleaned up for: {}", tempDirectory);

            return targetFile;

        } catch (IOException e) {
            logger.error("Failed to merge chunks to: {}", targetFile, e);
            throw new FileOperationException("Failed to merge chunks: " + e.getMessage(), e);
        }
    }

    /**
     * 验证临时目录是否存在且为目录。
     *
     * @param tempDirectory 临时目录
     * @throws InfrastructureException 如果目录无效
     */
    private void validateTempDirectory(Path tempDirectory) {
        if (!Files.exists(tempDirectory)) {
            logger.error("Temp directory does not exist: {}", tempDirectory);
            throw new FileOperationException("Temp directory does not exist: " + tempDirectory);
        }
        if (!Files.isDirectory(tempDirectory)) {
            logger.error("Path is not a directory: {}", tempDirectory);
            throw new FileOperationException("Path is not a directory: " + tempDirectory);
        }
    }

    /**
     * 验证所有分片文件是否存在。
     *
     * @param tempDirectory 临时目录
     * @param totalChunks   总分片数
     * @throws InfrastructureException 如果有分片缺失
     */
    private void validateChunks(Path tempDirectory, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            Path chunkFile = tempDirectory.resolve("chunk_" + i);
            if (!Files.exists(chunkFile)) {
                logger.error("Missing chunk file: {}", chunkFile);
                throw new FileOperationException("Missing chunk file: " + chunkFile);
            }
        }
        logger.debug("All {} chunks validated in {}", totalChunks, tempDirectory);
    }

    /**
     * 将单个分片文件追加到目标通道。
     *
     * @param targetChannel 目标文件通道
     * @param chunkFile     分片文件路径
     * @param buffer        缓冲区
     * @throws IOException 如果读写失败
     */
    private void appendChunk(FileChannel targetChannel, Path chunkFile, ByteBuffer buffer) throws IOException {
        try (FileChannel sourceChannel = FileChannel.open(chunkFile, StandardOpenOption.READ)) {
            long position = targetChannel.size();

            buffer.clear();
            int bytesRead;
            while ((bytesRead = sourceChannel.read(buffer)) > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    targetChannel.write(buffer);
                }
                buffer.clear();
            }

            if (bytesRead < 0) {
                logger.debug("Chunk {} appended at offset {}", chunkFile.getFileName(), position);
            }
        }
    }

    /**
     * 删除临时分片文件。
     *
     * @param tempDirectory 临时目录
     * @param totalChunks   总分片数
     */
    private void deleteChunks(Path tempDirectory, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            Path chunkFile = tempDirectory.resolve("chunk_" + i);
            try {
                Files.deleteIfExists(chunkFile);
            } catch (IOException e) {
                logger.warn("Failed to delete chunk file: {}", chunkFile, e);
            }
        }
    }
}
