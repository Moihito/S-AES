import java.util.ArrayList;
import java.util.List;

public class Utils {
    // 解析输入文本为 16-bit blocks
    public static List<Integer> parseInputToBlocks(String text, String mode) throws Exception {
        List<Integer> blocks = new ArrayList<>();
        if (mode.startsWith("Hex")) {
            String in = text.replaceAll(",", " ").replaceAll("\\s+", " ").trim();
            String[] toks = in.split(" ");
            for (String t : toks) {
                if (t.trim().isEmpty()) continue;
                blocks.add(parseHex16(t));
            }
        } else {
            byte[] bytes = text.getBytes("UTF-8");
            for (int i=0;i<bytes.length;i+=2) {
                int high = (bytes[i] & 0xFF);
                int low  = ( (i+1<bytes.length) ? (bytes[i+1] & 0xFF) : 0 );
                int val = (high<<8) | low;
                blocks.add(val);
            }
        }
        return blocks;
    }

    // 解析单个 16-bit hex 表达式（支持 0x 前缀）
    public static int parseHex16(String s) throws Exception {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        // allow >4 hex (caller may split) - but we still parse last 4 hex if longer:
        if (s.length() > 4) {
            // 保守处理：取低 4 位 hex（保持与你原实现兼容）
            s = s.substring(s.length()-4);
        }
        int v = Integer.parseInt(s, 16) & 0xFFFF;
        return v;
    }
}
