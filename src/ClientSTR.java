
import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class ClientSTR {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(StaticVal.IP, StaticVal.PORT));
            System.out.println("[client] connected");

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            String fLength = null, inStr = null, outStr = null;

            /**************************** request *******************************/
            //파일을 통해, 연산할 내용을 문자열로 입력받는다.
            String filePath = StaticVal.filePath;
            File file = new File(filePath, "test.txt");

            if (file.exists()) {

                /**************************** header [ByteBuffer, Socket] *******************************/
                //Header, ByteBuffer
                ByteBuffer headByteBuf = ByteBuffer.allocate(StaticVal.HEAD_SIZE);

                //Java -> C : le
                //headByteBuf.order(ByteOrder.LITTLE_ENDIAN);

                //type : 2byte
                headByteBuf.put(StaticVal.REQUEST);
                headByteBuf.put(StaticVal.PREC[0]);

                //데이터 길이, 문자열
                //Long -> String -> char -> byte, ex) 12L -> "12" -> '1' '2' -> 0x31 0x32
                fLength = String.valueOf(file.length());
                for (int i = 0; i < StaticVal.LENG_SIZE - fLength.length(); i++) {
                    headByteBuf.put((byte) 0x30);
                }
//                headByteBuf.put(new byte[StaticVal.LENGTH_MAX_SIZE - fLength.length()]); //padding

                //2byte char -> 1byte char
                for (int i = 0; i < fLength.length(); i++)
                    headByteBuf.put((byte) (fLength.charAt(i)));//-48));

                //1. [클라이언트] 헤더 송신
                headByteBuf.flip();
                os.write(headByteBuf.array());
                os.flush();

                /**************************** body [ByteBuffer, Socket] *******************************/
                //buffer, file read > byte write
                ByteBuffer bodyByteBuf = ByteBuffer.allocate(StaticVal.BUFFERSIZE);
                FileInputStream fis = new FileInputStream(file);

                //2. [클라이언트] 바디 송신
                int readByteCount = -1;
                while (0 < (readByteCount = fis.read(bodyByteBuf.array()))) {
                    bodyByteBuf.flip();
                    os.write(bodyByteBuf.array());
                }

                /**************************** receive *******************************/
                /**************************** Header recv [byte[], StringBuffer, Socket] *******************************/
                //3. [클라이언트] 헤더 수신
                byte[] headBuf = new byte[StaticVal.HEAD_SIZE];

                readByteCount = is.read(headBuf);
                StringBuffer sb = new StringBuffer(new String(headBuf));

                String type = sb.substring(0, 2);
                if (type.equals(StaticVal.RES)) {
                    String prec = sb.substring(2, 4);
                    String leng = sb.substring(4, 14);

                    Long bodyLength = Long.parseLong(leng);

                    /**************************** body recv [byte[], StringBuffer, Socket] *******************************/
                    byte[] bodyBuf = new byte[StaticVal.BUFFERSIZE];
                    StringBuffer data = new StringBuffer();

                    /*
                    만일, bodyLength가 1000만 byte, 즉 10메가 라면?
                    buffer를 10메가 만큼 할당 할 것인가?
                    나눠서 받아야 하지 않을까?
                    
                    while ((readByteCount = is.read(bodyBuf)) > 0)
                        data.append(new String(bodyBuf,0,readByteCount));
                    */

                    if (StaticVal.BUFFERSIZE > bodyLength) {
                        int bodyLengInt = Integer.parseInt(leng);
                        bodyBuf = new byte[bodyLengInt];
                        is.read(bodyBuf);
                        data.append(new String(bodyBuf));
                    }
                    else {
                        while ((readByteCount = is.read(bodyBuf)) > 0) {
                            data.append(new String(bodyBuf,0,readByteCount));
                        }
                    }
                    System.out.println("[client] recv result: "+ data.toString());
                }
            }
        } catch (ConnectException e) {
            System.out.println("[client] not connect");
        } catch (SocketTimeoutException e) {
            System.out.println("[client] read timeout");
        } catch (FileNotFoundException e) {
            System.out.println("[client] file not found");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && socket.isClosed() == false) {
                socket.close();
                System.out.println("[client] disconnection");
            }

        }
    }
}
