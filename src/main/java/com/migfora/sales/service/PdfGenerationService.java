package com.migfora.sales.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 4:16 AM
 */
@Service
@Slf4j
public class PdfGenerationService {


    public byte[] generatePdf(String title, String markdownContent, String domain) {
        try {
            log.info("[PDF] Generating | title={}", title);
            String html = buildHtml(title, markdownContent, domain);
            byte[] pdf = htmlToPdf(html);
            log.info("[PDF] Generated | bytes={}", pdf.length);
            return pdf;
        } catch (Exception ex) {
            log.error("[PDF] Generation failed | error={}", ex.getMessage());
            throw new RuntimeException("PDF generation failed: " + ex.getMessage());
        }
    }

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ConverterProperties props = new ConverterProperties();
            props.setCharset("UTF-8");
            HtmlConverter.convertToPdf(
                    new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)),
                    baos,
                    props
            );
            return baos.toByteArray();
        }
    }

    private String buildHtml(String title, String markdown, String domain) {
        String body = convertMarkdownToHtml(markdown);
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: Arial, Helvetica, sans-serif;
                            font-size: 13px;
                            line-height: 1.7;
                            color: #2c3e50;
                            background: #fff;
                        }
                        .header {
                            background: #1a2980;
                            color: white;
                            padding: 40px 40px 30px;
                            margin-bottom: 30px;
                        }
                        .header .company {
                            font-size: 11px;
                            opacity: 0.8;
                            margin-bottom: 8px;
                        }
                        .header h1 {
                            font-size: 22px;
                            font-weight: 700;
                            margin-bottom: 8px;
                        }
                        .header .meta {
                            font-size: 11px;
                            opacity: 0.7;
                        }
                        .content { padding: 0 40px; }
                        h1 {
                            font-size: 20px;
                            color: #1a2980;
                            margin: 28px 0 10px;
                            border-bottom: 2px solid #1a2980;
                            padding-bottom: 5px;
                        }
                        h2 {
                            font-size: 16px;
                            color: #1a2980;
                            margin: 22px 0 8px;
                        }
                        h3 {
                            font-size: 14px;
                            color: #2c3e50;
                            margin: 16px 0 6px;
                        }
                        p  { margin-bottom: 10px; }
                        ul, ol { margin: 6px 0 10px 24px; }
                        li { margin-bottom: 4px; }
                        strong { color: #1a2980; font-weight: bold; }
                        em { font-style: italic; }
                        code {
                            background: #f4f6f9;
                            border: 1px solid #dde1e7;
                            padding: 1px 5px;
                            border-radius: 3px;
                            font-size: 11px;
                            font-family: Courier, monospace;
                        }
                        pre {
                            background: #f4f6f9;
                            border: 1px solid #dde1e7;
                            border-left: 4px solid #1a2980;
                            padding: 12px;
                            border-radius: 3px;
                            margin: 10px 0;
                            font-size: 11px;
                            font-family: Courier, monospace;
                        }
                        blockquote {
                            border-left: 4px solid #1a2980;
                            background: #f0f4ff;
                            padding: 8px 14px;
                            margin: 10px 0;
                        }
                        table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin: 12px 0;
                        }
                        th {
                            background: #1a2980;
                            color: white;
                            padding: 8px 10px;
                            text-align: left;
                            font-size: 12px;
                        }
                        td {
                            padding: 7px 10px;
                            border-bottom: 1px solid #eef0f3;
                            font-size: 12px;
                        }
                        tr:nth-child(even) td { background: #f9fafc; }
                        .footer {
                            margin-top: 40px;
                            padding: 16px 40px;
                            background: #f4f6f9;
                            border-top: 1px solid #dde1e7;
                            font-size: 10px;
                            color: #7f8c8d;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="company">MIGFORA — Technology Services</div>
                        <h1>%s</h1>
                        <div class="meta">Domain: %s &nbsp;|&nbsp; Generated: %s</div>
                    </div>
                    <div class="content">%s</div>
                    <div class="footer">
                        Confidential — Generated by MIGFORA Sales Intelligence Platform
                    </div>
                </body>
                </html>
                """.formatted(title, domain, date, body);
    }

    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null) return "";
        String html = markdown;

        // ── Code blocks first ─────────────────────────────────────────────────────
        html = html.replaceAll("(?s)```[a-z]*\n?(.*?)```", "<pre><code>$1</code></pre>");

        // ── Tables ────────────────────────────────────────────────────────────────
        html = convertTables(html);

        // ── Headers ───────────────────────────────────────────────────────────────
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^### (.+)$",  "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.+)$",   "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.+)$",    "<h1>$1</h1>");

        // ── Bold and italic ───────────────────────────────────────────────────────
        html = html.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        html = html.replaceAll("\\*\\*(.+?)\\*\\*",       "<strong>$1</strong>");
        html = html.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");

        // ── Inline code ───────────────────────────────────────────────────────────
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");

        // ── Blockquote ────────────────────────────────────────────────────────────
        html = html.replaceAll("(?m)^> (.+)$", "<blockquote>$1</blockquote>");

        // ── Horizontal rule ───────────────────────────────────────────────────────
        html = html.replaceAll("(?m)^---+$", "<hr/>");

        // ── Lists ─────────────────────────────────────────────────────────────────
        html = convertLists(html);

        // ── Paragraphs ────────────────────────────────────────────────────────────
        html = wrapParagraphs(html);

        return html;
    }

    private String convertTables(String html) {
        String[] lines = html.split("\n");
        StringBuilder result = new StringBuilder();
        List<String> tableLines = new ArrayList<>();
        boolean inTable = false;

        for (String line : lines) {
            String trimmed = line.trim();
            boolean isTableLine = trimmed.startsWith("|") && trimmed.endsWith("|");

            if (isTableLine) {
                inTable = true;
                tableLines.add(trimmed);
            } else {
                if (inTable) {
                    result.append(renderTable(tableLines));
                    tableLines.clear();
                    inTable = false;
                }
                result.append(line).append("\n");
            }
        }

        // Handle table at end of content
        if (!tableLines.isEmpty()) {
            result.append(renderTable(tableLines));
        }

        return result.toString();
    }

    private String renderTable(List<String> lines) {
        if (lines.size() < 2) return String.join("\n", lines) + "\n";

        StringBuilder table = new StringBuilder("<table>\n");
        boolean headerDone = false;

        for (String line : lines) {
            // Skip separator lines like |---|---|
            if (line.replaceAll("[|\\-: ]", "").isEmpty()) {
                headerDone = true;
                continue;
            }

            // Split cells
            String[] cells = line.split("\\|");
            List<String> cellList = new ArrayList<>();
            for (String cell : cells) {
                String c = cell.trim();
                if (!c.isEmpty()) cellList.add(c);
            }

            if (cellList.isEmpty()) continue;

            String tag = !headerDone ? "th" : "td";
            table.append("<tr>");
            for (String cell : cellList) {
                // Apply inline bold/italic inside cells
                String cellContent = cell
                        .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                        .replaceAll("\\*(.+?)\\*",        "<em>$1</em>")
                        .replaceAll("`([^`]+)`",           "<code>$1</code>");
                table.append("<").append(tag).append(">")
                        .append(cellContent)
                        .append("</").append(tag).append(">");
            }
            table.append("</tr>\n");

            if (!headerDone) headerDone = true;
        }

        table.append("</table>\n");
        return table.toString();
    }

    private String convertLists(String html) {
        String[] lines = html.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inUl = false;
        boolean inOl = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.matches("^[-*•] .+")) {
                if (!inUl) { result.append("<ul>\n"); inUl = true; }
                String item = trimmed.replaceFirst("^[-*•] ", "");
                item = applyInlineFormatting(item);
                result.append("<li>").append(item).append("</li>\n");
            } else if (trimmed.matches("^\\d+\\. .+")) {
                if (!inOl) { result.append("<ol>\n"); inOl = true; }
                String item = trimmed.replaceFirst("^\\d+\\. ", "");
                item = applyInlineFormatting(item);
                result.append("<li>").append(item).append("</li>\n");
            } else {
                if (inUl) { result.append("</ul>\n"); inUl = false; }
                if (inOl) { result.append("</ol>\n"); inOl = false; }
                result.append(line).append("\n");
            }
        }

        if (inUl) result.append("</ul>\n");
        if (inOl) result.append("</ol>\n");

        return result.toString();
    }

    private String wrapParagraphs(String html) {
        String[] lines = html.split("\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean isBlock = trimmed.startsWith("<h")   ||
                    trimmed.startsWith("<ul")  ||
                    trimmed.startsWith("<ol")  ||
                    trimmed.startsWith("<li")  ||
                    trimmed.startsWith("</ul") ||
                    trimmed.startsWith("</ol") ||
                    trimmed.startsWith("<pre") ||
                    trimmed.startsWith("</pre")||
                    trimmed.startsWith("<table")||
                    trimmed.startsWith("</table")||
                    trimmed.startsWith("<tr")  ||
                    trimmed.startsWith("<th")  ||
                    trimmed.startsWith("<td")  ||
                    trimmed.startsWith("<blockquote") ||
                    trimmed.startsWith("<hr");

            if (isBlock) {
                sb.append(trimmed).append("\n");
            } else {
                sb.append("<p>").append(applyInlineFormatting(trimmed)).append("</p>\n");
            }
        }
        return sb.toString();
    }

    private String applyInlineFormatting(String text) {
        text = text.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        text = text.replaceAll("\\*\\*(.+?)\\*\\*",       "<strong>$1</strong>");
        text = text.replaceAll("`([^`]+)`",               "<code>$1</code>");
        return text;
    }
}
