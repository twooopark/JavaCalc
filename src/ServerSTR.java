
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

public class ServerSTR {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(StaticVal.PORT);
        System.out.println("[server] listening...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("[server] connected from " + socket.getRemoteSocketAddress());
            try {
                InputStream is = socket.getInputStream();
//                OutputStream os = socket.getOutputStream();
                    /**************************** receive *******************************/
                    /**************************** Header recv [ByteBuffer Array, Channel] *******************************/
                    //3. [서버] 헤더 수신
                    byte[] headBuf = new byte[StaticVal.HEAD_SIZE];

                    if( is.read(headBuf) != -1) {
                        StringBuffer sb = new StringBuffer(new String(headBuf));
                        String type = sb.substring(0, 2);
                        if (type.equals(StaticVal.REQ)) {
                            String prec = sb.substring(2, 4);
                            String leng = sb.substring(4, 14);
                            int bodyLengInt = Integer.parseInt(leng);

                            /**************************** body recv [byte[], StringBuffer, Socket] *******************************/

                            //4. [서버] 바디 수신
                            byte[] bodyBuf;
                            if (bodyLengInt > 4096)
                                bodyLengInt = StaticVal.BUFFERSIZE;
                            bodyBuf = new byte[bodyLengInt];

                            int readByteCount;
                            //BUFFERSIZE 보다 내용이 긴 경우, 계속해서 읽는다.
                            while (0 < (readByteCount = is.read(bodyBuf))) {
                                String inStr = new String(bodyBuf, 0, readByteCount);
                                System.out.print(inStr);
                            }
                            System.out.print(leng);
//                            StringBuffer bodyData = new StringBuffer();
//                            while (0 < (readByteCount = is.read(bodyBuf))) {
//
//                                String inStr = new String(bodyBuf, 0, readByteCount);
//                                System.out.print(inStr);
//
////                                bodyData.append(new String(bodyBuf));
//                                //if( (bodyLengInt -= readByteCount) == 0 ) break;
//                            }
//
//                            System.out.println("[client] recv data: " + bodyData.toString());
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

    private static int calculator(Stack<String> valSt, Stack<String> opSt) {
        int t1 = Integer.parseInt(valSt.pop());
        int t2 = Integer.parseInt(valSt.pop());
        int temp = 0;
        switch (opSt.pop().charAt(0)) {
            case '+':
                temp = t2 + t1;
                break;
            case '-':
                temp = t2 - t1;
                break;
            case '*':
                temp = t2 * t1;
                break;
            case '/':
                if (t1 == 0) {
                    System.out.print("Cannot divide by'0'");
                    break;
                }
                temp = t2 / t1;
                break;
            default:
                break;
        }
        return temp;
    }
}

