package com.apidoc.platform.infrastructure.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.IOException;
import org.springframework.util.StringUtils;

/**
 * Renders a small markdown-like subset into iText paragraphs: {@code **bold**}, {@code __underline__},
 * {@code *italic*}, and leading {@code - }, {@code * }, or {@code • } as bullets. Arrow ({@code →}) is plain text.
 */
public final class ItextRichTextBlockRenderer {

    public static final class Fonts {
        public final PdfFont normal;
        public final PdfFont bold;
        public final PdfFont italic;
        public final PdfFont boldItalic;

        public Fonts(PdfFont normal, PdfFont bold, PdfFont italic, PdfFont boldItalic) {
            this.normal = normal;
            this.bold = bold;
            this.italic = italic;
            this.boldItalic = boldItalic;
        }

        public static Fonts create() throws IOException {
            return new Fonts(
                    PdfFontFactory.createFont(StandardFonts.HELVETICA),
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD),
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE),
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE));
        }
    }

    private ItextRichTextBlockRenderer() {}

    public static void addRichBlock(Document doc, String text, Fonts fonts, float fontSize) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String normalized = text.replace("\r\n", "\n");
        for (String rawLine : normalized.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            boolean bullet = isBulletLine(line);
            String rest = bullet ? stripBulletPrefix(line) : line;

            Paragraph p = new Paragraph()
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(3f)
                    .setFont(fonts.normal)
                    .setFontSize(fontSize);
            if (bullet) {
                p.setMarginLeft(14f);
                p.setFirstLineIndent(-10f);
                p.add(new Text("• ").setFont(fonts.normal).setFontSize(fontSize));
            }
            appendRichLine(rest, p, fonts, fontSize);
            doc.add(p);
        }
        doc.add(new Paragraph().setMarginBottom(2f));
    }

    private static boolean isBulletLine(String line) {
        return line.startsWith("- ")
                || line.startsWith("• ")
                || (line.startsWith("* ") && !line.startsWith("**"));
    }

    private static String stripBulletPrefix(String line) {
        if (line.startsWith("• ")) {
            return line.substring(2).trim();
        }
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2).trim();
        }
        return line;
    }

    /** Appends one line of rich text into an existing paragraph. */
    public static void appendRichLine(String line, Paragraph p, Fonts fonts, float fontSize) {
        appendBoldRegions(line, p, fonts, fontSize);
    }

    private static void appendBoldRegions(String s, Paragraph p, Fonts fonts, float fontSize) {
        int i = 0;
        while (true) {
            int bs = s.indexOf("**", i);
            if (bs < 0) {
                appendUnderlineRegions(s.substring(i), p, fonts, fontSize, false);
                return;
            }
            if (bs > i) {
                appendUnderlineRegions(s.substring(i, bs), p, fonts, fontSize, false);
            }
            int be = s.indexOf("**", bs + 2);
            if (be < 0) {
                appendUnderlineRegions(s.substring(bs), p, fonts, fontSize, false);
                return;
            }
            appendUnderlineRegions(s.substring(bs + 2, be), p, fonts, fontSize, true);
            i = be + 2;
        }
    }

    private static void appendUnderlineRegions(String s, Paragraph p, Fonts fonts, float fontSize, boolean bold) {
        int i = 0;
        while (true) {
            int us = s.indexOf("__", i);
            if (us < 0) {
                appendItalicStars(s.substring(i), p, fonts, fontSize, bold, false);
                return;
            }
            if (us > i) {
                appendItalicStars(s.substring(i, us), p, fonts, fontSize, bold, false);
            }
            int ue = s.indexOf("__", us + 2);
            if (ue < 0) {
                appendItalicStars(s.substring(us), p, fonts, fontSize, bold, false);
                return;
            }
            String under = s.substring(us + 2, ue);
            addText(p, under, fonts, fontSize, bold, false, true);
            i = ue + 2;
        }
    }

    private static void appendItalicStars(String s, Paragraph p, Fonts fonts, float fontSize, boolean bold, boolean underline) {
        int i = 0;
        while (i < s.length()) {
            int star = s.indexOf('*', i);
            if (star < 0) {
                addText(p, s.substring(i), fonts, fontSize, bold, false, underline);
                return;
            }
            if (star > i) {
                addText(p, s.substring(i, star), fonts, fontSize, bold, false, underline);
            }
            if (star + 1 < s.length() && s.charAt(star + 1) == '*') {
                addText(p, "**", fonts, fontSize, bold, false, underline);
                i = star + 2;
                continue;
            }
            int end = s.indexOf('*', star + 1);
            if (end < 0) {
                addText(p, s.substring(star), fonts, fontSize, bold, false, underline);
                return;
            }
            String mid = s.substring(star + 1, end);
            addText(p, mid, fonts, fontSize, bold, true, underline);
            i = end + 1;
        }
    }

    private static void addText(
            Paragraph p, String chunk, Fonts fonts, float fontSize, boolean bold, boolean italic, boolean underline) {
        if (chunk.isEmpty()) {
            return;
        }
        PdfFont font;
        if (bold && italic) {
            font = fonts.boldItalic;
        } else if (bold) {
            font = fonts.bold;
        } else if (italic) {
            font = fonts.italic;
        } else {
            font = fonts.normal;
        }
        Text t = new Text(chunk).setFont(font).setFontSize(fontSize);
        if (underline) {
            t.setUnderline(0.75f, -2f);
        }
        p.add(t);
    }
}
