package com.meng.lovespace.user.upload;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * 校验相册图片 URL 是否指向「当前用户」名下的 {@code albums/日期/userId/...} 对象键，防止盗链登记。
 */
public final class AlbumImageUrlValidator {

    private AlbumImageUrlValidator() {}

    /**
     * @param imageUrl 相对 {@code /local-files/...} 或绝对 URL
     * @param userId 当前用户
     */
    public static boolean isAllowedAlbumUrl(String imageUrl, String userId) {
        if (imageUrl == null || imageUrl.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        String path = extractPath(imageUrl.trim());
        if (path == null) {
            return false;
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        int idx = path.indexOf("albums/");
        if (idx < 0) {
            return false;
        }
        String tail = path.substring(idx);
        Pattern p = Pattern.compile(
                "^albums/\\d{4}-\\d{2}-\\d{2}/" + Pattern.quote(userId) + "/[^/]+$");
        return p.matcher(tail).matches();
    }

    private static String extractPath(String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            try {
                URI u = URI.create(imageUrl);
                return u.getPath();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (imageUrl.startsWith("/local-files/")) {
            return imageUrl.substring("/local-files/".length());
        }
        return imageUrl;
    }
}
