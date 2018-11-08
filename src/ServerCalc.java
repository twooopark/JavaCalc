
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

public class ServerCalc {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(StaticVal.PORT));
        System.out.println("[server] listening...");

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            System.out.println("[server] connected from " + socketChannel.getRemoteAddress());
            try {

                    /**
                     1. 목표 : 매우 긴 연산을 처리하는 것
                     2. 문자로 된 '수백만개의 연산' 연산자 우선순위에 따라 처리가 과연 가능할까?
                     3. ex) "10+10-20+10+10-20 ... 10+10-20*7" , 길이 10mb
                            과연, 결과는 0이 나올까? -120이 나올까?
                     4.

                     * /
                    /**************************** Header recv [ByteBuffer Array, Channel] *******************************/
                    //3. [서버] 헤더 수신
                    ByteBuffer typeBuf = ByteBuffer.allocate(StaticVal.TYPE_SIZE);
                    ByteBuffer precisionBuf = ByteBuffer.allocate(StaticVal.PREC_SIZE);
                    ByteBuffer lengthBuf = ByteBuffer.allocate(StaticVal.LENG_SIZE);
                    ByteBuffer[] headerBuf = {typeBuf, precisionBuf, lengthBuf};

                while (true) {
                    if (socketChannel.read(headerBuf) > 0) {
                        //pointer 0으로 변경하여, 처음부터 읽을 수 있도록 함.
                        for (ByteBuffer buf : headerBuf) buf.flip();
                        //type
                        if (StandardCharsets.UTF_8.decode(typeBuf).toString().equals(StaticVal.REQ)) {
                            String sPrecision = StandardCharsets.UTF_8.decode(precisionBuf).toString();
                            String sLength = StandardCharsets.UTF_8.decode(lengthBuf).toString();
                            Long bodyLength = Long.parseLong(sLength);
                            int iBodyLength = Integer.parseInt(sLength);


                            /**************************** Body recv [ByteBuffer, Channel] *******************************/
                            //4. [서버] 바디 수신
                            StringBuffer bodyData = new StringBuffer();
                            ByteBuffer dataBuf = ByteBuffer.allocate(StaticVal.BUFFERSIZE);

                            if (StaticVal.BUFFERSIZE > bodyLength) {
                                dataBuf = ByteBuffer.allocate(iBodyLength);
                            } else {
                                long quotient = bodyLength / StaticVal.BUFFERSIZE;
                                long remainder = bodyLength % StaticVal.BUFFERSIZE;

                                for (int i = 0; i < quotient; i++) {
                                    socketChannel.read(dataBuf);
                                    dataBuf.flip();
                                    bodyData.append(StandardCharsets.UTF_8.decode(dataBuf).toString());
                                    dataBuf.clear();
                                }
                                dataBuf = ByteBuffer.allocate((int)remainder);
                            }
                            socketChannel.read(dataBuf);
                            dataBuf.flip();
                            bodyData.append(StandardCharsets.UTF_8.decode(dataBuf).toString());

                            if(bodyData.equals("")){
                                System.out.println("[ERROR] bodyData is null");
                                break;
                            }

                            /**************************** Calc [Stack<String>] *******************************/
                            String result = "123";

//                            //후위표기 연산(값 저장)
//                            Stack<String> valSt = new Stack();
//
//                            //중위표기 -> 후위표기 변환(연산자 저장)
//                            Stack<String> opSt = new Stack();
//                            String valTemp = "";
//
//                            //연산
//                            for (int i = 0; i < bodyLength; i++) {
//                                char token = bodyData.charAt(i);
//                                //피연산자
//                                if (!StaticVal.op.contains(token)) { //if(token < 48 || 57 < token)
//                                    valTemp += token;
//                                }
//                                //연산자
//                                else if (StaticVal.op.contains(token)) {
//                                    //피연산자 푸시, valTemp 초기화
//                                    valSt.push(valTemp);
//                                    valTemp = "";
//
//                                    //앞에 연산자가 있는 경우
//                                    if (!opSt.isEmpty()) {
//                                        //calculator : valSt에서 pop 한 값 2개, opSt로 연산
//                                        valSt.push(String.valueOf(calculator(valSt, opSt)));
//                                        opSt.push(String.valueOf(token));
//                                    }
//                                    //첫 연산자는 바로 push
//                                    else {
//                                        opSt.push(String.valueOf(token));
//                                    }
//                                } else {
//                                    System.out.println("[server] ?: " + token);
//                                }
//                            }
//                            valSt.push(valTemp);
//                            String result = String.valueOf(calculator(valSt, opSt));
//                            System.out.println("[server] calc result: " + result);

                            /**************************** response [ByteBuffer, Channel] *******************************/
                            //response Header
                            for (ByteBuffer buf : headerBuf) buf.clear();

                            typeBuf.put(StaticVal.RES.getBytes());
                            precisionBuf.put(sPrecision.getBytes());

                            String resultLength = String.valueOf(result.length());
                            for (int i = 0; i < StaticVal.LENG_SIZE - resultLength.length(); i++)
                                lengthBuf.put((byte) 0x30);
                            for (int i = 0; i < resultLength.length(); i++)
                                lengthBuf.put((byte) (resultLength.charAt(i)));

                            //5.[서버] 응답 헤더 송신
                            for (ByteBuffer buf : headerBuf) buf.flip();
                            socketChannel.write(headerBuf);

                            //6.[서버] 응답 바디 송신
                            ByteBuffer bodyBuf = ByteBuffer.allocate(result.length());
                            bodyBuf.put(result.getBytes());
                            bodyBuf.flip();
                            socketChannel.write(bodyBuf);
                        }
                    } else {
//                        if(!socketChannel.isConnected()) break;
                        if(socketChannel.read(headerBuf) == 0) break;
                    }
                }
            } catch (SocketException e) {
                System.out.println("[server] sudden closed by client");
            } finally {
                if (socketChannel != null && socketChannel.isOpen() == true) {
                    //socketChannel.close();
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

