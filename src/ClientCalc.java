
import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientCalc {

    public static void main(String[] args) throws IOException {
        Socket socket = SocketChannel.open().socket();
        try {
            socket.connect(new InetSocketAddress(StaticVal.IP, StaticVal.PORT));
            System.out.println("[client] connected");

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            SocketChannel socketChannel = socket.getChannel();

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter a file name. ex) test.txt \n >> ");
            String inputFileName = br.readLine();

            String filePath = StaticVal.filePath;
            File file = new File(filePath, inputFileName);

            /*************************** 클라이언트 - 요청 송신 ***********************

             1. 연산할 데이터(바디)의 양이 10mb를 넘는 경우를 테스트 하고자 File 입력을 받는다.
             2. 헤더를 보내고(writeAndflush), 바디를 보낸다.
             3. //바디 사이즈가 일정 크기보다 크다면, ByteBuffer를 사용한다.
             4. ByteBuffer : 빠른 I/O와 대용량 처리에 유리하다.
             5. JVM의 HeapBuffer 대신, OS 커널 메모리 버퍼 사용 가능(Direct Memory Access)

             */

            if (file.exists()) {
                /**************************** header [ByteBuffer, Socket] *******************************/
                StringBuffer headSB = new StringBuffer(StaticVal.HEAD_SIZE);
                headSB.append(StaticVal.REQ);
                headSB.append(StaticVal.PRECS[2]);

                String fLength = String.valueOf(file.length());
                if (fLength.length() < 10) {
                    int zeroPaddingSize = StaticVal.LENG_SIZE - fLength.length();
                    while ((zeroPaddingSize--) > 0) headSB.append("0");
                }
                headSB.append(fLength);

                os.write(headSB.toString().getBytes());
                os.flush();

                /**************************** body [ByteBuffer, FileChannel, SocketChannel] *******************************/


                Path path = Paths.get(filePath, inputFileName);
                FileChannel fic = FileChannel.open(path);

                //Direct Memory Access
                fic.transferTo(0, fic.size(), socketChannel);
/*
                if(file.length() > StaticVal.FILE_SIZE_1MB) {// 1mb : 임시
                    ByteBuffer bodyByteBuf = ByteBuffer.allocateDirect(StaticVal.BUFFERSIZE);

                    int readByteCount = -1;
                    while (0 < (readByteCount = fic.read(bodyByteBuf))) {
                        System.out.println("Number of bytes read: "+ readByteCount);
                        bodyByteBuf.flip();
                        while (bodyByteBuf.hasRemaining()){
                            socketChannel.write(bodyByteBuf);
                        }
                        bodyByteBuf.clear();
                    }
                }
                else
                    fic.transferTo(0, fic.size(), socketChannel);
*/

                fic.close();
                socketChannel.shutdownOutput();
            } else
                System.out.println("file not exist!");


            /*************************** 클라이언트 - 결과 수신 ***********************

             1. 문자열로 변환된 결과 값(long)의 길이가 20자리를 넘을 수 없다. (long의 범위 (64bit) : -2^63 ~ 2^63 -1)
             2. 그러므로, 기존 문자열 프로토콜과 유사하게, 소켓I/O와 스트링 연산을 이용해 간단히 처리

             */

            /**************************** Header recv [byte[], StringBuffer, Socket] *******************************/
             byte[] headBuf = new byte[StaticVal.HEAD_SIZE];

            if (is.read(headBuf) != -1) {
                StringBuffer sb = new StringBuffer(new String(headBuf));
                String type = sb.substring(0, 2);
                if (type.equals(StaticVal.RES)) {
                    String prec = sb.substring(2, 4);
                    String leng = sb.substring(4, 14);
                    int bodyLengInt = Integer.parseInt(leng);

                    /**************************** body recv [byte[], StringBuffer, Socket] *******************************/
                    byte[] bodyBuf = new byte[bodyLengInt];

                    StringBuffer data = new StringBuffer();
                    if (is.read(bodyBuf) != -1) data.append(new String(bodyBuf));
                    System.out.println("[client] recv result: " + data.toString());
                }
            } else
                System.out.println("end of stream!");

        } catch (ConnectException e) {
            System.out.println("[client] not connect");
        } catch (FileNotFoundException e) {
            System.out.println("[client] file not found");
        } finally {
            if (socket != null && socket.isClosed() == false) {
                socket.close();
                System.out.println("[client] disconnection");
            }
        }
    }
}
