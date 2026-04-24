import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniLangIDE extends JFrame {
    private JTextPane editor;
    private JTextArea lineNumbers;
    // 5 output panels: Output (status/errors), Tokens, Parse Tree, Symbol Table, TAC
    private JTextArea outputArea, tokensArea, parseTreeArea, tacArea;
    private JTable symbolTable;
    private DefaultTableModel symbolTableModel;
    private JTabbedPane outputTabs;
    private JLabel statusLabel;
    private File currentFile = null;
    private boolean isDarkTheme = false;

    // Dark Theme Colors (VS Code style)
    private Color bgDark = new Color(30, 30, 30);
    private Color fgDark = new Color(212, 212, 212);
    private Color gutterDark = new Color(37, 37, 38);
    private Color navDark = new Color(45, 45, 45);
    private Color accentGreen = new Color(78, 204, 130);
    private Color accentRed = new Color(244, 71, 71);

    // Light Theme Colors
    private Color bgLight = new Color(255, 255, 255);
    private Color fgLight = new Color(0, 0, 0);
    private Color gutterLight = new Color(240, 240, 240);
    private Color navLight = new Color(220, 220, 220);

    // Syntax Highlighting Colors
    private final Color COLOR_KEYWORD = new Color(86, 156, 214); // Blue
    private final Color COLOR_STRING = new Color(206, 145, 120); // Orange/Brown
    private final Color COLOR_COMMENT = new Color(106, 153, 85); // Green
    private final Color COLOR_TYPE = new Color(78, 201, 176); // Teal
    private final Color COLOR_NUMBER = new Color(181, 206, 168); // Light Green

    public MiniLangIDE() {
        setTitle("Mini Language Compiler");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.PLAIN, 12));

        setupUI();
        applyTheme();
        bindShortcuts();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // ===== TOP: Toolbar =====
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnOpen = createToolbarButton("Open");
        JButton btnSave = createToolbarButton("Save");
        JButton btnCompile = createToolbarButton("Run / Compile");
        JButton btnClear = createToolbarButton("Clear Output");
        JButton btnTheme = createToolbarButton("Toggle Theme");

        btnOpen.addActionListener(e -> openFile());
        btnSave.addActionListener(e -> saveFile());
        btnCompile.addActionListener(e -> compileCode());
        btnClear.addActionListener(e -> clearOutputs());
        btnTheme.addActionListener(e -> toggleTheme());

        toolbar.add(btnOpen);
        toolbar.add(btnSave);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(btnCompile);
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(btnTheme);
        add(toolbar, BorderLayout.NORTH);

        // ===== LEFT: Code Editor with Line Numbers =====
        editor = new JTextPane();
        editor.setFont(new Font("Consolas", Font.PLAIN, 16));
        editor.setCaretColor(Color.WHITE);
        ((AbstractDocument) editor.getDocument()).setDocumentFilter(new TabToSpaceFilter());

        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 16));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(new EmptyBorder(0, 10, 0, 10));

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateEditor();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateEditor();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                /* Ignored to prevent loop */ }
        });

        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.setRowHeaderView(lineNumbers);
        editorScrollPane.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel curFileLabel = new JLabel("  Editor - Untitled");
        curFileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        curFileLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        leftPanel.add(curFileLabel, BorderLayout.NORTH);
        leftPanel.add(editorScrollPane, BorderLayout.CENTER);

        // ===== RIGHT: Output Panels (4 tabs) =====
        outputTabs = new JTabbedPane();

        // Tab 1: Output (compilation status + errors)
        outputArea = createOutputArea();
        outputTabs.addTab("Output", new JScrollPane(outputArea));

        // Tab 2: Tokens
        tokensArea = createOutputArea();
        outputTabs.addTab("Tokens", new JScrollPane(tokensArea));

        // Tab 3: Parse Tree
        parseTreeArea = createOutputArea();
        outputTabs.addTab("Parse Tree", new JScrollPane(parseTreeArea));

        // Tab 4: TAC (Three-Address Code)
        tacArea = createOutputArea();
        outputTabs.addTab("TAC", new JScrollPane(tacArea));

        // Tab 5: Symbol Table (as a proper JTable)
        String[] columns = { "#", "Name", "Type", "Scope" };
        symbolTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        symbolTable = new JTable(symbolTableModel);
        symbolTable.setFont(new Font("Consolas", Font.PLAIN, 14));
        symbolTable.setRowHeight(28);
        symbolTable.setShowGrid(true);
        symbolTable.setGridColor(new Color(60, 60, 60));
        symbolTable.setIntercellSpacing(new Dimension(1, 1));
        outputTabs.addTab("Symbol Table", new JScrollPane(symbolTable));

        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel outputLabel = new JLabel("  Compiler Phases");
        outputLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        outputLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        rightPanel.add(outputLabel, BorderLayout.NORTH);
        rightPanel.add(outputTabs, BorderLayout.CENTER);

        // ===== SplitPane =====
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // ===== BOTTOM: Status Bar =====
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(new LineBorder(Color.GRAY, 1));
        statusLabel = new JLabel(" Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setEditable(false);
        return area;
    }

    private JButton createToolbarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void bindShortcuts() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        KeyStroke saveStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask);
        editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(saveStroke, "Save");
        editor.getActionMap().put("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        KeyStroke runStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutMask);
        editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(runStroke, "Run");
        editor.getActionMap().put("Run", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                compileCode();
            }
        });
    }

    private void updateEditor() {
        int totalLines = editor.getDocument().getDefaultRootElement().getElementCount();
        StringBuilder linesText = new StringBuilder();
        for (int i = 1; i <= totalLines; i++) {
            linesText.append(i).append("\n");
        }
        lineNumbers.setText(linesText.toString());
        SwingUtilities.invokeLater(this::applySyntaxHighlighting);
    }

    private void applySyntaxHighlighting() {
        try {
            StyledDocument doc = editor.getStyledDocument();
            String text = doc.getText(0, doc.getLength());

            SimpleAttributeSet baseStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(baseStyle, isDarkTheme ? fgDark : fgLight);
            doc.setCharacterAttributes(0, text.length(), baseStyle, true);

            // MiniLang (Bhojpuri) keywords
            highlightRegex(doc, text, "\\b(ank|bol|agar|jabtak|naito|suno|dashmlav|akshar)\\b", COLOR_KEYWORD);
            // Generic keywords
            highlightRegex(doc, text, "\\b(if|else|while|for|return|break|continue|main|void)\\b", COLOR_KEYWORD);
            highlightRegex(doc, text, "\\b(int|float|char|double|bool)\\b", COLOR_TYPE);
            highlightRegex(doc, text, "\\b\\d+(\\.\\d+)?\\b", COLOR_NUMBER);
            highlightRegex(doc, text, "\"[^\"]*\"", COLOR_STRING);
            highlightRegex(doc, text, "//.*", COLOR_COMMENT);
            highlightRegex(doc, text, "/\\*.*?\\*/", COLOR_COMMENT);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void highlightRegex(StyledDocument doc, String text, String regex, Color color) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
        applySyntaxHighlighting();
    }

    private void applyTheme() {
        Color bg = isDarkTheme ? bgDark : bgLight;
        Color fg = isDarkTheme ? fgDark : fgLight;
        Color gutterBg = isDarkTheme ? gutterDark : gutterLight;
        Color navBg = isDarkTheme ? navDark : navLight;

        editor.setBackground(bg);
        editor.setForeground(fg);
        editor.setCaretColor(fg);

        lineNumbers.setBackground(gutterBg);
        lineNumbers.setForeground(isDarkTheme ? Color.GRAY : Color.DARK_GRAY);

        // Output text areas
        JTextArea[] areas = { outputArea, tokensArea, parseTreeArea, tacArea };
        for (JTextArea area : areas) {
            area.setBackground(bg);
            area.setForeground(fg);
        }

        // Symbol Table styling
        symbolTable.setBackground(bg);
        symbolTable.setForeground(fg);
        symbolTable.setSelectionBackground(isDarkTheme ? new Color(50, 50, 80) : new Color(184, 207, 229));
        symbolTable.setGridColor(isDarkTheme ? new Color(60, 60, 60) : new Color(200, 200, 200));
        JTableHeader header = symbolTable.getTableHeader();
        header.setBackground(isDarkTheme ? new Color(50, 50, 55) : new Color(230, 230, 230));
        header.setForeground(isDarkTheme ? new Color(200, 200, 200) : Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Center-align all table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < symbolTable.getColumnCount(); i++) {
            symbolTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Toolbar and status bar
        Container[] containers = {
                getContentPane(),
                (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH),
                (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH)
        };
        for (Container c : containers) {
            if (c != null)
                c.setBackground(navBg);
        }
    }

    // ===== FILE ACTIONS =====

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                editor.setText("");
                editor.read(reader, null);
                statusLabel.setText(" Loaded " + currentFile.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage());
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            writer.write(editor.getText());
            statusLabel.setText(" Saved " + currentFile.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
        }
    }

    private void clearOutputs() {
        outputArea.setText("");
        tokensArea.setText("");
        parseTreeArea.setText("");
        tacArea.setText("");
        symbolTableModel.setRowCount(0);
        statusLabel.setText(" Output cleared.");
    }

    // ===== COMPILE: Run ./minilang, capture output, split into panels =====

    private void compileCode() {
        statusLabel.setText(" Compiling...");
        clearOutputs();
        String code = editor.getText();

        if (code.trim().isEmpty()) {
            statusLabel.setText(" Compilation failed: Editor is empty.");
            outputArea
                    .setText(" Error: No source code provided.\n\nPlease write some code in the editor and try again.");
            outputTabs.setSelectedIndex(0); // Switch to Output tab
            return;
        }

        new Thread(() -> {
            try {
                // 1. Save editor text to input.txt (what the C compiler reads)
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("input.txt"))) {
                    writer.write(code);
                }

                // 2. Execute the existing compiler binary — NO modifications to compiler logic
                Process p = new ProcessBuilder("./minilang").start();

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                StringBuilder stdoutBuf = new StringBuilder();
                StringBuilder stderrBuf = new StringBuilder();
                String s;
                while ((s = stdInput.readLine()) != null) {
                    stdoutBuf.append(s).append("\n");
                }
                while ((s = stdError.readLine()) != null) {
                    stderrBuf.append(s).append("\n");
                }

                int exitCode = p.waitFor();
                String fullOut = stdoutBuf.toString() + stderrBuf.toString();

                // 3. Parse the captured terminal output into sections
                StringBuilder tokens = new StringBuilder();
                StringBuilder parseTree = new StringBuilder();
                StringBuilder tacGen = new StringBuilder();
                StringBuilder errors = new StringBuilder();
                StringBuilder symbolEntries = new StringBuilder();

                String[] lines = fullOut.split("\n");
                // 0=Tokens, 1=ParseTree, 2=SymbolTable, 3=TAC
                int currentPhase = 0;

                for (String line : lines) {
                    String trimmed = line.trim();
                    String lower = trimmed.toLowerCase();

                    // Capture any error lines (syntax or semantic)
                    if (lower.contains("syntax error") || lower.contains("syntax galat")
                            || lower.contains("semantic error") || lower.contains("not declared")
                            || lower.contains("already declared")) {
                        errors.append(line).append("\n");
                        continue;
                    }

                    // Skip informational lines like "Parsing completed successfully"
                    if (lower.contains("parsing completed")) {
                        continue;
                    }

                    // Detect phase transitions
                    if (trimmed.equals("Parse Tree:")) {
                        currentPhase = 1;
                        continue;
                    } else if (trimmed.equals("Symbol Table:")) {
                        currentPhase = 2;
                        continue;
                    } else if (trimmed.equals("Three-Address Code:")
                            || trimmed.equals("Three-Address Code (TAC):")
                            || lower.contains("three-address code")) {
                        currentPhase = 3;
                        continue;
                    }

                    // Route line to correct section
                    if (currentPhase == 0) {
                        if (!trimmed.isEmpty()) {
                            tokens.append(line).append("\n");
                        }
                    } else if (currentPhase == 1) {
                        parseTree.append(line).append("\n");
                    } else if (currentPhase == 2) {
                        if (!trimmed.isEmpty()) {
                            symbolEntries.append(line).append("\n");
                        }
                    } else if (currentPhase == 3) {
                        tacGen.append(line).append("\n");
                    }
                }

                // 4. Update all GUI panels on EDT
                SwingUtilities.invokeLater(() -> {
                    // --- Tokens Tab ---
                    tokensArea.setText(tokens.toString().trim());

                    // --- Parse Tree Tab ---
                    parseTreeArea.setText(parseTree.toString().trim());

                    // --- TAC Tab ---
                    tacArea.setText(tacGen.toString().trim());

                    // --- Symbol Table Tab (proper table with Type & Scope columns) ---
                    symbolTableModel.setRowCount(0);
                    String[] symLines = symbolEntries.toString().trim().split("\n");
                    for (String symLine : symLines) {
                        symLine = symLine.trim();
                        if (symLine.isEmpty())
                            continue;
                        // Format from C now is: "1 : a : ank"
                        String[] parts = symLine.split("\\s*:\\s*");
                        if (parts.length >= 3) {
                            String index = parts[0].trim();
                            String name = parts[1].trim();
                            String rawType = parts[2].trim();
                            String type = rawType;
                            if(rawType.equals("ank")) type = "Integer (ank)";
                            else if(rawType.equals("dashmlav")) type = "Float (dashmlav)";
                            else if(rawType.equals("akshar")) type = "Char/Str (akshar)";
                            
                            String scope = "Global";
                            symbolTableModel.addRow(new Object[] { index, name, type, scope });
                        }
                    }

                    // --- Output Tab (compilation result / errors) ---
                    boolean hasErrors = errors.length() > 0;
                    if (hasErrors) {
                        // outputArea.setText("═══════════════════════════════════════\n");
                        outputArea.append("COMPILATION FAILED\n");
                        // outputArea.append("═══════════════════════════════════════\n\n");
                        outputArea.append("Errors:\n");
                        outputArea.append("\n");
                        outputArea.append(errors.toString().trim() + "\n\n");
                        // outputArea.append("───────────────────────────────────────\n");
                        outputArea.append("Fix the errors above and recompile.\n");
                        statusLabel.setText(" Compilation Failed — Errors Found");
                    } else {
                        // outputArea.setText("═══════════════════════════════════════\n");
                        outputArea.append("COMPILATION SUCCESSFUL\n");
                        // outputArea.append("═══════════════════════════════════════\n\n");
                        outputArea.append("All phases completed without errors.\n\n");
                        outputArea.append("  Lexical Analysis   ---- [DONE]\n");
                        outputArea.append("  Syntax Analysis    ---- [DONE]\n");
                        outputArea.append("  Semantic Analysis  ---- [DONE]\n\n");
                        outputArea.append(
                                "Tokens:       " + tokens.toString().trim().split("\n").length + " tokens generated\n");
                        outputArea.append("Symbols:      " + symbolTableModel.getRowCount() + " symbols in table\n");
                        // outputArea.append("───────────────────────────────────────\n");
                        statusLabel.setText("Compilation Successful");
                    }

                    // Auto-switch to Output tab so user sees the result first
                    outputTabs.setSelectedIndex(0);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Execution Error!");
                    // outputArea.setText("═══════════════════════════════════════\n");
                    outputArea.append("EXECUTION ERROR\n");
                    // outputArea.append("═══════════════════════════════════════\n\n");
                    outputArea.append(ex.getMessage() + "\n");
                    outputTabs.setSelectedIndex(0);
                });
            }
        }).start();
    }

    // Helper: map Tab key to 4 spaces in editor
    private static class TabToSpaceFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            super.insertString(fb, offset, string.replaceAll("\t", "    "), attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            super.replace(fb, offset, length, text.replaceAll("\t", "    "), attrs);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> {
            MiniLangIDE ide = new MiniLangIDE();
            ide.setVisible(true);
        });
    }
}
