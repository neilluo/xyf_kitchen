package com.grace.platform.metadata.infrastructure.llm;

/**
 * LLM服务通用接口，定义与大型语言模型交互的契约。
 * 可被Metadata和Promotion两个上下文共用。
 */
public interface LlmService {

    /**
     * 调用LLM完成请求，返回生成的内容。
     *
     * @param request LLM请求参数
     * @return LLM响应结果
     */
    LlmResponse complete(LlmRequest request);
}
