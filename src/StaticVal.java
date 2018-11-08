import java.util.Arrays;
import java.util.List;

public class StaticVal {
    static final String REQ = "01";
    static final String RES = "02";

    //정밀도 0~6
    static final String[] PRECS = {"00", "01", "02", "03", "04", "05", "06"};
    static final String[] PREC_STR_Format = {"%.0f", "%.1f", "%.2f", "%.3f", "%.4f", "%.5f", "%.6f"};

    static final String filePath = System.getProperty("user.dir") + "\\src";
    static final String IP = "172.21.25.143";//"123.2.134.41";//"172.21.24.182";//
    static final int PORT = 11001;//35100;//

    static final int BUFFERSIZE = 1024;
    static final int FILE_SIZE_1MB = 1000000;

    static final int TYPE_SIZE = 2;
    static final int PREC_SIZE = 2;
    static final int LENG_SIZE = 10;

    static final int HEAD_SIZE = TYPE_SIZE+PREC_SIZE+LENG_SIZE;

    static final List<Character> op = Arrays.asList('*', '/', '+', '-', '(', ')');//42, 47, 43, 45, 40, 41); // *, /, +, -, (, )
}