package com.apidoc.platform.application.execution;

import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class DynamicApiUrlBuilder {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    public String buildRequestUrl(ApiMaster master, ObjectNode payload, HttpMethod method) {
        String base = trimTrailingSlash(master.getBaseUrl().trim());
        String pathTemplate = StringUtils.hasText(master.getPathTemplate()) ? master.getPathTemplate().trim() : "";
        if (!pathTemplate.isEmpty() && !pathTemplate.startsWith("/")) {
            pathTemplate = "/" + pathTemplate;
        }
        String resolvedPath = resolvePathPlaceholders(pathTemplate, payload);
        String url = base + resolvedPath;

        if (method == HttpMethod.GET || method == HttpMethod.DELETE || method == HttpMethod.HEAD) {
            Set<String> usedKeys = extractPlaceholderKeys(master.getPathTemplate());
            String query = buildQueryString(payload, usedKeys);
            if (StringUtils.hasText(query)) {
                url += (url.contains("?") ? "&" : "?") + query;
            }
        }
        return url;
    }

    private Set<String> extractPlaceholderKeys(String template) {
        Set<String> keys = new HashSet<>();
        if (!StringUtils.hasText(template)) {
            return keys;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            keys.add(m.group(1).trim().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private String resolvePathPlaceholders(String pathTemplate, ObjectNode payload) {
        if (!StringUtils.hasText(pathTemplate)) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(pathTemplate);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            String encoded = urlEncode(readScalarAsString(payload, key));
            if (encoded == null) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST, "Missing or null value for path placeholder: {" + key + "}");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(encoded));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String buildQueryString(ObjectNode payload, Set<String> placeholderKeysLower) {
        StringBuilder q = new StringBuilder();
        payload.fieldNames().forEachRemaining(name -> {
            if (placeholderKeysLower.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            JsonNode v = payload.get(name);
            if (v == null || v.isNull() || v.isObject() || v.isArray()) {
                return;
            }
            appendParam(q, name, v.asText());
        });
        return q.toString();
    }

    private void appendParam(StringBuilder q, String name, String value) {
        if (q.length() > 0) {
            q.append('&');
        }
        q.append(urlEncode(name)).append('=').append(urlEncode(value));
    }

    private String urlEncode(String raw) {
        try {
            return URLEncoder.encode(raw, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Reads dotted path (e.g. {@code user.id}) from root object.
     */
    private String readScalarAsString(ObjectNode root, String dottedKey) {
        String[] parts = dottedKey.split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            if (node == null || !node.isObject()) {
                return null;
            }
            node = node.get(part);
        }
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() || node.isArray()) {
            return node.toString();
        }
        return node.asText();
    }

    private String trimTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** Validates URL shape; throws if invalid. */
    public void validateAsUri(String url) {
        try {
            UriComponentsBuilder.fromUriString(url).build(true);
        } catch (Exception ex) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Invalid resolved URL: " + ex.getMessage());
        }
    }
}
