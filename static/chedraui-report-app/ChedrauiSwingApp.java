import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class ChedrauiSwingApp {

    private static final String SEARCH_ENDPOINT =
            "https://www.chedraui.com.mx/api/catalog_system/pub/products/search";
    private static final int LINK_COLUMN_INDEX = 7;
    private static final int PRICE_COLUMN_INDEX = 9;
    private static final int LIST_PRICE_COLUMN_INDEX = 10;

    private static final String[] COLUMNS = {
            "productId",
            "productName",
            "brand",
            "productTitle",
            "releaseDate",
            "categories",
            "categoriesIds",
            "link",
            "Maximo de venta",
            "Price",
            "ListPrice",
            "PriceWithoutDiscount",
            "AvailableQuantity",
            "IsAvailable"
    };

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0);
    private final JLabel statusLabel = new JLabel("Listo");
    private final JLabel productsFoundLabel = new JLabel("Productos encontrados: 0");
    private final JTextArea outputArea = new JTextArea(8, 120);
    private final JScrollPane outputScrollPane = new JScrollPane(outputArea);
    private final JTextField categoryOneField = new JTextField("", 10);
    private final JTextField categoryTwoField = new JTextField("", 10);
    private final JTextField fromField = new JTextField("", 4);
    private final JTextField toField = new JTextField("", 4);
    private static final DateTimeFormatter LOG_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MXN_CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChedrauiSwingApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Chedraui Products Report (Standalone Swing)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 700);
        frame.setLocationRelativeTo(null);

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable currentTable,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(currentTable, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    component.setBackground(isOfferRow(row) ? new Color(220, 245, 220) : Color.WHITE);
                }
                return component;
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != LINK_COLUMN_INDEX) {
                    return;
                }

                Object value = table.getValueAt(row, col);
                if (value == null) {
                    return;
                }

                String url = String.valueOf(value).trim();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    openInChrome(url);
                }
            }
        });

        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == LINK_COLUMN_INDEX) {
                    Object value = table.getValueAt(row, col);
                    String url = value == null ? "" : String.valueOf(value).trim();
                    table.setCursor((url.startsWith("http://") || url.startsWith("https://"))
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel tablePanel = new JPanel(new BorderLayout());
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryPanel.add(productsFoundLabel);
        tablePanel.add(summaryPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JButton fetchButton = new JButton("Consumir endpoint");
        fetchButton.addActionListener(e -> fetchProducts());

        JButton exportButton = new JButton("Generar HTML");
        exportButton.addActionListener(e -> exportHtml());

        JButton clearOutputButton = new JButton("Limpiar salida");
        clearOutputButton.addActionListener(e -> outputArea.setText(""));

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputScrollPane.setVisible(true);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Categoria 1:"));
        topPanel.add(categoryOneField);
        topPanel.add(new JLabel("Categoria 2:"));
        topPanel.add(categoryTwoField);
        topPanel.add(new JLabel("Desde:"));
        topPanel.add(fromField);
        topPanel.add(new JLabel("Hasta:"));
        topPanel.add(toField);
        topPanel.add(fetchButton);
        topPanel.add(exportButton);
        topPanel.add(clearOutputButton);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(statusLabel);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomPanel, BorderLayout.NORTH);
        southPanel.add(outputScrollPane, BorderLayout.CENTER);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(tablePanel, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void fetchProducts() {
        try {
            String categoryOne = categoryOneField.getText() == null ? "" : categoryOneField.getText().trim();
            String categoryTwo = categoryTwoField.getText() == null ? "" : categoryTwoField.getText().trim();
            if (categoryOne.isBlank() || categoryTwo.isBlank()) {
                statusLabel.setText("Debes capturar ambas categorias antes de consumir");
                log("Validacion: faltan parametros de categoria (Categoria 1 y Categoria 2).");
                return;
            }

            String fromRaw = fromField.getText() == null ? "" : fromField.getText().trim();
            String toRaw = toField.getText() == null ? "" : toField.getText().trim();
            if (fromRaw.isBlank() || toRaw.isBlank()) {
                statusLabel.setText("Debes capturar los parametros desde/hasta");
                log("Validacion: faltan parametros desde/hasta.");
                return;
            }

            int from = Integer.parseInt(fromRaw);
            int to = Integer.parseInt(toRaw);
            if (from < 0 || to < 0 || from > to) {
                statusLabel.setText("Rango invalido: revisa desde/hasta");
                log("Validacion: rango invalido. desde=" + from + ", hasta=" + to + ".");
                return;
            }

            statusLabel.setText("Consultando endpoint...");
            log("Iniciando consumo de endpoint.");
            tableModel.setRowCount(0);
            updateProductsFound(0);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            String composedCategoryPath = categoryOne + "/" + categoryTwo;
            log("Categoria compuesta a consultar: " + composedCategoryPath + " | desde=" + from + " | hasta=" + to);

            List<Product> products = fetchProductsByCategory(client, composedCategoryPath, from, to);

            for (Product product : products) {
                tableModel.addRow(new Object[] {
                        product.productId,
                        product.productName,
                        product.brand,
                        product.productTitle,
                        product.releaseDate,
                        product.categories,
                        product.categoriesIds,
                        product.link,
                        product.maximoVenta,
                        formatPriceMxn(product.price),
                        formatPriceMxn(product.listPrice),
                        formatPriceMxn(product.priceWithoutDiscount),
                        product.availableQuantity,
                        product.isAvailable
                });
            }

            int totalRows = tableModel.getRowCount();
            updateProductsFound(totalRows);
            statusLabel.setText("Productos cargados: " + totalRows);
            log("Endpoint consumido correctamente. Productos cargados: " + totalRows);

            if (totalRows == 0) {
                statusLabel.setText("Consumo completado sin productos");
                log("El endpoint respondio sin productos para mostrar.");
                return;
            }
        } catch (Exception ex) {
            statusLabel.setText("Error al consultar endpoint");
            updateProductsFound(0);
            log("Error al consultar endpoint: " + ex.getMessage());
        }
    }

    private void exportHtml() {
        try {
            if (tableModel.getRowCount() == 0) {
                statusLabel.setText("No hay datos para exportar");
                log("No hay datos para exportar.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(Path.of("chedraui-productos-report.html").toFile());
            int result = chooser.showSaveDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                log("Exportacion cancelada por el usuario.");
                return;
            }

            Path output = chooser.getSelectedFile().toPath();
            String html = buildHtml();
            Files.writeString(output, html, StandardCharsets.UTF_8);
            statusLabel.setText("HTML generado en: " + output.toAbsolutePath());
            log("Reporte HTML generado correctamente en: " + output.toAbsolutePath());
        } catch (Exception ex) {
            statusLabel.setText("Error al exportar HTML");
            log("Error al exportar HTML: " + ex.getMessage());
        }
    }

    private void log(String message) {
        outputArea.append("[" + LocalDateTime.now().format(LOG_DATE_TIME) + "] " + message + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void updateProductsFound(int total) {
        productsFoundLabel.setText("Productos encontrados: " + total);
    }

    private static double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) {
            return Double.MAX_VALUE;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return Double.MAX_VALUE;
        }
    }

    private static String formatPriceMxn(String rawValue) {
        double parsed = parseDoubleSafe(rawValue);
        if (parsed == Double.MAX_VALUE) {
            return "";
        }
        return MXN_CURRENCY.format(parsed);
    }

    private static String buildCategoryEndpoint(String categoryPath, int from, int to) {
        return SEARCH_ENDPOINT + "?fq=C:/" + categoryPath + "/&O=OrderByPriceASC&_from=" + from + "&_to=" + to;
    }

    private List<Product> fetchProductsByCategory(HttpClient client, String categoryPath, int from, int to) throws Exception {
        String endpoint = buildCategoryEndpoint(categoryPath, from, to);
        log("Consultando categoria: " + categoryPath + " -> " + endpoint);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .GET()
                .timeout(Duration.ofSeconds(40))
                .header("Accept", "application/json")
                .header("Authorization", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " en categoria " + categoryPath + ": " + response.body());
        }

        log("HTTP recibido para categoria " + categoryPath + ": " + response.statusCode());

        List<String> productObjects = splitTopLevelObjects(response.body());
        List<Product> products = new ArrayList<>();
        for (String productJson : productObjects) {
            products.add(mapProduct(productJson));
        }
        log("Productos recibidos para categoria " + categoryPath + ": " + products.size());
        return products;
    }

    private boolean isOfferRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return false;
        }

        String price = String.valueOf(tableModel.getValueAt(row, PRICE_COLUMN_INDEX));
        String listPrice = String.valueOf(tableModel.getValueAt(row, LIST_PRICE_COLUMN_INDEX));
        double priceValue = parseCurrencySafe(price);
        double listPriceValue = parseCurrencySafe(listPrice);
        return priceValue >= 0 && listPriceValue >= 0 && priceValue < listPriceValue;
    }

    private static double parseCurrencySafe(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }

        String cleaned = value.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank()) {
            return -1;
        }

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void openInChrome(String url) {
        try {
            Process process = new ProcessBuilder("open", "-a", "Google Chrome", url).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log("Abriendo URL en Chrome: " + url);
                return;
            }
            throw new IllegalStateException("No se pudo abrir Chrome, codigo " + exitCode);
        } catch (Exception ex) {
            log("No se pudo abrir Chrome directamente. Se intenta navegador por defecto. Motivo: " + ex.getMessage());
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(url));
                    log("URL abierta en navegador por defecto: " + url);
                }
            } catch (Exception browseEx) {
                statusLabel.setText("No fue posible abrir el link");
                log("Error al abrir link: " + browseEx.getMessage());
            }
        }
    }

    private String buildHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"es\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Reporte Productos Chedraui</title>\n");
        sb.append("<style>");
        sb.append("body{font-family:Segoe UI,Tahoma,sans-serif;background:#f6f8fb;color:#1f2836;margin:20px;}");
        sb.append("h1{margin:0 0 8px 0;} .meta{color:#5b6678;font-size:12px;margin-bottom:12px;}");
        sb.append(".wrap{overflow-x:auto;background:#fff;border:1px solid #dbe2ee;border-radius:10px;}");
        sb.append("table{border-collapse:collapse;min-width:1700px;width:100%;}");
        sb.append("th,td{border:1px solid #dbe2ee;padding:8px;font-size:12px;text-align:left;vertical-align:top;}");
        sb.append("th{background:#0c5cab;color:#fff;position:sticky;top:0;} tr:nth-child(even){background:#f3f6fb;}");
        sb.append(".num{text-align:right;white-space:nowrap;}");
        sb.append("</style></head><body>\n");
        sb.append("<h1>Reporte de productos Chedraui</h1>\n");
        sb.append("<div class=\"meta\">Categoria consultada: ")
            .append(escapeHtml(categoryOneField.getText()))
            .append("/")
            .append(escapeHtml(categoryTwoField.getText()))
            .append("</div>\n");
        sb.append("<div class=\"meta\">Total productos: ").append(tableModel.getRowCount()).append("</div>\n");
        sb.append("<div class=\"wrap\"><table><thead><tr>");

        for (String col : COLUMNS) {
            sb.append("<th>").append(escapeHtml(col)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append("<tr>");
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                String value = String.valueOf(tableModel.getValueAt(r, c));
                if (c >= 9 && c <= 12) {
                    sb.append("<td class=\"num\">").append(escapeHtml(value)).append("</td>");
                } else if (c == 7 && value != null && value.startsWith("http")) {
                    sb.append("<td><a href=\"").append(escapeHtml(value)).append("\" target=\"_blank\" rel=\"noopener\">")
                            .append(escapeHtml(value)).append("</a></td>");
                } else {
                    sb.append("<td>").append(escapeHtml(value)).append("</td>");
                }
            }
            sb.append("</tr>");
        }

        sb.append("</tbody></table></div></body></html>");
        return sb.toString();
    }

    private Product mapProduct(String productJson) {
        return new Product(
                extractString(productJson, "productId"),
                extractString(productJson, "productName"),
                extractString(productJson, "brand"),
                extractString(productJson, "productTitle"),
                formatReleaseDate(extractString(productJson, "releaseDate")),
                extractArrayAsJoinedText(productJson, "categories"),
                extractArrayAsJoinedText(productJson, "categoriesIds"),
                extractString(productJson, "link"),
                extractArrayFirstText(productJson, "Máximo de venta"),
                extractNumber(productJson, "Price"),
                extractNumber(productJson, "ListPrice"),
                extractNumber(productJson, "PriceWithoutDiscount"),
                extractNumber(productJson, "AvailableQuantity"),
                extractBoolean(productJson, "IsAvailable")
        );
    }

    private static String formatReleaseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "";
        }

        String value = rawDate.trim();

        try {
            return LocalDate.parse(value).format(OUTPUT_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate().format(OUTPUT_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(value).toLocalDate().format(OUTPUT_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value).toLocalDate().format(OUTPUT_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        return value;
    }

    private static List<String> splitTopLevelObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < jsonArray.length(); i++) {
            char ch = jsonArray.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(jsonArray.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return normalizeWhitespace(unescapeJson(m.group(1)));
        }
        return "";
    }

    private static String extractNumber(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String extractBoolean(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).toLowerCase();
        }
        return "";
    }

    private static String extractArrayAsJoinedText(String json, String key) {
        String body = extractArrayBody(json, key);
        if (body.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        Matcher m = Pattern.compile("\\\"(.*?)\\\"", Pattern.DOTALL).matcher(body);
        while (m.find()) {
            values.add(normalizeWhitespace(unescapeJson(m.group(1))));
        }
        return String.join(" | ", values);
    }

    private static String extractArrayFirstText(String json, String key) {
        String body = extractArrayBody(json, key);
        if (body.isEmpty()) {
            return "";
        }
        Matcher m = Pattern.compile("\\\"(.*?)\\\"", Pattern.DOTALL).matcher(body);
        if (m.find()) {
            return normalizeWhitespace(unescapeJson(m.group(1)));
        }
        return "";
    }

    private static String extractArrayBody(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static String escapeHtml(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record Product(
            String productId,
            String productName,
            String brand,
            String productTitle,
            String releaseDate,
            String categories,
            String categoriesIds,
            String link,
            String maximoVenta,
            String price,
            String listPrice,
            String priceWithoutDiscount,
            String availableQuantity,
            String isAvailable
    ) {
    }
}
