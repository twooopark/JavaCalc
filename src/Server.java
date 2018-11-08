
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(StaticVal.PORT);
        System.out.println("[server] listening...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("[server] connected from " + socket.getRemoteSocketAddress());
            try {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                /**
                 1. 목표 : 매우 긴 연산을 처리하는 것
                 2. 문자로 된 '수백만개의 연산' 연산자 우선순위에 따라 처리가 과연 가능할까?
                 3. ex) "10+10-20+10+10-20 ... 10+10-20*7" , 길이 10mb
                 과연, 결과는 0이 나올까? -120이 나올까?
                 4.
                 */
                /**************************** Header recv [ByteBuffer Array, Channel] *******************************/
                //3. [서버] 헤더 수신
                byte[] headBuf = new byte[StaticVal.HEAD_SIZE];

                if (is.read(headBuf) != -1) {
                    StringBuffer headSB = new StringBuffer(new String(headBuf));
                    String type = headSB.substring(0, 2);
                    if (type.equals(StaticVal.REQ)) {
                        String prec = headSB.substring(2, 4);
                        int precInt = Integer.parseInt(prec);
                        String leng = headSB.substring(4, 14);
                        int bodyLengInt = Integer.parseInt(leng);
                        System.out.println("[server] recv head: " + headSB.toString());

                        /**************************** body recv [byte[], StringBuffer, Socket] *******************************/

                        //4. [서버] 바디 수신
                        byte[] bodyBuf;
                        if (bodyLengInt > 4096)
                            bodyLengInt = StaticVal.BUFFERSIZE;
                        bodyBuf = new byte[bodyLengInt];

                        int readByteCount;

                        StringBuffer bodyData = new StringBuffer();
                        while (0 < (readByteCount = is.read(bodyBuf))) {
                            String inStr = new String(bodyBuf, 0, readByteCount);
                            String val = "";
                            for (int i = 0; i < bodyLengInt; i++) {
                                char ch = inStr.charAt(i);
                                System.out.print(ch);
                                if (!StaticVal.op.contains(ch)) {
                                    val += ch;
                                } else if (StaticVal.op.contains(ch)) {
                                    if (!val.isEmpty())
                                        val += " ";
                                    bodyData.append(val + ch + " ");
                                    val = "";
                                }
                            }
                            bodyData.append(val);
                        }
                        System.out.println("[server] recv body: " + bodyData.toString());

                        String stackExpressionStr[] = divideExpression(bodyData.toString());
//                            for (String e : stackExpressionStr) System.out.print(e + " ");

                        String res = String.format(StaticVal.PREC_STR_Format[precInt], calculate(stackExpressionStr));
                        System.out.println("[server] calc result: " + res);

                        /**************************** response [ByteBuffer, Channel] *******************************/
                        //response Header
                        headSB.delete(0, 14);

                        headSB.append(StaticVal.RES);
                        headSB.append(StaticVal.PRECS[2]);

                        leng = String.valueOf(res.length());
                        if (leng.length() < 10) {
                            int zeroPaddingSize = StaticVal.LENG_SIZE - leng.length();
                            while ((zeroPaddingSize--) > 0) headSB.append("0");
                        }
                        headSB.append(leng);

//                            System.out.println(headSB.toString()+res);
                        os.write((headSB.toString() + res).getBytes());
                        os.flush();
                    }
                }

            } catch (SocketException e) {
                System.out.println("[server] sudden closed by client");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null && socket.isClosed() == true) {
                    //socket.close();
                    System.out.println("[server] disconnection by client");
                }
            }
        }
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
//                System.out.print("\n" + num2 + exp + num1);
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

