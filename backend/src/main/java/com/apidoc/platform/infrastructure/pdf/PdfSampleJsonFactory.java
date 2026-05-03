package com.apidoc.platform.infrastructure.pdf;

import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.apidoc.platform.infrastructure.persistence.entity.ResponseKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Builds illustrative JSON strings for PDF documentation (pretty-printed).
 */
public final class PdfSampleJsonFactory {

    private PdfSampleJsonFactory() {}

    public static String prettyRequestSample(ApiMaster master, ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        List<ApiField> roots = master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        for (ApiField field : roots) {
            JsonNode v = requestFieldToJson(field, mapper);
            if (v != null) {
                root.set(field.getFieldKey(), v);
            }
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** @deprecated use {@link #prettySuccessResponseSample(ApiMaster, ObjectMapper)} */
    @Deprecated
    public static String prettyResponseSample(ApiMaster master, ObjectMapper mapper) {
        return prettySuccessResponseSample(master, mapper);
    }

    public static String prettySuccessResponseSample(ApiMaster master, ObjectMapper mapper) {
        return prettyResponseSampleForKind(master, mapper, true);
    }

    public static String prettyFailureResponseSample(ApiMaster master, ObjectMapper mapper) {
        return prettyResponseSampleForKind(master, mapper, false);
    }

    private static String prettyResponseSampleForKind(ApiMaster master, ObjectMapper mapper, boolean success) {
        ObjectNode root = mapper.createObjectNode();
        List<ApiResponseField> roots =
                master.getResponseFields().stream()
                        .filter(
                                f -> {
                                    if (f.getParent() != null) {
                                        return false;
                                    }
                                    if (success) {
                                        return isSuccessKind(f.getResponseKind());
                                    }
                                    return f.getResponseKind() == ResponseKind.FAILURE;
                                })
                        .sorted(Comparator.comparing(ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .collect(Collectors.toList());
        for (ApiResponseField field : roots) {
            JsonNode v = responseFieldToJson(field, mapper);
            if (v != null) {
                root.set(field.getFieldKey(), v);
            }
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static boolean isSuccessKind(ResponseKind k) {
        return k == null || k == ResponseKind.SUCCESS;
    }

    private static JsonNode requestFieldToJson(ApiField field, ObjectMapper mapper) {
        if (StringUtils.hasText(field.getDefaultValue())) {
            try {
                return mapper.readTree(field.getDefaultValue());
            } catch (Exception ignored) {
                return mapper.getNodeFactory().textNode(field.getDefaultValue());
            }
        }
        String type = field.getDataType() == null ? "STRING" : field.getDataType().trim().toUpperCase(Locale.ROOT);
        List<ApiField> children = sortedRequestChildren(field);

        if ("OBJECT".equals(type)) {
            ObjectNode obj = mapper.createObjectNode();
            for (ApiField c : children) {
                JsonNode v = requestFieldToJson(c, mapper);
                if (v != null) {
                    obj.set(c.getFieldKey(), v);
                }
            }
            return obj;
        }
        if ("ARRAY".equals(type)) {
            ArrayNode arr = mapper.createArrayNode();
            if (!children.isEmpty()) {
                ObjectNode el = mapper.createObjectNode();
                for (ApiField c : children) {
                    JsonNode v = requestFieldToJson(c, mapper);
                    if (v != null) {
                        el.set(c.getFieldKey(), v);
                    }
                }
                arr.add(el);
            }
            return arr;
        }
        return requestScalar(type, mapper);
    }

    private static JsonNode responseFieldToJson(ApiResponseField field, ObjectMapper mapper) {
        String type = field.getDataType() == null ? "STRING" : field.getDataType().trim().toUpperCase(Locale.ROOT);
        List<ApiResponseField> children = sortedResponseChildren(field);

        if ("OBJECT".equals(type)) {
            ObjectNode obj = mapper.createObjectNode();
            for (ApiResponseField c : children) {
                JsonNode v = responseFieldToJson(c, mapper);
                if (v != null) {
                    obj.set(c.getFieldKey(), v);
                }
            }
            return obj;
        }
        if ("ARRAY".equals(type)) {
            ArrayNode arr = mapper.createArrayNode();
            if (!children.isEmpty()) {
                ObjectNode el = mapper.createObjectNode();
                for (ApiResponseField c : children) {
                    JsonNode v = responseFieldToJson(c, mapper);
                    if (v != null) {
                        el.set(c.getFieldKey(), v);
                    }
                }
                arr.add(el);
            }
            return arr;
        }
        return responseScalar(type, mapper);
    }

    private static JsonNode requestScalar(String type, ObjectMapper mapper) {
        if ("STRING".equals(type) || "TEXT".equals(type)) {
            return mapper.getNodeFactory().textNode("string");
        }
        if ("NUMBER".equals(type) || "DOUBLE".equals(type) || "FLOAT".equals(type)) {
            return mapper.getNodeFactory().numberNode(0);
        }
        if ("INTEGER".equals(type) || "INT".equals(type) || "LONG".equals(type)) {
            return mapper.getNodeFactory().numberNode(0);
        }
        if ("BOOLEAN".equals(type) || "BOOL".equals(type)) {
            return mapper.getNodeFactory().booleanNode(false);
        }
        return mapper.getNodeFactory().textNode("…");
    }

    private static JsonNode responseScalar(String type, ObjectMapper mapper) {
        return requestScalar(type, mapper);
    }

    private static List<ApiField> sortedRequestChildren(ApiField field) {
        return field.getChildFields().stream()
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private static List<ApiResponseField> sortedResponseChildren(ApiResponseField field) {
        return field.getChildFields().stream()
                .sorted(Comparator.comparing(ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }
}
