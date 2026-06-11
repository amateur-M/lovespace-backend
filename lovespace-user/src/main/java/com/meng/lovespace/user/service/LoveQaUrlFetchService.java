package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 恋爱知识库 URL 抓取：Jsoup 正文提取 + SSRF 防护 + 大小/超时限制。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaUrlFetchService {

    private static final int BAD_REQUEST = 40093;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final RagAiProperties ragAiProperties;

    /**
     * 抓取 URL 并提取可读正文。
     *
     * @param urlString 公网 http(s) URL
     * @return 清洗后的纯文本
     */
    public String fetchText(String urlString) {
        if (!StringUtils.hasText(urlString)) {
            throw new LoveQaBusinessException(BAD_REQUEST, "sourceUrl 不能为空");
        }
        String trimmed = urlString.trim();
        URI uri = parseAndValidateUri(trimmed);
        assertPublicHost(uri);

        int timeoutMs = Math.max(3, ragAiProperties.getIngestUrlTimeoutSeconds()) * 1000;
        int maxBytes = Math.max(64 * 1024, ragAiProperties.getIngestUrlMaxBytes());

        try {
            Document doc =
                    Jsoup.connect(trimmed)
                            .userAgent("LoveSpace-RAG/1.0")
                            .timeout(timeoutMs)
                            .maxBodySize(maxBytes)
                            .ignoreContentType(true)
                            .followRedirects(true)
                            .get();
            doc.select("script, style, nav, footer, header, aside, noscript, iframe").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            text = text.replaceAll("\\s+", " ").trim();
            if (!StringUtils.hasText(text)) {
                throw new LoveQaBusinessException(BAD_REQUEST, "无法从 URL 提取有效正文");
            }
            log.info("love-qa url fetch ok host={} textLen={}", uri.getHost(), text.length());
            return text;
        } catch (LoveQaBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("love-qa url fetch failed url={}", trimmed, e);
            throw new LoveQaBusinessException(BAD_REQUEST, "URL 抓取失败：" + safeMessage(e));
        }
    }

    private static URI parseAndValidateUri(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new LoveQaBusinessException(BAD_REQUEST, "URL 格式无效");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new LoveQaBusinessException(BAD_REQUEST, "仅支持 http/https URL");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new LoveQaBusinessException(BAD_REQUEST, "URL 缺少主机名");
        }
        if (StringUtils.hasText(uri.getUserInfo())) {
            throw new LoveQaBusinessException(BAD_REQUEST, "URL 不允许包含用户信息");
        }
        return uri;
    }

    private static void assertPublicHost(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if ("localhost".equals(host) || host.endsWith(".local") || host.endsWith(".internal")) {
            throw new LoveQaBusinessException(BAD_REQUEST, "不允许访问内网或本地地址");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            for (InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || isMetadataHost(host)) {
                    throw new LoveQaBusinessException(BAD_REQUEST, "不允许访问内网或本地地址");
                }
            }
        } catch (UnknownHostException e) {
            throw new LoveQaBusinessException(BAD_REQUEST, "无法解析 URL 主机名");
        }
    }

    private static boolean isMetadataHost(String host) {
        return "metadata.google.internal".equals(host)
                || "169.254.169.254".equals(host);
    }

    private static String safeMessage(Exception e) {
        String msg = e.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }
}
