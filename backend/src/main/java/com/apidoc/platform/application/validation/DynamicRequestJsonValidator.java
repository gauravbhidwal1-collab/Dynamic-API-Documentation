package com.apidoc.platform.application.validation;

import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DynamicRequestJsonValidator {

    private final ObjectMapper objectMapper;

    /**
     * Validates payload against the API's request field tree, applies defaults, returns a deep copy safe to mutate.
     */
    public ObjectNode validateAndMergeDefaults(ApiMaster master, JsonNode payload) {
        if (!payload.isObject()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Request JSON must be an object at the root");
        }
        ObjectNode root = payload.deepCopy();
        List<ApiField> roots = master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        List<String> errors = new ArrayList<String>();
        for (ApiField field : roots) {
            validateField(field, root, "", errors);
        }
        if (!errors.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }
        return root;
    }

    private void validateField(ApiField field, ObjectNode parentObject, String pathPrefix, List<String> errors) {
        String path = pathPrefix + field.getFieldKey();
        JsonNode value = parentObject.get(field.getFieldKey());
        boolean missing = !parentObject.has(field.getFieldKey()) || value == null || value.isNull();

        if (missing) {
            if (StringUtils.hasText(field.getDefaultValue())) {
                JsonNode def = parseDefault(field.getDefaultValue());
                parentObject.set(field.getFieldKey(), def);
                value = parentObject.get(field.getFieldKey());
                missing = false;
            }
        }

        if (missing) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                errors.add("Missing required field: " + path);
            }
            return;
        }

        String type = field.getDataType() == null ? "STRING" : field.getDataType().trim().toUpperCase(Locale.ROOT);
        if (!typeMatches(type, value)) {
            errors.add("Invalid type for " + path + ": expected " + type + " but was " + value.getNodeType());
            return;
        }

        if ("OBJECT".equals(type)) {
            if (!value.isObject()) {
                return;
            }
            ObjectNode childObj = (ObjectNode) value;
            List<ApiField> children = field.getChildFields().stream()
                    .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toList());
            for (ApiField child : children) {
                validateField(child, childObj, path + ".", errors);
            }
        } else if ("ARRAY".equals(type)) {
            if (value.isArray()) {
                validateArrayElements(field, (ArrayNode) value, path, errors);
            }
        }
    }

    private void validateArrayElements(ApiField field, ArrayNode array, String path, List<String> errors) {
        List<ApiField> children = field.getChildFields().stream()
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (children.isEmpty()) {
            return;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonNode el = array.get(i);
            String elPath = path + "[" + i + "]";
            if (!el.isObject()) {
                errors.add("Array element must be object at " + elPath);
                continue;
            }
            ObjectNode elObj = (ObjectNode) el;
            for (ApiField child : children) {
                validateField(child, elObj, elPath + ".", errors);
            }
        }
    }

    private JsonNode parseDefault(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(raw);
        }
    }

    private boolean typeMatches(String type, JsonNode value) {
        if ("STRING".equals(type) || "TEXT".equals(type)) {
            return value.isTextual();
        }
        if ("NUMBER".equals(type) || "DOUBLE".equals(type) || "FLOAT".equals(type)) {
            return value.isNumber();
        }
        if ("INTEGER".equals(type) || "INT".equals(type) || "LONG".equals(type)) {
            return value.isIntegralNumber();
        }
        if ("BOOLEAN".equals(type) || "BOOL".equals(type)) {
            return value.isBoolean();
        }
        if ("OBJECT".equals(type)) {
            return value.isObject();
        }
        if ("ARRAY".equals(type)) {
            return value.isArray();
        }
        return true;
    }
}
