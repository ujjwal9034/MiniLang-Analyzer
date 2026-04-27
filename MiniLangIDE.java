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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniLangIDE extends JFrame {
    private JTextPane editor;
    private JTextArea lineNumbers;
    private JTextPane outputArea;
    private JTextArea parseTreeArea; // kept for legacy ref, replaced by panel below
    private GraphicalTreePanel graphTreePanel;
    private JTable tokenTable, symbolTable, tacTable, lexSummaryTable, grammarTable;
    private DefaultTableModel tokenTableModel, symbolTableModel, tacTableModel, lexSummaryModel, grammarTableModel;
    private JTabbedPane outputTabs;
    private JLabel statusLabel;
    private JPanel topPanel, toolbar, phasePanel;
    private JLabel[] phaseLabels;
    private JLabel titleLbl;
    private File currentFile = null;
    private boolean isDarkTheme = false;

    // Dark Theme Colors
    private Color bgDark = new Color(22, 22, 30);
    private Color fgDark = new Color(220, 220, 230);
    private Color gutterDark = new Color(30, 30, 40);
    private Color navDark = new Color(28, 28, 38);
    private Color panelDark = new Color(32, 32, 44);
    private Color accentBlue = new Color(99, 130, 255);
    private Color accentGreen = new Color(80, 220, 140);
    private Color accentRed = new Color(255, 85, 85);
    private Color accentOrange = new Color(255, 180, 60);
    private Color accentPurple = new Color(180, 120, 255);
    // Light Theme Colors
    private Color bgLight = new Color(250, 250, 255);
    private Color fgLight = new Color(30, 30, 40);
    private Color gutterLight = new Color(235, 235, 245);
    private Color navLight = new Color(240, 240, 248);

    // Syntax Highlighting Colors
    private final Color COLOR_KEYWORD = new Color(86, 156, 214); // Blue
    private final Color COLOR_STRING = new Color(206, 145, 120); // Orange/Brown
    private final Color COLOR_COMMENT = new Color(106, 153, 85); // Green
    private final Color COLOR_TYPE = new Color(78, 201, 176); // Teal
    private final Color COLOR_NUMBER = new Color(181, 206, 168); // Light Green

    public MiniLangIDE() {
        setTitle("⚡ MiniLang Analyzer IDE");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("TabbedPane.selectedForeground", accentBlue);
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
        setupUI();
        applyTheme();
        bindShortcuts();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // ===== TOP: Modern Toolbar & Header =====
        topPanel = new JPanel(new BorderLayout());

        titleLbl = new JLabel("MiniLang Analyzer", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(accentBlue);
        titleLbl.setBorder(new EmptyBorder(10, 0, 0, 0));

        toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        toolbar.setBorder(new EmptyBorder(2, 8, 8, 8));
        JButton btnOpen = createToolbarButton("\uD83D\uDCC2 Open", null, null);
        JButton btnSave = createToolbarButton("\uD83D\uDCBE Save", null, null);
        JButton btnCompile = createToolbarButton("▶ Compile & Run", new Color(60, 140, 255), Color.WHITE);
        JButton btnClear = createToolbarButton("\uD83D\uDDD1 Clear", null, null);
        JButton btnTheme = createToolbarButton("\uD83C\uDF13 Theme", null, null);

        btnOpen.addActionListener(e -> openFile());
        btnSave.addActionListener(e -> saveFile());
        btnCompile.addActionListener(e -> compileCode());
        btnClear.addActionListener(e -> clearOutputs());
        btnTheme.addActionListener(e -> toggleTheme());

        toolbar.add(btnOpen);
        toolbar.add(btnSave);
        toolbar.add(createSep());
        toolbar.add(btnCompile);
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnTheme);

        topPanel.add(titleLbl, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // ===== LEFT: Code Editor =====
        editor = new JTextPane();
        editor.setFont(new Font("Consolas", Font.PLAIN, 15));
        editor.setCaretColor(Color.WHITE);
        ((AbstractDocument) editor.getDocument()).setDocumentFilter(new TabToSpaceFilter());
        lineNumbers = new JTextArea("1");
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 15));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(new EmptyBorder(0, 10, 0, 10));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateEditor();
            }

            public void removeUpdate(DocumentEvent e) {
                updateEditor();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setRowHeaderView(lineNumbers);
        editorScroll.setBorder(null);
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel fileLabel = new JLabel("  \uD83D\uDCDD Code Editor");
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fileLabel.setForeground(accentBlue);
        fileLabel.setBorder(new EmptyBorder(6, 4, 6, 0));
        leftPanel.add(fileLabel, BorderLayout.NORTH);
        leftPanel.add(editorScroll, BorderLayout.CENTER);

        // ===== RIGHT: Tabbed Output =====
        outputTabs = new JTabbedPane();
        outputTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Output
        outputArea = new JTextPane();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setEditable(false);
        outputTabs.addTab("\uD83D\uDCE4 Output", new JScrollPane(outputArea));

        // Tab 2: Tokens TABLE
        tokenTableModel = createTableModel(new String[] { "#", "Token Type", "Lexeme", "Category" });
        tokenTable = createStyledTable(tokenTableModel);
        outputTabs.addTab("\uD83C\uDFF7 Tokens", new JScrollPane(tokenTable));

        // Tab 3: Lexical Summary TABLE
        lexSummaryModel = createTableModel(new String[] { "Category", "Count", "Details" });
        lexSummaryTable = createStyledTable(lexSummaryModel);
        outputTabs.addTab("\uD83D\uDCCA Lexical Summary", new JScrollPane(lexSummaryTable));

        // Tab 4: Parse Tree
        graphTreePanel = new GraphicalTreePanel();
        outputTabs.addTab("\uD83C\uDF33 Parse Tree", graphTreePanel);

        // Tab 5: Symbol Table
        symbolTableModel = createTableModel(new String[] { "#", "Name", "Type", "Custom Type", "Scope" });
        symbolTable = createStyledTable(symbolTableModel);
        outputTabs.addTab("\uD83D\uDCCB Symbol Table", new JScrollPane(symbolTable));

        // Tab 6: TAC TABLE
        tacTableModel = createTableModel(new String[] { "Line", "Instruction", "Type" });
        tacTable = createStyledTable(tacTableModel);
        outputTabs.addTab("\u2699 TAC", new JScrollPane(tacTable));

        // Tab 7: Grammar Reference TABLE
        grammarTableModel = createTableModel(new String[] { "#", "Production Rule", "Description" });
        grammarTable = createStyledTable(grammarTableModel);
        populateGrammar();
        outputTabs.addTab("\uD83D\uDCD6 Grammar", new JScrollPane(grammarTable));

        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel outLabel = new JLabel("  ⚙ Compiler Output Phases");
        outLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        outLabel.setForeground(accentPurple);
        outLabel.setBorder(new EmptyBorder(6, 4, 6, 0));
        rightPanel.add(outLabel, BorderLayout.NORTH);
        rightPanel.add(outputTabs, BorderLayout.CENTER);

        // ===== SplitPane =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(560);
        split.setResizeWeight(0.4);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // ===== BOTTOM: Status Bar with Phase Indicators =====
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(4, 10, 4, 10));
        statusLabel = new JLabel(" ✦ Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);
        phasePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        String[] phases = { "Lexer", "Parser", "Semantic", "TAC" };
        phaseLabels = new JLabel[phases.length];
        for (int i = 0; i < phases.length; i++) {
            phaseLabels[i] = new JLabel("● " + phases[i]);
            phaseLabels[i].setFont(new Font("Segoe UI", Font.BOLD, 11));
            phaseLabels[i].setForeground(Color.GRAY);
            phasePanel.add(phaseLabels[i]);
        }
        statusBar.add(phasePanel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JComponent createSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(2, 28));
        return s;
    }

    private DefaultTableModel createTableModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Consolas", Font.PLAIN, 13));
        t.setRowHeight(30);
        t.setShowGrid(true);
        t.setGridColor(new Color(50, 50, 65));
        t.setIntercellSpacing(new Dimension(1, 1));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer ctr = new DefaultTableCellRenderer();
        ctr.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(ctr);
        return t;
    }

    private void populateGrammar() {
        String[][] rules = {
                { "1", "Program → Statement*", "Entry point of program" },
                { "2", "Statement → Declaration | Assignment | Print | Input | If | While", "All statement types" },
                { "3", "Declaration → ank IDENTIFIER ;", "Integer variable declaration (Custom)" },
                { "4", "Declaration → dashmlav IDENTIFIER ;", "Float variable declaration (Custom)" },
                { "5", "Declaration → akshar IDENTIFIER ;", "Char/String declaration (Custom)" },
                { "6", "Assignment → IDENTIFIER = Expression ;", "Variable assignment" },
                { "7", "Print → bol IDENTIFIER ;", "Print statement (Custom for 'speak')" },
                { "8", "Input → suno IDENTIFIER ;", "Input statement (Custom for 'listen')" },
                { "9", "If → agar ( Expr ) Stmt [naito Stmt]", "If-else ('agar'=if, 'naito'=else)" },
                { "10", "While → jabtak ( Expr ) Stmt", "While loop ('jabtak'=until)" },
                { "11", "Expression → Expr Op Expr | ID | NUM", "Binary expression or terminal" },
                { "12", "Operator → + | - | * | / | % | == | != | < | <= | > | >=", "All operators" },
        };
        for (String[] r : rules)
            grammarTableModel.addRow(r);
    }

    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setEditable(false);
        return area;
    }

    private JButton createToolbarButton(String text, Color overrideBg, Color overrideFg) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setOpaque(true); // Required for Mac OS
        btn.setBorderPainted(false); // Required for Mac OS to show background

        if (overrideBg != null)
            btn.setBackground(overrideBg);
        if (overrideFg != null)
            btn.setForeground(overrideFg);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(accentBlue);
                btn.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                if (overrideBg != null) {
                    btn.setBackground(overrideBg);
                    btn.setForeground(overrideFg);
                } else {
                    btn.setBackground(isDarkTheme ? bgDark : bgLight);
                    btn.setForeground(isDarkTheme ? fgDark : fgLight);
                }
            }
        });
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

            // MiniLang (Custom) keywords
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
        Color headerBg = isDarkTheme ? new Color(40, 40, 55) : new Color(225, 225, 240);
        Color headerFg = isDarkTheme ? new Color(180, 180, 220) : new Color(40, 40, 60);
        Color gridCol = isDarkTheme ? new Color(50, 50, 65) : new Color(210, 210, 220);
        Color selBg = isDarkTheme ? new Color(55, 55, 90) : new Color(184, 207, 229);

        editor.setBackground(bg);
        editor.setForeground(fg);
        editor.setCaretColor(fg);
        lineNumbers.setBackground(gutterBg);
        lineNumbers.setForeground(isDarkTheme ? new Color(100, 100, 120) : Color.DARK_GRAY);

        // Text areas
        graphTreePanel.applyBg(bg);
        outputArea.setBackground(bg);
        outputArea.setForeground(fg);

        // All JTables
        JTable[] tables = { tokenTable, symbolTable, tacTable, lexSummaryTable, grammarTable };
        for (JTable t : tables) {
            t.setBackground(bg);
            t.setForeground(fg);
            t.setSelectionBackground(selBg);
            t.setGridColor(gridCol);
            JTableHeader h = t.getTableHeader();
            h.setBackground(headerBg);
            h.setForeground(headerFg);
        }

        // Panels
        Container[] containers = {
                getContentPane(),
                (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.NORTH),
                (Container) ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH)
        };
        for (Container c : containers) {
            if (c != null)
                c.setBackground(navBg);
        }
        if (phasePanel != null)
            phasePanel.setBackground(navBg);
        if (topPanel != null)
            topPanel.setBackground(navBg);
        if (toolbar != null) {
            toolbar.setBackground(navBg);
            for (Component comp : toolbar.getComponents()) {
                if (comp instanceof JButton) {
                    JButton b = (JButton) comp;
                    // We don't overwrite the Compile button's hardcoded colors
                    if (!b.getText().contains("Compile")) {
                        b.setBackground(bg);
                        b.setForeground(fg);
                    }
                }
            }
        }
        outputTabs.setBackground(navBg);
        outputTabs.setForeground(fg); // Tab text color

        // Deep recursive theming for scroll panes, viewports, and split panes
        setRecursiveTheme(getContentPane(), bg, navBg);
    }

    private void setRecursiveTheme(Component c, Color bg, Color navBg) {
        if (c instanceof JScrollPane) {
            c.setBackground(bg);
            ((JScrollPane) c).getViewport().setBackground(bg);
            ((JScrollPane) c).setBorder(BorderFactory.createLineBorder(navBg, 1));
        } else if (c instanceof JSplitPane) {
            c.setBackground(navBg);
            ((JSplitPane) c).getComponent(0).setBackground(navBg); // leftPanel
            ((JSplitPane) c).getComponent(1).setBackground(navBg); // rightPanel
        } else if (c instanceof JPanel && c.getBackground() != accentBlue) {
            // Only theme generic JPanels that don't have custom backgrounds
            // We'll leave it simple for now and rely on our manual theming for specific
            // panels
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                setRecursiveTheme(child, bg, navBg);
            }
        }
    }

    // ===== OUTPUT HELPERS =====

    private void appendToOutput(String text, Color color, boolean bold) {
        StyledDocument doc = outputArea.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);
        StyleConstants.setFontFamily(attrs, "Consolas");
        StyleConstants.setFontSize(attrs, 14);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // ===== INLINE ERROR HIGHLIGHTING =====

    private void highlightErrorLine(int errorLine) {
        if (errorLine < 1) return;
        StyledDocument doc = editor.getStyledDocument();
        Element root = doc.getDefaultRootElement();
        if (errorLine > root.getElementCount()) return;
        Element lineElem = root.getElement(errorLine - 1);
        int start = lineElem.getStartOffset();
        int end = lineElem.getEndOffset() - 1;
        SimpleAttributeSet highlight = new SimpleAttributeSet();
        StyleConstants.setBackground(highlight, new Color(120, 30, 30));
        doc.setCharacterAttributes(start, Math.max(1, end - start), highlight, false);
        // Scroll editor to the error line
        try {
            editor.setCaretPosition(start);
        } catch (Exception ignored) {}
    }

    private void clearErrorHighlights() {
        StyledDocument doc = editor.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setBackground(normal, isDarkTheme ? bgDark : bgLight);
        doc.setCharacterAttributes(0, doc.getLength(), normal, false);
        SwingUtilities.invokeLater(this::applySyntaxHighlighting);
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
        graphTreePanel.clear();
        tokenTableModel.setRowCount(0);
        symbolTableModel.setRowCount(0);
        tacTableModel.setRowCount(0);
        lexSummaryModel.setRowCount(0);
        for (JLabel pl : phaseLabels)
            pl.setForeground(Color.GRAY);
        statusLabel.setText(" ✦ Output cleared.");
    }

    private String categorizeToken(String type) {
        switch (type) {
            case "KEYWORD":
                return "Keyword";
            case "IDENTIFIER":
                return "Identifier";
            case "NUMBER":
                return "Literal";
            case "STRING_LITERAL":
                return "String Literal";
            case "CHAR_LITERAL":
                return "Char Literal";
            case "SEMICOLON":
            case "LPAREN":
            case "RPAREN":
                return "Punctuation";
            case "ASSIGN":
                return "Assignment";
            default:
                if (type.equals("PLUS") || type.equals("MINUS") || type.equals("MULT") || type.equals("DIV")
                        || type.equals("MOD"))
                    return "Arithmetic Op";
                if (type.equals("EQ") || type.equals("NEQ") || type.equals("LT") || type.equals("LTE")
                        || type.equals("GT") || type.equals("GTE"))
                    return "Relational Op";
                return "Other";
        }
    }

    private void compileCode() {
        statusLabel.setText(" ⏳ Compiling...");
        clearOutputs();
        clearErrorHighlights();
        String code = editor.getText();

        if (code.trim().isEmpty()) {
            statusLabel.setText(" ✗ Compilation failed: Editor is empty.");
            appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
            appendToOutput("  ✗  NO SOURCE CODE\n", accentRed, true);
            appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
            appendToOutput("  Error: Editor is empty.\n", new Color(200, 200, 200), false);
            appendToOutput("  Please write some code and try again.\n", accentOrange, false);
            outputTabs.setSelectedIndex(0);
            return;
        }

        new Thread(() -> {
            try {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("input.txt"))) {
                    writer.write(code);
                } catch (IOException ioe) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(" ✗ File Error");
                        appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
                        appendToOutput("  ✗  FILE ERROR\n", accentRed, true);
                        appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
                        appendToOutput("Error writing input.txt: " + ioe.getMessage() + "\n", new Color(200,200,200), false);
                        outputTabs.setSelectedIndex(0);
                    });
                    return;
                }

                Process p;
                try {
                    p = new ProcessBuilder("./minilang").start();
                } catch (IOException ioe) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(" ✗ Compiler Not Found");
                        appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
                        appendToOutput("  ✗  COMPILER NOT FOUND\n", accentRed, true);
                        appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
                        appendToOutput("Error: The 'minilang' executable was not found.\n", new Color(200,200,200), false);
                        appendToOutput("Make sure you compiled the C code (gcc ... -o minilang)\n", accentOrange, false);
                        outputTabs.setSelectedIndex(0);
                    });
                    return;
                }

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                StringBuilder stdoutBuf = new StringBuilder();
                StringBuilder stderrBuf = new StringBuilder();
                String s;
                while ((s = stdInput.readLine()) != null)
                    stdoutBuf.append(s).append("\n");
                while ((s = stdError.readLine()) != null)
                    stderrBuf.append(s).append("\n");

                boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    p.destroyForcibly();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(" ✗ Compilation Failed: Timeout");
                        appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
                        appendToOutput("  ✗  TIMEOUT ERROR\n", accentRed, true);
                        appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
                        appendToOutput("The compiler took too long (Timeout > 5s).\n", new Color(200,200,200), false);
                        appendToOutput("This usually means an infinite loop in your lexer or parser.\n", accentOrange, false);
                        outputTabs.setSelectedIndex(0);
                    });
                    return;
                }

                int exitCode = p.exitValue();
                String fullOut = stdoutBuf.toString() + stderrBuf.toString();

                StringBuilder tokens = new StringBuilder();
                StringBuilder parseTree = new StringBuilder();
                StringBuilder tacGen = new StringBuilder();
                StringBuilder errors = new StringBuilder();
                StringBuilder symbolEntries = new StringBuilder();

                String[] lines = fullOut.split("\n");
                int currentPhase = 0;
                boolean isErrorBlock = false;
                for (String line : lines) {
                    String trimmed = line.trim();
                    String lower = trimmed.toLowerCase();

                    if (isErrorBlock) {
                        errors.append(line).append("\n");
                        continue;
                    }

                    if (lower.contains("syntax error") || lower.contains("syntax galat")
                            || lower.contains("semantic error") || lower.contains("not declared")
                            || lower.contains("already declared")
                            || lower.contains("lexical error") || lower.contains("unrecognized char")
                            || lower.contains("invalid char") || lower.contains("unclosed string")) {
                        isErrorBlock = true;
                        errors.append(line).append("\n");
                        continue;
                    }
                    if (lower.contains("parsing completed"))
                        continue;
                    if (trimmed.equals("Parse Tree:")) {
                        currentPhase = 1;
                        continue;
                    } else if (trimmed.equals("Symbol Table:")) {
                        currentPhase = 2;
                        continue;
                    } else if (trimmed.equals("Three-Address Code:") || trimmed.equals("Three-Address Code (TAC):")
                            || lower.contains("three-address code")) {
                        currentPhase = 3;
                        continue;
                    }
                    if (trimmed.startsWith("---"))
                        continue;

                    if (currentPhase == 0 && !trimmed.isEmpty())
                        tokens.append(line).append("\n");
                    else if (currentPhase == 1)
                        parseTree.append(line).append("\n");
                    else if (currentPhase == 2 && !trimmed.isEmpty())
                        symbolEntries.append(line).append("\n");
                    else if (currentPhase == 3)
                        tacGen.append(line).append("\n");
                }

                if (exitCode != 0 && errors.length() == 0) {
                    if (stderrBuf.length() > 0)
                        errors.append(stderrBuf.toString());
                    else
                        errors.append("Compiler terminated unexpectedly (exit code ").append(exitCode).append(")\n");
                }

                // Build final strings for EDT (strip ANSI codes)
                final String ansiRegex = "\u001B\\[[;\\d]*m";
                final String tokStr = tokens.toString().replaceAll(ansiRegex, "").trim();
                final String ptStr = parseTree.toString().replaceAll(ansiRegex, "").trim();
                final String tacStr = tacGen.toString().replaceAll(ansiRegex, "").trim();
                final String symStr = symbolEntries.toString().replaceAll(ansiRegex, "").trim();
                final String errStr = errors.toString().replaceAll(ansiRegex, "").trim();

                SwingUtilities.invokeLater(() -> {
                    // --- TOKENS TABLE ---
                    tokenTableModel.setRowCount(0);
                    Map<String, Integer> categoryCounts = new LinkedHashMap<>();
                    Map<String, StringBuilder> categoryDetails = new LinkedHashMap<>();
                    if (!tokStr.isEmpty()) {
                        String[] tokLines = tokStr.split("\n");
                        for (int i = 0; i < tokLines.length; i++) {
                            String[] parts = tokLines[i].split("\\s*:\\s*");
                            if (parts.length >= 2) {
                                String tType = parts[0].trim();
                                String tVal = parts[1].trim();
                                String cat = categorizeToken(tType);
                                tokenTableModel.addRow(new Object[] { i + 1, tType, tVal, cat });
                                categoryCounts.merge(cat, 1, Integer::sum);
                                categoryDetails.computeIfAbsent(cat, k -> new StringBuilder()).append(tVal)
                                        .append(", ");
                            }
                        }
                    }

                    // --- LEXICAL SUMMARY TABLE ---
                    lexSummaryModel.setRowCount(0);
                    int totalTokens = tokenTableModel.getRowCount();
                    lexSummaryModel
                            .addRow(new Object[] { "Total Tokens", totalTokens, "All tokens from lexical analysis" });
                    for (Map.Entry<String, Integer> e : categoryCounts.entrySet()) {
                        String details = categoryDetails.get(e.getKey()).toString();
                        if (details.endsWith(", "))
                            details = details.substring(0, details.length() - 2);
                        if (details.length() > 60)
                            details = details.substring(0, 57) + "...";
                        lexSummaryModel.addRow(new Object[] { e.getKey(), e.getValue(), details });
                    }
                    lexSummaryModel.addRow(new Object[] { "Unique Categories", categoryCounts.size(),
                            categoryCounts.keySet().toString() });

                    boolean hasErrors = errStr.length() > 0;
                    boolean lexicalError = errStr.toLowerCase().contains("lexical")
                            || errStr.toLowerCase().contains("unrecognized")
                            || errStr.toLowerCase().contains("invalid char");
                    boolean syntaxError = errStr.toLowerCase().contains("syntax")
                            || errStr.toLowerCase().contains("galat");
                    boolean semanticError = errStr.toLowerCase().contains("semantic")
                            || errStr.toLowerCase().contains("declar") || errStr.toLowerCase().contains("type");

                    if (hasErrors) {
                        // Clear subsequent phases if compilation failed
                        if (lexicalError) {
                            graphTreePanel.showError("\u274c Lexical error encountered.\nParse tree could not be generated.");
                            symbolTableModel.setRowCount(0);
                            tacTableModel.setRowCount(0);
                            tacTableModel.addRow(new Object[] { "-", "Aborted due to Lexical Error", "Error" });
                        } else if (syntaxError) {
                            graphTreePanel.showError("\u274c Syntax error encountered.\nParse tree could not be generated.");
                            symbolTableModel.setRowCount(0);
                            tacTableModel.setRowCount(0);
                            tacTableModel.addRow(new Object[] { "-", "Aborted due to Syntax Error", "Error" });
                        } else if (semanticError) {
                            graphTreePanel.showError("\u274c Semantic error encountered.");
                            symbolTableModel.setRowCount(0);
                            tacTableModel.setRowCount(0);
                            tacTableModel.addRow(new Object[] { "-", "Aborted due to Semantic Error", "Error" });
                        } else {
                            graphTreePanel.showError("\u274c Compilation error encountered.");
                            symbolTableModel.setRowCount(0);
                            tacTableModel.setRowCount(0);
                        }
                    } else {
                        // --- PARSE TREE ---
                        graphTreePanel.setTreeText(ptStr);

                        // --- TAC TABLE ---
                        tacTableModel.setRowCount(0);
                        if (!tacStr.isEmpty()) {
                            String[] tacLines = tacStr.split("\n");
                            int ln = 1;
                            for (String tl : tacLines) {
                                String t = tl.trim();
                                if (t.isEmpty())
                                    continue;
                                String itype = "Assignment";
                                if (t.startsWith("read"))
                                    itype = "Input";
                                else if (t.startsWith("print"))
                                    itype = "Output";
                                else if (t.startsWith("if "))
                                    itype = "Conditional Jump";
                                else if (t.startsWith("ifFalse"))
                                    itype = "Conditional Jump";
                                else if (t.startsWith("goto"))
                                    itype = "Unconditional Jump";
                                else if (t.startsWith("L") && t.endsWith(":"))
                                    itype = "Label";
                                else if (t.startsWith("//"))
                                    itype = "Comment";
                                tacTableModel.addRow(new Object[] { ln++, t, itype });
                            }
                        }

                        // --- SYMBOL TABLE ---
                        symbolTableModel.setRowCount(0);
                        if (!symStr.isEmpty()) {
                            String[] symLines = symStr.split("\n");
                            for (String symLine : symLines) {
                                symLine = symLine.trim();
                                if (symLine.isEmpty())
                                    continue;
                                String[] parts = symLine.split("\\s*:\\s*");
                                if (parts.length >= 3) {
                                    String idx = parts[0].trim();
                                    String name = parts[1].trim();
                                    String raw = parts[2].trim();
                                    String type = raw;
                                    if (raw.equals("ank"))
                                        type = "Integer";
                                    else if (raw.equals("dashmlav"))
                                        type = "Float";
                                    else if (raw.equals("akshar"))
                                        type = "Char/String";
                                    symbolTableModel.addRow(new Object[] { idx, name, type, raw, "Global" });
                                }
                            }
                        }
                    }

                    // --- OUTPUT TAB ---
                    // Extract error line number for inline highlighting
                    int errorLineNum = -1;
                    if (hasErrors) {
                        Matcher lm = Pattern.compile("Line\\s*:\\s*(\\d+)").matcher(errStr);
                        boolean found = lm.find();
                        if (!found) { lm = Pattern.compile("[Aa]t line (\\d+)").matcher(errStr); found = lm.find(); }
                        if (found) { try { errorLineNum = Integer.parseInt(lm.group(1)); } catch (NumberFormatException ignored) {} }
                        if (errorLineNum < 1) {
                            Matcher lm2 = Pattern.compile("[Ll]ine (\\d+)").matcher(errStr);
                            if (lm2.find()) { try { errorLineNum = Integer.parseInt(lm2.group(1)); } catch (NumberFormatException ignored) {} }
                        }
                    }
                    final int finalErrLine = errorLineNum;

                    Color dimWhite = isDarkTheme ? new Color(180, 180, 200) : new Color(60, 60, 80);
                    Color dividerColor = isDarkTheme ? new Color(70, 70, 100) : new Color(160, 160, 190);

                    if (hasErrors) {
                        appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
                        appendToOutput("  ✗  COMPILATION FAILED\n", accentRed, true);
                        appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
                        appendToOutput("  Phase                    Status\n", dimWhite, true);
                        appendToOutput("  ─────────────────────    ──────\n", dividerColor, false);
                        if (lexicalError) {
                            appendToOutput("  ① Lexical Analysis   ", dimWhite, false);
                            appendToOutput("  ✗ FAILED\n", accentRed, true);
                            appendToOutput("  ② Syntax Analysis    ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n", Color.GRAY, false);
                            appendToOutput("  ③ Semantic Analysis  ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n", Color.GRAY, false);
                            appendToOutput("  ④ TAC Generation     ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n\n", Color.GRAY, false);
                            phaseLabels[0].setForeground(accentRed);
                        } else if (syntaxError) {
                            appendToOutput("  ① Lexical Analysis   ", dimWhite, false);
                            appendToOutput("  ✓ DONE\n", accentGreen, true);
                            appendToOutput("  ② Syntax Analysis    ", dimWhite, false);
                            appendToOutput("  ✗ FAILED\n", accentRed, true);
                            appendToOutput("  ③ Semantic Analysis  ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n", Color.GRAY, false);
                            appendToOutput("  ④ TAC Generation     ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n\n", Color.GRAY, false);
                            phaseLabels[0].setForeground(accentGreen);
                            phaseLabels[1].setForeground(accentRed);
                        } else if (semanticError) {
                            appendToOutput("  ① Lexical Analysis   ", dimWhite, false);
                            appendToOutput("  ✓ DONE\n", accentGreen, true);
                            appendToOutput("  ② Syntax Analysis    ", dimWhite, false);
                            appendToOutput("  ✓ DONE\n", accentGreen, true);
                            appendToOutput("  ③ Semantic Analysis  ", dimWhite, false);
                            appendToOutput("  ✗ FAILED\n", accentRed, true);
                            appendToOutput("  ④ TAC Generation     ", dimWhite, false);
                            appendToOutput("  - SKIPPED\n\n", Color.GRAY, false);
                            phaseLabels[0].setForeground(accentGreen);
                            phaseLabels[1].setForeground(accentGreen);
                            phaseLabels[2].setForeground(accentRed);
                        } else {
                            appendToOutput("  Compilation failed at unknown phase.\n\n", accentRed, false);
                            phaseLabels[0].setForeground(accentRed);
                        }
                        appendToOutput("─────────────────────────────────────────\n", dividerColor, false);
                        appendToOutput("  Error Details:\n", accentOrange, true);
                        // Print each error line with appropriate color
                        for (String el : errStr.split("\n")) {
                            String etrim = el.trim();
                            if (etrim.startsWith("Line") || etrim.startsWith("Expected") || etrim.startsWith("Found") || etrim.startsWith("Source")) {
                                appendToOutput("  " + el + "\n", dimWhite, false);
                            } else if (etrim.startsWith("Hint") || etrim.contains("Hint") || etrim.startsWith("💡")) {
                                appendToOutput("  " + el + "\n", accentOrange, false);
                            } else if (!etrim.isEmpty()) {
                                appendToOutput("  " + el + "\n", accentRed, false);
                            }
                        }
                        if (finalErrLine > 0) {
                            appendToOutput("\n  ⮕ Error on line " + finalErrLine + " (highlighted in editor)\n", accentOrange, true);
                        }
                        appendToOutput("─────────────────────────────────────────\n", dividerColor, false);
                        appendToOutput("  Fix the error above and recompile.\n", Color.GRAY, false);
                        statusLabel.setText(" ✗ Compilation Failed" + (finalErrLine > 0 ? " (line " + finalErrLine + ")" : ""));
                        if (finalErrLine > 0) highlightErrorLine(finalErrLine);
                    } else {
                        appendToOutput("╔═══════════════════════════════════════╗\n", accentGreen, true);
                        appendToOutput("  ✓  COMPILATION SUCCESSFUL\n", accentGreen, true);
                        appendToOutput("╚═══════════════════════════════════════╝\n\n", accentGreen, true);
                        appendToOutput("  Phase                    Status\n", dimWhite, true);
                        appendToOutput("  ─────────────────────    ──────\n", dividerColor, false);
                        appendToOutput("  ① Lexical Analysis   ", dimWhite, false);
                        appendToOutput("  ✓ DONE\n", accentGreen, true);
                        appendToOutput("  ② Syntax Analysis    ", dimWhite, false);
                        appendToOutput("  ✓ DONE\n", accentGreen, true);
                        appendToOutput("  ③ Semantic Analysis  ", dimWhite, false);
                        appendToOutput("  ✓ DONE\n", accentGreen, true);
                        appendToOutput("  ④ TAC Generation     ", dimWhite, false);
                        appendToOutput("  ✓ DONE\n\n", accentGreen, true);
                        appendToOutput("─────────────────────────────────────────\n", dividerColor, false);
                        appendToOutput("  Summary:\n", accentBlue, true);
                        appendToOutput("  • Tokens generated:   ", dimWhite, false);
                        appendToOutput(totalTokens + "\n", accentOrange, true);
                        appendToOutput("  • Symbols in table:   ", dimWhite, false);
                        appendToOutput(symbolTableModel.getRowCount() + "\n", accentOrange, true);
                        appendToOutput("  • TAC instructions:   ", dimWhite, false);
                        appendToOutput(tacTableModel.getRowCount() + "\n", accentOrange, true);
                        appendToOutput("  • Token categories:   ", dimWhite, false);
                        appendToOutput(categoryCounts.size() + "\n", accentOrange, true);
                        appendToOutput("─────────────────────────────────────────\n", dividerColor, false);
                        statusLabel.setText(" ✓ Compilation Successful");
                        for (JLabel pl : phaseLabels)
                            pl.setForeground(accentGreen);
                    }
                    outputTabs.setSelectedIndex(0);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(" ✗ Execution Error!");
                    appendToOutput("╔═══════════════════════════════════════╗\n", accentRed, true);
                    appendToOutput("  EXECUTION ERROR\n", accentRed, true);
                    appendToOutput("╚═══════════════════════════════════════╝\n\n", accentRed, true);
                    appendToOutput(ex.getMessage() + "\n", new Color(200, 200, 200), false);
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

    // =====================================================================
    //  GRAPHICAL PARSE TREE PANEL
    // =====================================================================
    static class GraphicalTreePanel extends JPanel {

        // ---- Layout constants ----
        private static final int NW   = 112; // node box width
        private static final int NH   = 34;  // node box height
        private static final int HGAP = 20;  // min horizontal gap between siblings
        private static final int VGAP = 58;  // vertical gap between levels

        // ---- Node model ----
        static class GNode {
            String label;
            int x, y;
            java.util.List<GNode> children = new java.util.ArrayList<>();
            GNode(String l) { label = l; }
        }

        private GNode   root;
        private String  errorMsg;
        private double  zoom      = 1.0;
        private int     panX      = 0, panY = 0;
        private boolean centered  = false;
        private Point   dragStart = null;

        GraphicalTreePanel() {
            setOpaque(true);
            setBackground(new Color(22, 22, 30));
            setToolTipText("Scroll = Zoom | Drag = Pan");

            addMouseWheelListener(e -> {
                zoom = Math.max(0.2, Math.min(3.5, zoom - e.getPreciseWheelRotation() * 0.09));
                repaint();
            });
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e)  { dragStart = e.getPoint(); }
                public void mouseDragged(MouseEvent e)  {
                    if (dragStart != null) {
                        panX += e.getX() - dragStart.x;
                        panY += e.getY() - dragStart.y;
                        dragStart = e.getPoint();
                        repaint();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        // ---- Public API ----
        void setTreeText(String text) {
            errorMsg = null;
            root = parseAscii(text);
            if (root != null) doLayout(root, 0, 0);
            centered = false;
            zoom = 1.0;
            repaint();
        }

        void showError(String msg) {
            root = null;
            errorMsg = msg;
            repaint();
        }

        void clear() {
            root = null;
            errorMsg = null;
            centered = false;
            repaint();
        }

        void applyBg(Color bg) {
            setBackground(bg);
            repaint();
        }

        // ---- ASCII parser ----
        private GNode parseAscii(String text) {
            if (text == null || text.trim().isEmpty()) return null;
            String[] lines = text.split("\n");
            GNode[] stack = new GNode[256];
            int[] sdepth  = new int[256];
            int sp = 0;
            GNode rootNode = null;
            for (String raw : lines) {
                String line = raw.replaceAll("\r", "");
                if (line.trim().isEmpty()) continue;
                int depth = calcDepth(line);
                String label = extractLabel(line);
                if (label.isEmpty()) continue;
                GNode node = new GNode(label);
                if (depth == 0) {
                    rootNode = node;
                    stack[0] = node; sdepth[0] = 0; sp = 1;
                } else {
                    while (sp > 1 && sdepth[sp - 1] >= depth) sp--;
                    if (sp > 0) stack[sp - 1].children.add(node);
                    stack[sp] = node; sdepth[sp] = depth; sp++;
                }
            }
            return rootNode;
        }

        private int calcDepth(String line) {
            int pos = 0;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\u2502' || c == '\u251C' || c == '\u2514' || c == '\u2500' || c == ' ') pos++;
                else break;
            }
            return pos / 4;
        }

        private String extractLabel(String line) {
            return line.replaceAll("^[\u2502\u251C\u2514\u2500\\s]+", "").trim();
        }

        // ---- Reingold-style layout (no overlap) ----
        // Returns the right x-boundary after placing node's whole subtree
        private int doLayout(GNode node, int depth, int leftBound) {
            node.y = depth * (NH + VGAP);
            if (node.children.isEmpty()) {
                node.x = leftBound + NW / 2;
                return leftBound + NW + HGAP;
            }
            int childLeft = leftBound;
            int firstX = Integer.MAX_VALUE, lastX = Integer.MIN_VALUE;
            for (GNode child : node.children) {
                childLeft = doLayout(child, depth + 1, childLeft);
                firstX = Math.min(firstX, child.x);
                lastX  = Math.max(lastX,  child.x);
            }
            node.x = (firstX + lastX) / 2;
            return childLeft;
        }

        // ---- Paint ----
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            if (root == null && errorMsg == null) { drawPlaceholder(g2); return; }
            if (errorMsg != null)                  { drawError(g2);       return; }

            // Auto-center on first render
            if (!centered && getWidth() > 50) {
                panX = getWidth() / 2 - root.x;
                panY = 30;
                centered = true;
            }

            // Hint bar (screen space, before transform)
            g2.setColor(new Color(60, 60, 90));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString(String.format("Zoom: %.0f%%   |   Scroll wheel = Zoom   |   Drag = Pan", zoom * 100),
                          10, getHeight() - 8);

            // Apply pan + zoom
            java.awt.geom.AffineTransform orig = g2.getTransform();
            g2.translate(panX, panY);
            g2.scale(zoom, zoom);

            drawEdges(g2, root);
            drawNodes(g2, root);

            g2.setTransform(orig);
        }

        private void drawPlaceholder(Graphics2D g2) {
            g2.setColor(new Color(70, 70, 100));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            String msg = "No parse tree — compile a program first";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }

        private void drawError(Graphics2D g2) {
            String[] parts = errorMsg.split("\n");
            int total = parts.length * 22;
            int startY = getHeight() / 2 - total / 2;
            for (int i = 0; i < parts.length; i++) {
                g2.setColor(i == 0 ? new Color(255, 85, 85) : new Color(160, 160, 180));
                g2.setFont(new Font("Segoe UI", i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 15 : 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(parts[i], (getWidth() - fm.stringWidth(parts[i])) / 2, startY + i * 24);
            }
        }

        // Draw bezier curves from parent to each child
        private void drawEdges(Graphics2D g2, GNode node) {
            g2.setColor(new Color(90, 90, 135));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (GNode child : node.children) {
                int x1 = node.x,  y1 = node.y + NH;
                int x2 = child.x, y2 = child.y;
                int cy = (y1 + y2) / 2;
                java.awt.geom.GeneralPath p = new java.awt.geom.GeneralPath();
                p.moveTo(x1, y1);
                p.curveTo(x1, cy, x2, cy, x2, y2);
                g2.draw(p);
                drawEdges(g2, child);
            }
        }

        // Draw colored rounded boxes with labels
        private void drawNodes(Graphics2D g2, GNode node) {
            int x = node.x - NW / 2;
            int y = node.y;

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRoundRect(x + 3, y + 3, NW, NH, 12, 12);

            // Box fill
            Color bg = nodeColor(node.label);
            g2.setColor(bg);
            g2.fillRoundRect(x, y, NW, NH, 12, 12);

            // Subtle top sheen
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillRoundRect(x, y, NW, NH / 2, 12, 12);

            // Border
            g2.setStroke(new BasicStroke(1.3f));
            g2.setColor(bg.brighter());
            g2.drawRoundRect(x, y, NW, NH, 12, 12);

            // Label — shrink font if too wide
            g2.setColor(Color.WHITE);
            Font font = new Font("Segoe UI", Font.BOLD, 12);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            String lbl = node.label;
            if (fm.stringWidth(lbl) > NW - 10) {
                font = new Font("Segoe UI", Font.BOLD, 10);
                g2.setFont(font);
                fm = g2.getFontMetrics(font);
            }
            // Truncate with ellipsis if still too wide
            while (fm.stringWidth(lbl) > NW - 8 && lbl.length() > 2)
                lbl = lbl.substring(0, lbl.length() - 1);
            if (!lbl.equals(node.label)) lbl += "\u2026";

            int tx = x + (NW - fm.stringWidth(lbl)) / 2;
            int ty = y + (NH + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(lbl, tx, ty);

            for (GNode child : node.children) drawNodes(g2, child);
        }

        // Color palette by node type
        private Color nodeColor(String label) {
            switch (label) {
                case "Program":     return new Color(30,  85, 200);
                case "Declaration": return new Color(18, 148, 148);
                case "Assignment":  return new Color(108, 48, 192);
                case "Input":       return new Color(195, 108, 18);
                case "Print":       return new Color(38,  158, 78);
                case "If":          return new Color(195, 148, 12);
                case "Else":        return new Color(165, 112, 12);
                case "While":       return new Color(192,  42, 102);
                case "Condition":   return new Color(52,  108, 205);
                default:
                    // Operators
                    if (label.matches("[+\\-*/]|==|!=|<=|>=|<|>")) return new Color(200, 55, 55);
                    // Numbers
                    if (label.matches("\\d+(\\.\\d+)?"))           return new Color(48,  130, 68);
                    // Identifiers / leaves
                    return new Color(55, 55, 88);
            }
        }
    }
}

