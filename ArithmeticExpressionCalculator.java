import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Stack;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ArithmeticExpressionCalculator {

    // TOKEN TYPES
    enum TokenType {
        NUMBER,
        OPERATOR,
        LEFT_PAREN,
        RIGHT_PAREN
    }


    // TOKEN CLASS
    static class Token {

        TokenType type;
        String lexeme;
        int position;

        Token(TokenType type, String lexeme, int position) {
            this.type = type;
            this.lexeme = lexeme;
            this.position = position;
        }

        @Override
        public String toString() {
            return String.format("%s %s %d", type, lexeme, position);
        }
    }


    // TOKENIZER
    public static ArrayList<Token> tokenize(String expression) {

        ArrayList<Token> tokens = new ArrayList<>();

        int i = 0;

        while (i < expression.length()) {

            char ch = expression.charAt(i);

            // Ignore spaces
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }


            // NUMBER
            if (Character.isDigit(ch) || ch == '.') {

                int start = i;

                StringBuilder number = new StringBuilder();

                boolean decimalFound = false;

                while (i < expression.length()) {

                    ch = expression.charAt(i);

                    if (Character.isDigit(ch)) {

                        number.append(ch);

                    } else if (ch == '.') {

                        if (decimalFound) {
                            throw new RuntimeException(
                                    "Invalid Number : Multiple decimal points at position " + i);
                        }

                        decimalFound = true;
                        number.append(ch);

                    } else {
                        break;
                    }

                    i++;
                }

                // Reject "."
                if (number.toString().equals(".")) {
                    throw new RuntimeException(
                            "Invalid Number at position " + start);
                }

                // Convert ".5" -> "0.5"
                if (number.charAt(0) == '.') {
                    number.insert(0, '0');
                }

                // Convert "5." -> "5.0"
                if (number.charAt(number.length() - 1) == '.') {
                    number.append('0');
                }

                tokens.add(new Token(
                        TokenType.NUMBER,
                        number.toString(),
                        start));

                continue;
            }
            // OPERATORS

            if (ch == '+' || ch == '-' ||
                    ch == '*' || ch == '/' ||
                    ch == '%' || ch == '^') {

                tokens.add(new Token(
                        TokenType.OPERATOR,
                        String.valueOf(ch),
                        i));

                i++;
                continue;
            }
            // LEFT PAREN

            if (ch == '(') {

                tokens.add(new Token(
                        TokenType.LEFT_PAREN,
                        "(",
                        i));

                i++;
                continue;
            }

            // RIGHT PAREN

            if (ch == ')') {

                tokens.add(new Token(
                        TokenType.RIGHT_PAREN,
                        ")",
                        i));

                i++;
                continue;
            }

            // INVALID CHARACTER

            throw new RuntimeException(
                    "Invalid Character '" + ch +
                            "' at position " + i);
        }

        return tokens;
    }

    // PRINT TOKENS

    public static void printTokens(ArrayList<Token> tokens) {
        System.out.printf("%s %s %s %s%n",
                "Index", "Type", "Lexeme", "Position");

        for (int i = 0; i < tokens.size(); i++) {

            Token t = tokens.get(i);
            System.out.printf("%d %s %s %d",
                    i,
                    t.type,
                    t.lexeme,
                    t.position);
        }
    }

    // VALIDATOR (FSM)

    public static boolean validateTokens(ArrayList<Token> tokens) {
        return validateTokens(tokens, null);
    }

    public static boolean validateTokens(ArrayList<Token> tokens, StringBuilder errorOut) {

        boolean expectNumber = true;

        int balance = 0;

        for (Token token : tokens) {

            if (expectNumber) {

                // Expect NUMBER or (

                if (token.type == TokenType.NUMBER) {

                    expectNumber = false;

                } else if (token.type == TokenType.LEFT_PAREN) {

                    balance++;

                } else {

                    String msg = "Error : Expected NUMBER but found " + token.lexeme;
                    System.out.println(msg);
                    if (errorOut != null) errorOut.append(msg);
                    return false;

                }

            } else {

                // Expect OPERATOR or )

                if (token.type == TokenType.OPERATOR) {

                    expectNumber = true;

                } else if (token.type == TokenType.RIGHT_PAREN) {

                    balance--;

                    if (balance < 0) {

                        String msg = "Error : Extra ')' ";
                        System.out.println(msg);
                        if (errorOut != null) errorOut.append(msg);
                        return false;
                    }

                } else {

                    String msg = "Error : Expected OPERATOR but found " + token.lexeme;
                    System.out.println(msg);
                    if (errorOut != null) errorOut.append(msg);
                    return false;

                }

            }

        }

        if (balance != 0) {

            String msg = "Error : Parentheses not balanced";
            System.out.println(msg);
            if (errorOut != null) errorOut.append(msg);
            return false;

        }

        if (expectNumber) {

            String msg = "Error : Expression cannot end with operator";
            System.out.println(msg);
            if (errorOut != null) errorOut.append(msg);
            return false;

        }

        return true;

    }

    public static int precedence(String op) {

        switch (op) {

            case "+":
            case "-":
                return 1;

            case "*":
            case "/":
            case "%":
                return 2;

            case "^":
                return 3;
        }

        return 0;
    }

    public static boolean isRightAssociative(String op) {

        return op.equals("^");

    }

    public static double apply(double a, double b, String op) {

        switch (op) {

            case "+":
                return a + b;

            case "-":
                return a - b;

            case "*":
                return a * b;

            case "/":
                return a / b;

            case "%":
                return a % b;

            case "^":
                return Math.pow(a, b);
        }

        throw new RuntimeException("Unknown Operator");
    }

    public static double evaluate(ArrayList<Token> tokens) {

        Stack<Double> operands = new Stack<>();

        Stack<String> operators = new Stack<>();

        for (Token token : tokens) {

            // NUMBER

            if (token.type == TokenType.NUMBER) {

                operands.push(Double.parseDouble(token.lexeme));
            }

            // (

            else if (token.type == TokenType.LEFT_PAREN) {

                operators.push("(");

            }

            // )

            else if (token.type == TokenType.RIGHT_PAREN) {

                while (!operators.peek().equals("(")) {

                    double b = operands.pop();
                    double a = operands.pop();

                    String op = operators.pop();

                    operands.push(apply(a, b, op));
                }

                operators.pop();

            }

            // OPERATOR

            else {

                while (!operators.isEmpty()
                        && !operators.peek().equals("(")
                        &&

                        (

                                precedence(operators.peek())
                                        >
                                        precedence(token.lexeme)

                                        ||

                                        (

                                                precedence(operators.peek())
                                                        ==
                                                        precedence(token.lexeme)

                                                        &&

                                                        !isRightAssociative(token.lexeme)

                                        )

                        )

                ) {

                    double b = operands.pop();

                    double a = operands.pop();

                    String op = operators.pop();

                    operands.push(apply(a, b, op));

                }

                operators.push(token.lexeme);

            }

        }

        while (!operators.isEmpty()) {

            double b = operands.pop();

            double a = operands.pop();

            String op = operators.pop();

            operands.push(apply(a, b, op));

        }

        return operands.pop();

    }

    // GUI
    
    static class CalculatorGUI extends JFrame {

        private final JTextField display;
        private final JTextArea historyArea;

        CalculatorGUI() {

            super("Arithmetic Expression Calculator");

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(420, 620);
            setMinimumSize(new Dimension(380, 560));
            setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(new EmptyBorder(12, 12, 12, 12));
            setContentPane(root);

            //  Display 
            display = new JTextField();
            display.setFont(new Font("Consolas", Font.PLAIN, 26));
            display.setHorizontalAlignment(JTextField.RIGHT);
            display.setPreferredSize(new Dimension(100, 55));
            root.add(display, BorderLayout.NORTH);

            //  History / result area 
            historyArea = new JTextArea();
            historyArea.setEditable(false);
            historyArea.setFont(new Font("Consolas", Font.PLAIN, 14));
            historyArea.setLineWrap(true);
            historyArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(historyArea);
            scrollPane.setPreferredSize(new Dimension(100, 140));
            root.add(scrollPane, BorderLayout.CENTER);

            // Buttons 
            JPanel buttonPanel = buildButtonPanel();
            root.add(buttonPanel, BorderLayout.SOUTH);

            getRootPane().setDefaultButton(null);

            // Allow typing directly + Enter key to evaluate
            display.addActionListener(e -> evaluateExpression());
        }

        private JPanel buildButtonPanel() {

            String[] buttonLabels = {
                    "(", ")", "%", "C",
                    "7", "8", "9", "/",
                    "4", "5", "6", "*",
                    "1", "2", "3", "-",
                    "0", ".", "^", "+",
                    "\u2190", "CE", "=", ""
            };

            JPanel panel = new JPanel(new GridLayout(6, 4, 6, 6));

            for (String label : buttonLabels) {

                if (label.isEmpty()) {
                    panel.add(new JLabel(""));
                    continue;
                }

                JButton button = new JButton(label);
                button.setFont(new Font("SansSerif", Font.PLAIN, 18));
                button.setFocusPainted(false);

                switch (label) {

                    case "=":
                        button.setBackground(new Color(76, 175, 80));
                        button.setForeground(Color.WHITE);
                        button.addActionListener(this::onEquals);
                        break;

                    case "C":
                        button.setBackground(new Color(244, 67, 54));
                        button.setForeground(Color.WHITE);
                        button.addActionListener(e -> {
                            display.setText("");
                            historyArea.setText("");
                        });
                        break;

                    case "CE":
                        button.addActionListener(e -> display.setText(""));
                        break;

                    case "\u2190": // backspace
                        button.addActionListener(e -> {
                            String text = display.getText();
                            if (!text.isEmpty()) {
                                display.setText(text.substring(0, text.length() - 1));
                            }
                        });
                        break;

                    default:
                        button.addActionListener(e -> display.setText(display.getText() + label));
                        break;
                }

                panel.add(button);
            }

            return panel;
        }

        private void onEquals(ActionEvent e) {
            evaluateExpression();
        }

        private void evaluateExpression() {

            String expression = display.getText();

            if (expression == null || expression.trim().isEmpty()) {
                return;
            }

            try {

                ArrayList<Token> tokens = tokenize(expression);

                StringBuilder errorMsg = new StringBuilder();

                if (!validateTokens(tokens, errorMsg)) {
                    historyArea.append(expression + "  =>  " + errorMsg + "\n");
                    return;
                }

                double result = evaluate(tokens);

                String resultText = formatResult(result);

                historyArea.append(expression + "  =  " + resultText + "\n");

                display.setText(resultText);

            } catch (Exception ex) {
                historyArea.append(expression + "  =>  Error : " + ex.getMessage() + "\n");
            }
        }

        private String formatResult(double result) {
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        }
    }

    // Main

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            CalculatorGUI gui = new CalculatorGUI();
            gui.setVisible(true);
        });
    }
}