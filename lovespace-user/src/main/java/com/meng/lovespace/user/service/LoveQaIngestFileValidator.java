package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 恋爱知识库文件入库校验：扩展名白名单、大小上限、UTF-8 编码。 */
@Component
@RequiredArgsConstructor
public class LoveQaIngestFileValidator {

    private static final int BAD_REQUEST = 40093;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "markdown");

    private final RagAiProperties ragAiProperties;

    /**
     * 校验并读取 UTF-8 文本。
     *
     * @return 文件正文
     */
    public String readValidatedUtf8Text(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LoveQaBusinessException(BAD_REQUEST, "请选择要上传的文件");
        }
        validateExtension(file.getOriginalFilename());
        long maxBytes = Math.max(1024, ragAiProperties.getIngestMaxFileBytes());
        if (file.getSize() > maxBytes) {
            throw new LoveQaBusinessException(
                    BAD_REQUEST, "文件过大，上限 " + (maxBytes / 1024 / 1024) + "MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文件读取失败");
        }
        if (bytes.length > maxBytes) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文件过大");
        }
        decodeUtf8Strict(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void validateExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文件名无效");
        }
        String lower = filename.trim().toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1) {
            throw new LoveQaBusinessException(BAD_REQUEST, "仅支持 .txt、.md 文本文件");
        }
        String ext = lower.substring(dot + 1);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new LoveQaBusinessException(BAD_REQUEST, "仅支持 .txt、.md 文本文件");
        }
    }

    private static void decodeUtf8Strict(byte[] bytes) {
        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer ignored = decoder.decode(ByteBuffer.wrap(bytes));
            ignored.clear();
        } catch (CharacterCodingException e) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文件须为 UTF-8 编码");
        }
    }
}
