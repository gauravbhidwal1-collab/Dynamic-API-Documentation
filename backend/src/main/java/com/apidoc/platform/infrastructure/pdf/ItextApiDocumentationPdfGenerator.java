package com.apidoc.platform.infrastructure.pdf;

import com.apidoc.platform.application.dto.DocumentedHttpHeaderDto;
import com.apidoc.platform.application.dto.FailureValidationRuleDto;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.apidoc.platform.infrastructure.persistence.entity.ResponseKind;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Generates API PDF documentation in a layout similar to typical ABFL-style specs:
 * purpose narrative, optional sequenced activities / notes / impact, attributes table, input details,
 * standard and documented headers, request/response body tables with illustrative JSON.
 */
@Component
@RequiredArgsConstructor
public class ItextApiDocumentationPdfGenerator {

    private static final SolidBorder CELL_BORDER = new SolidBorder(new DeviceRgb(200, 210, 230), 0.45f);

    private static final DeviceRgb ACCENT_BLUE = new DeviceRgb(25, 118, 210);
    private static final DeviceRgb ACCENT_DARK = new DeviceRgb(13, 71, 161);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(187, 222, 251);
    private static final DeviceRgb HEADER_FG = new DeviceRgb(15, 40, 80);
    private static final DeviceRgb TITLE_BAR = new DeviceRgb(33, 150, 243);
    private static final DeviceRgb ZEBRA = new DeviceRgb(248, 250, 255);
    private static final DeviceRgb REQ_JSON_BG = new DeviceRgb(232, 245, 233);
    private static final DeviceRgb RES_JSON_BG = new DeviceRgb(255, 243, 224);
    private static final DeviceRgb ERR_JSON_BG = new DeviceRgb(255, 235, 238);

    private final ObjectMapper objectMapper;

    private int zebraCounter;

    /** Scaled per {@link #generate(ApiMaster, boolean)} — guarded by the same lock as generation. */
    private float tableFont = 9f;

    private float sectionFont = 11f;
    private float titleFont = 18f;
    private float bodyFont = 10f;
    private float smallFont = 8f;
    private float captionFont = 9f;

    /**
     * @param compact when true, typography is slightly tighter (better for on-screen preview).
     */
    public byte[] generate(ApiMaster master, boolean compact) throws IOException {
        synchronized (this) {
            float m = compact ? 0.88f : 1f;
            tableFont = 9f * m;
            sectionFont = 11f * m;
            titleFont = 18f * m;
            bodyFont = 10f * m;
            smallFont = 8f * m;
            captionFont = 9f * m;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            ItextRichTextBlockRenderer.Fonts richFonts = ItextRichTextBlockRenderer.Fonts.create();
            PdfFont normal = richFonts.normal;
            PdfFont bold = richFonts.bold;
            PdfFont courier = PdfFontFactory.createFont(StandardFonts.COURIER);

            List<DocumentedHttpHeaderDto> documentedHeaders = parseDocumentedHeaders(master);
            List<FailureValidationRuleDto> failureValidations = parseFailureValidations(master);

            try (Document doc = new Document(pdf, PageSize.A4)) {
                doc.setMargins(48, 48, 56, 48);
                doc.setFont(normal);

                Table titleBar = new Table(UnitValue.createPercentArray(new float[] {100f})).useAllAvailableWidth();
                Cell titleCell = new Cell()
                        .setBackgroundColor(TITLE_BAR)
                        .setPadding(14)
                        .setBorder(new SolidBorder(TITLE_BAR, 0));
                titleCell.add(new Paragraph(master.getName())
                        .setFont(bold)
                        .setFontSize(titleFont)
                        .setFontColor(ColorConstants.WHITE));
                titleBar.addCell(titleCell);
                doc.add(titleBar);

                if (StringUtils.hasText(master.getApiCode())) {
                    doc.add(new Paragraph("API name convention: " + master.getApiCode())
                            .setFont(bold)
                            .setFontSize(bodyFont)
                            .setFontColor(ACCENT_DARK)
                            .setMarginBottom(12));
                }

                doc.add(sectionHeading("Purpose"));
                addPurposeRich(doc, master.getDescription(), richFonts);

                if (StringUtils.hasText(master.getActivitiesSequenceText())) {
                    doc.add(sectionHeading("Sequenced activities"));
                    doc.add(new Paragraph(
                                    "Below are the activities this API will carry out in the mentioned sequence:")
                            .setFont(normal)
                            .setFontSize(tableFont)
                            .setMarginBottom(6));
                    ItextRichTextBlockRenderer.addRichBlock(
                            doc, master.getActivitiesSequenceText(), richFonts, tableFont);
                }

                if (StringUtils.hasText(master.getAdditionalNotesText())) {
                    doc.add(sectionHeading("Note"));
                    ItextRichTextBlockRenderer.addRichBlock(
                            doc, master.getAdditionalNotesText(), richFonts, tableFont);
                }

                if (StringUtils.hasText(master.getImpactOnSystemText())) {
                    doc.add(sectionHeading("Impact on the system"));
                    ItextRichTextBlockRenderer.addRichBlock(
                            doc, master.getImpactOnSystemText(), richFonts, tableFont);
                }

                doc.add(sectionHeading("Attributes"));
                doc.add(buildAttributesTable(master, normal, bold));

                doc.add(sectionHeading("Input details"));
                doc.add(sectionSubheading("AUTHORIZATION"));
                doc.add(new Paragraph(
                                "Authentication is not stored in this API definition. "
                                        + "Use the scheme agreed for your environment (for example Basic Auth, "
                                        + "Bearer token, or mutual TLS).")
                        .setFont(normal)
                        .setFontSize(tableFont)
                        .setMarginBottom(8));

                doc.add(sectionSubheading("STANDARD HEADERS (REFERENCE)"));
                doc.add(buildStandardHeadersTable(normal, bold));

                doc.add(sectionSubheading("DOCUMENTED API HEADERS"));
                doc.add(buildDocumentedHeadersTable(documentedHeaders, normal, bold));

                doc.add(sectionHeading("Validations (failure reason)"));
                doc.add(buildFailureValidationsTable(failureValidations, normal, bold));

                doc.add(new Paragraph("P.S.: URLs and path templates may be case-sensitive; follow your integration guide.")
                        .setFont(normal)
                        .setFontSize(smallFont)
                        .setItalic()
                        .setMarginTop(6)
                        .setMarginBottom(12));

                doc.add(sectionHeading("REQUEST BODY"));
                doc.add(buildRequestBodyTable(master, normal, bold));
                addIllustrativeJsonBlock(
                        doc,
                        "Example request JSON (illustrative)",
                        PdfSampleJsonFactory.prettyRequestSample(master, objectMapper),
                        REQ_JSON_BG,
                        normal,
                        courier);

                doc.add(sectionHeading("RESPONSE BODY (SUCCESS)"));
                doc.add(
                        buildResponseBodyTable(
                                master,
                                normal,
                                bold,
                                ResponseKind.SUCCESS,
                                "No success response fields defined for this API."));
                addIllustrativeJsonBlock(
                        doc,
                        "Example success response JSON (illustrative)",
                        PdfSampleJsonFactory.prettySuccessResponseSample(master, objectMapper),
                        RES_JSON_BG,
                        normal,
                        courier);

                doc.add(sectionHeading("RESPONSE BODY (FAILURE / ERROR)"));
                doc.add(
                        buildResponseBodyTable(
                                master,
                                normal,
                                bold,
                                ResponseKind.FAILURE,
                                "No failure / error response fields defined for this API."));
                addIllustrativeJsonBlock(
                        doc,
                        "Example failure response JSON (illustrative)",
                        PdfSampleJsonFactory.prettyFailureResponseSample(master, objectMapper),
                        ERR_JSON_BG,
                        normal,
                        courier);

                String generated = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now());
                doc.add(new Paragraph("Generated: " + generated)
                        .setFont(normal)
                        .setFontSize(smallFont)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setMarginTop(20));
            }
            return out.toByteArray();
        }
    }

    /** Same as {@link #generate(ApiMaster, boolean)} with {@code compact == false}. */
    public byte[] generate(ApiMaster master) throws IOException {
        return generate(master, false);
    }

    private List<DocumentedHttpHeaderDto> parseDocumentedHeaders(ApiMaster master) {
        String json = master.getDocumentedHeadersJson();
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DocumentedHttpHeaderDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<FailureValidationRuleDto> parseFailureValidations(ApiMaster master) {
        String json = master.getFailureValidationsJson();
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FailureValidationRuleDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void addPurposeRich(
            Document doc, String description, ItextRichTextBlockRenderer.Fonts fonts) {
        if (!StringUtils.hasText(description)) {
            doc.add(new Paragraph("—").setFont(fonts.normal).setFontSize(tableFont).setMarginBottom(8));
            return;
        }
        ItextRichTextBlockRenderer.addRichBlock(doc, description, fonts, tableFont);
    }

    private Paragraph sectionHeading(String title) {
        return new Paragraph(title.toUpperCase(Locale.ROOT) + ":")
                .setFontSize(sectionFont)
                .setBold()
                .setFontColor(ACCENT_DARK)
                .setMarginTop(14)
                .setMarginBottom(6);
    }

    private Paragraph sectionSubheading(String title) {
        return new Paragraph(title)
                .setFontSize(bodyFont)
                .setBold()
                .setFontColor(ACCENT_BLUE)
                .setMarginTop(8)
                .setMarginBottom(4);
    }

    private void addIllustrativeJsonBlock(
            Document doc,
            String caption,
            String json,
            DeviceRgb background,
            PdfFont normal,
            PdfFont courier) {
        doc.add(new Paragraph(caption)
                .setFont(normal)
                .setFontSize(captionFont)
                .setFontColor(ACCENT_DARK)
                .setBold()
                .setMarginTop(8)
                .setMarginBottom(4));
        Table wrap = new Table(UnitValue.createPercentArray(new float[] {100f})).useAllAvailableWidth();
        Cell c = new Cell().setBackgroundColor(background).setPadding(10).setBorder(CELL_BORDER);
        for (String line : json.split("\n")) {
            c.add(new Paragraph(line.isEmpty() ? " " : line)
                    .setFont(courier)
                    .setFontSize(smallFont)
                    .setFontColor(new DeviceRgb(33, 33, 33))
                    .setMargin(0));
        }
        wrap.addCell(c);
        doc.add(wrap);
    }

    private void resetZebra() {
        zebraCounter = 0;
    }

    private Color nextZebraRowBg() {
        return (zebraCounter++ % 2 == 0) ? ColorConstants.WHITE : ZEBRA;
    }

    private Table buildAttributesTable(ApiMaster master, PdfFont normal, PdfFont bold) {
        resetZebra();
        Table table = new Table(UnitValue.createPercentArray(new float[] {32f, 68f})).useAllAvailableWidth();
        addHeaderRow2(table, "Particulars", "Attributes", bold);
        addBodyRow2(table, "API code", textOrDash(master.getApiCode()), normal, bold, nextZebraRowBg());
        addBodyRow2(table, "HTTP method", textOrDash(master.getHttpMethod()), normal, bold, nextZebraRowBg());
        addBodyRow2(table, "Full endpoint URL", textOrDash(buildFullUrl(master)), normal, bold, nextZebraRowBg());
        addBodyRow2(
                table,
                "Active",
                Boolean.TRUE.equals(master.getActive()) ? "Yes" : "No",
                normal,
                bold,
                nextZebraRowBg());
        addBodyRow2(
                table,
                "API type (Synchronous / Asynchronous)",
                "— (not captured in definition)",
                normal,
                bold,
                nextZebraRowBg());
        addBodyRow2(table, "Partner specific", "— (not captured in definition)", normal, bold, nextZebraRowBg());
        return table;
    }

    private Table buildStandardHeadersTable(PdfFont normal, PdfFont bold) {
        resetZebra();
        Table table = new Table(UnitValue.createPercentArray(new float[] {22f, 38f, 40f})).useAllAvailableWidth();
        addHeaderRow3(table, "Key", "Value", "Description", bold);
        addBodyRow3(table, "Accept", "application/json", "Expected response format", normal, bold, nextZebraRowBg());
        addBodyRow3(table, "Content-Type", "application/json", "Request body format for POST/PUT/PATCH", normal, bold, nextZebraRowBg());
        addBodyRow3(
                table,
                "Authorization",
                "<credentials>",
                "Set per your auth scheme (not stored here)", normal, bold, nextZebraRowBg());
        return table;
    }

    private Table buildDocumentedHeadersTable(
            List<DocumentedHttpHeaderDto> headers, PdfFont normal, PdfFont bold) {
        resetZebra();
        Table table = new Table(UnitValue.createPercentArray(new float[] {22f, 28f, 50f})).useAllAvailableWidth();
        addHeaderRow3(table, "Header name", "Example value", "Description", bold);
        if (headers == null || headers.isEmpty()) {
            Cell c = new Cell(1, 3)
                    .add(new Paragraph(
                                    "No API-specific headers documented in the builder. Add rows under “Documented API headers” in API Builder.")
                            .setFont(normal)
                            .setFontSize(tableFont)
                            .setItalic())
                    .setPadding(6)
                    .setBorder(CELL_BORDER)
                    .setBackgroundColor(new DeviceRgb(255, 252, 245));
            table.addCell(c);
            return table;
        }
        for (DocumentedHttpHeaderDto h : headers) {
            if (h == null) {
                continue;
            }
            String k = textOrDash(h.getHeaderKey());
            String v = textOrDash(h.getHeaderValue());
            String d = textOrDash(h.getDescription());
            Color bg = nextZebraRowBg();
            addBodyRow3(table, k, v, d, normal, bold, bg);
        }
        return table;
    }

    private Table buildFailureValidationsTable(
            List<FailureValidationRuleDto> rows, PdfFont normal, PdfFont bold) {
        resetZebra();
        Table table = new Table(UnitValue.createPercentArray(new float[] {32f, 68f})).useAllAvailableWidth();
        addHeaderRow2(table, "Validation message", "Scenario", bold);
        if (rows == null || rows.isEmpty()) {
            Cell c = new Cell(1, 2)
                    .add(
                            new Paragraph(
                                            "No failure validations documented. Add rows under Validations (failure reason) in API Builder.")
                                    .setFont(normal)
                                    .setFontSize(tableFont)
                                    .setItalic())
                    .setPadding(6)
                    .setBorder(CELL_BORDER)
                    .setBackgroundColor(new DeviceRgb(255, 252, 245));
            table.addCell(c);
            return table;
        }
        for (FailureValidationRuleDto v : rows) {
            if (v == null) {
                continue;
            }
            Color bg = nextZebraRowBg();
            addBodyRow2(
                    table,
                    textOrDash(v.getValidationMessage()),
                    textOrDash(v.getScenario()),
                    normal,
                    bold,
                    bg);
        }
        return table;
    }

    private Table buildRequestBodyTable(ApiMaster master, PdfFont normal, PdfFont bold) {
        resetZebra();
        Table table =
                new Table(UnitValue.createPercentArray(new float[] {26f, 14f, 44f, 16f})).useAllAvailableWidth();
        addHeaderRow4(
                table,
                "Input parameters",
                "Data type",
                "Value description",
                "Mandatory / Non-mandatory",
                bold);
        List<ApiField> roots = rootRequestFields(master);
        if (roots.isEmpty()) {
            addMergedRow4(table, "No request fields defined for this API.", normal);
            return table;
        }
        List<RequestRow> rows = new ArrayList<>();
        for (ApiField root : roots) {
            collectRequestRows(root, "", rows);
        }
        for (RequestRow r : rows) {
            Color bg = nextZebraRowBg();
            addBodyRow4(table, r.parameter, r.dataType, r.description, r.mandatory, normal, bold, bg);
        }
        return table;
    }

    private Table buildResponseBodyTable(
            ApiMaster master,
            PdfFont normal,
            PdfFont bold,
            ResponseKind kind,
            String emptyMessage) {
        resetZebra();
        Table table = new Table(UnitValue.createPercentArray(new float[] {28f, 16f, 56f})).useAllAvailableWidth();
        addHeaderRow3(table, "Output parameters", "Data type", "Value description", bold);
        List<ApiResponseField> roots = rootResponseFields(master, kind);
        if (roots.isEmpty()) {
            Cell c = new Cell(1, 3)
                    .add(new Paragraph(emptyMessage)
                            .setFont(normal)
                            .setFontSize(tableFont))
                    .setPadding(6)
                    .setBorder(CELL_BORDER)
                    .setBackgroundColor(new DeviceRgb(255, 250, 240));
            table.addCell(c);
            return table;
        }
        List<ResponseRow> rows = new ArrayList<>();
        for (ApiResponseField root : roots) {
            collectResponseRows(root, "", rows);
        }
        for (ResponseRow r : rows) {
            Color bg = nextZebraRowBg();
            addBodyRow3(table, r.parameter, r.dataType, r.description, normal, bold, bg);
        }
        return table;
    }

    private void collectRequestRows(ApiField field, String prefix, List<RequestRow> out) {
        String path = prefix.isEmpty() ? field.getFieldKey() : prefix + "." + field.getFieldKey();
        String type = field.getDataType() != null ? field.getDataType().trim() : "—";
        String desc = buildRequestDescription(field);
        String mand =
                Boolean.TRUE.equals(field.getRequired()) ? "MANDATORY" : "NON-MANDATORY";

        List<ApiField> children = sortedRequestChildren(field);
        boolean isComposite = "OBJECT".equalsIgnoreCase(type) || "ARRAY".equalsIgnoreCase(type);

        if (isComposite && !children.isEmpty()) {
            String groupDesc = StringUtils.hasText(field.getDescription())
                    ? field.getDescription()
                    : ("Composite field (" + type.toLowerCase(Locale.ROOT) + "). Nested parameters below.");
            out.add(new RequestRow(path + " (" + type.toLowerCase(Locale.ROOT) + ")", type, groupDesc, "—"));
            for (ApiField child : children) {
                collectRequestRows(child, path, out);
            }
            return;
        }

        out.add(new RequestRow(path, type, desc, mand));
        for (ApiField child : children) {
            collectRequestRows(child, path, out);
        }
    }

    private void collectResponseRows(ApiResponseField field, String prefix, List<ResponseRow> out) {
        String path = prefix.isEmpty() ? field.getFieldKey() : prefix + "." + field.getFieldKey();
        String type = field.getDataType() != null ? field.getDataType().trim() : "—";
        String desc = buildResponseDescription(field);

        List<ApiResponseField> children = sortedResponseChildren(field);
        boolean isComposite = "OBJECT".equalsIgnoreCase(type) || "ARRAY".equalsIgnoreCase(type);

        if (isComposite && !children.isEmpty()) {
            String groupDesc = StringUtils.hasText(field.getDescription())
                    ? field.getDescription()
                    : ("Composite field (" + type.toLowerCase(Locale.ROOT) + "). Nested outputs below.");
            out.add(new ResponseRow(path + " (" + type.toLowerCase(Locale.ROOT) + ")", type, groupDesc));
            for (ApiResponseField child : children) {
                collectResponseRows(child, path, out);
            }
            return;
        }

        out.add(new ResponseRow(path, type, desc));
        for (ApiResponseField child : children) {
            collectResponseRows(child, path, out);
        }
    }

    private String buildRequestDescription(ApiField field) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(field.getDescription())) {
            sb.append(field.getDescription().trim());
        }
        if (StringUtils.hasText(field.getDefaultValue())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("Default: ").append(field.getDefaultValue().trim());
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }

    private String buildResponseDescription(ApiResponseField field) {
        if (StringUtils.hasText(field.getDescription())) {
            return field.getDescription().trim();
        }
        return "—";
    }

    private List<ApiField> sortedRequestChildren(ApiField field) {
        return field.getChildFields().stream()
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ApiResponseField> sortedResponseChildren(ApiResponseField field) {
        return field.getChildFields().stream()
                .sorted(Comparator.comparing(ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ApiField> rootRequestFields(ApiMaster master) {
        return master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ApiResponseField> rootResponseFields(ApiMaster master, ResponseKind kind) {
        return master.getResponseFields().stream()
                .filter(
                        f -> {
                            if (f.getParent() != null) {
                                return false;
                            }
                            if (kind == ResponseKind.FAILURE) {
                                return f.getResponseKind() == ResponseKind.FAILURE;
                            }
                            return f.getResponseKind() == null || f.getResponseKind() == ResponseKind.SUCCESS;
                        })
                .sorted(Comparator.comparing(ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private String buildFullUrl(ApiMaster master) {
        String base = master.getBaseUrl() != null ? master.getBaseUrl().trim() : "";
        String path = master.getPathTemplate() != null ? master.getPathTemplate().trim() : "";
        if (!StringUtils.hasText(path)) {
            return base;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private void addHeaderRow2(Table table, String c1, String c2, PdfFont bold) {
        table.addCell(headerCell(c1, bold));
        table.addCell(headerCell(c2, bold));
    }

    private void addBodyRow2(Table table, String label, String value, PdfFont normal, PdfFont bold, Color rowBg) {
        table.addCell(dataCell(textOrDash(label), bold, rowBg));
        table.addCell(dataCell(textOrDash(value), normal, rowBg));
    }

    private void addHeaderRow3(Table table, String c1, String c2, String c3, PdfFont bold) {
        table.addCell(headerCell(c1, bold));
        table.addCell(headerCell(c2, bold));
        table.addCell(headerCell(c3, bold));
    }

    private void addBodyRow3(
            Table table, String c1, String c2, String c3, PdfFont normal, PdfFont bold, Color rowBg) {
        table.addCell(dataCell(textOrDash(c1), bold, rowBg));
        table.addCell(dataCell(textOrDash(c2), normal, rowBg));
        table.addCell(dataCell(textOrDash(c3), normal, rowBg));
    }

    private void addHeaderRow4(Table table, String c1, String c2, String c3, String c4, PdfFont bold) {
        table.addCell(headerCell(c1, bold));
        table.addCell(headerCell(c2, bold));
        table.addCell(headerCell(c3, bold));
        table.addCell(headerCell(c4, bold));
    }

    private void addBodyRow4(
            Table table,
            String c1,
            String c2,
            String c3,
            String c4,
            PdfFont normal,
            PdfFont bold,
            Color rowBg) {
        table.addCell(dataCell(textOrDash(c1), bold, rowBg));
        table.addCell(dataCell(textOrDash(c2), normal, rowBg));
        table.addCell(dataCell(textOrDash(c3), normal, rowBg));
        table.addCell(dataCell(textOrDash(c4), normal, rowBg));
    }

    private void addMergedRow4(Table table, String text, PdfFont normal) {
        Cell c = new Cell(1, 4)
                .add(new Paragraph(text).setFont(normal).setFontSize(tableFont))
                .setPadding(6)
                .setBorder(CELL_BORDER)
                .setItalic()
                .setBackgroundColor(new DeviceRgb(255, 252, 245));
        table.addCell(c);
    }

    private Cell headerCell(String text, PdfFont bold) {
        return new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(tableFont).setFontColor(HEADER_FG))
                .setBackgroundColor(HEADER_BG)
                .setPadding(6)
                .setBorder(CELL_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell dataCell(String text, PdfFont font, Color background) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(tableFont))
                .setPadding(5)
                .setBorder(CELL_BORDER)
                .setBackgroundColor(background)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private static String textOrDash(String s) {
        return StringUtils.hasText(s) ? s : "—";
    }

    private static final class RequestRow {
        final String parameter;
        final String dataType;
        final String description;
        final String mandatory;

        RequestRow(String parameter, String dataType, String description, String mandatory) {
            this.parameter = parameter;
            this.dataType = dataType;
            this.description = description;
            this.mandatory = mandatory;
        }
    }

    private static final class ResponseRow {
        final String parameter;
        final String dataType;
        final String description;

        ResponseRow(String parameter, String dataType, String description) {
            this.parameter = parameter;
            this.dataType = dataType;
            this.description = description;
        }
    }
}
