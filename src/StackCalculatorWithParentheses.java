import java.util.Scanner;
import java.util.Stack;

public class StackCalculatorWithParentheses {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("수식을 입력하세요.");
        String expression = sc.nextLine();

        String stackExpressionStr[] = divideExpression(expression);

        for (String e : stackExpressionStr) {
            System.out.print(e + " ");
        }

        System.out.println("\n" + calculate(stackExpressionStr));
    }


    private static String[] divideExpression(String expression) {
        String[] expressionArr = expression.split(" ");
        String expressionStr = "";
        Stack<String> operatorStack = new Stack<String>();

        for (String exp : expressionArr) {
            try {
                double number = Double.parseDouble(exp);
                expressionStr += number + " ";
            } catch (NumberFormatException e) { // 연산자 차례
                if (exp.equals("(")) operatorStack.push("(");
                else if (exp.equals(")")) {
                    while (!operatorStack.peek().equals("(")) expressionStr += operatorStack.pop() + " ";
                    operatorStack.pop(); // "(" 삭제
                } else {
                    OperatorPriorityWithParentheses priority = OperatorPriorityWithParentheses.findPriority(exp);
                    while (!operatorStack.isEmpty()) {
                        String expInStack = operatorStack.peek();
                        if (priority.getPriority() <= OperatorPriorityWithParentheses.findPriority(expInStack).getPriority())
                            expressionStr += operatorStack.pop() + " ";
                        else break;
                    }
                    operatorStack.push(exp);
                }

            }
        }

        while (!operatorStack.isEmpty()) expressionStr += operatorStack.pop() + " ";

        return expressionStr.trim().split(" ");
    }

    private static double calculate(String[] stackExpressionStr) {
        Stack<Double> numberStack = new Stack<Double>();
        for (String exp : stackExpressionStr) {
            try {
                double number = Double.parseDouble(exp);
                numberStack.push(number);
            } catch (NumberFormatException e) {
                double num1 = numberStack.pop();
                double num2 = numberStack.pop();
                System.out.print("\n" + num2 + exp + num1);
                switch (exp) {
                    case "+":
                        numberStack.push(num2 + num1);
                        break;
                    case "-":
                        numberStack.push(num2 - num1);
                        break;
                    case "*":
                        numberStack.push(num2 * num1);
                        break;
                    case "/":
                        numberStack.push(num2 / num1);
                        break;
                }
            }
        }

        return numberStack.pop();
    }
}