import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SAESGui {
    private JFrame frame;
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextField keyField;
    private JTextField key2Field;
    private JTextField ivField;
    private JComboBox<String> inputModeCombo;
    private JComboBox<String> operationCombo;
    private JComboBox<String> modeCombo;
    private JComboBox<String> multiCombo;
    private JCheckBox showIntermediate;

    public void createAndShow() {
        frame = new JFrame("S-AES 教学实验 (Java Swing)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel left = buildLeftColumn();
        main.add(left, BorderLayout.WEST);

        main.add(buildOutputPanel(), BorderLayout.CENTER);

        frame.setContentPane(main);
        frame.setVisible(true);
    }

    // ---------- UI 构建 ----------

    private JPanel buildLeftColumn() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(createInputPanel());
        left.add(Box.createVerticalStrut(8));
        left.add(createControlPanel());
        left.add(Box.createVerticalStrut(8));
        left.add(createActionPanel());
        return left;
    }

    private JComponent buildOutputPanel() {
        outputArea = new JTextArea(30, 50);
        outputArea.setEditable(false);
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("输出 / 日志"));
        return outScroll;
    }

    private JPanel createInputPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("输入"));

        inputArea = new JTextArea(6, 30);
        JScrollPane inScroll = new JScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("明文 / 密文（Hex 或 ASCII）"));
        p.add(inScroll);

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputModeCombo = new JComboBox<>(new String[]{"Hex (每组 16-bit)", "ASCII (任意长度, 每 2 Bytes 一组)"});
        modeRow.add(new JLabel("输入模式:"));
        modeRow.add(inputModeCombo);
        p.add(modeRow);

        JPanel keyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        keyRow.add(new JLabel("密钥 K (16-bit hex 或 32-bit hex for K1+K2):"));
        keyField = new JTextField(12);
        keyRow.add(keyField);
        p.add(keyRow);

        JPanel key2Row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        key2Row.add(new JLabel("密钥 K2 (若双重/三重单独输入):"));
        key2Field = new JTextField(12);
        key2Row.add(key2Field);
        p.add(key2Row);

        JPanel ivRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ivRow.add(new JLabel("IV (CBC, 16-bit hex，可留空随机生成):"));
        ivField = new JTextField(8);
        ivRow.add(ivField);
        p.add(ivRow);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(4,1));
        p.setBorder(BorderFactory.createTitledBorder("控制"));

        JPanel opRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opRow.add(new JLabel("操作:"));
        operationCombo = new JComboBox<>(new String[]{"Encrypt", "Decrypt"});
        opRow.add(operationCombo);
        p.add(opRow);

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modeRow.add(new JLabel("块模式:"));
        modeCombo = new JComboBox<>(new String[]{"ECB", "CBC"});
        modeRow.add(modeCombo);
        p.add(modeRow);

        JPanel multiRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        multiRow.add(new JLabel("多重加密:"));
        multiCombo = new JComboBox<>(new String[]{"Single", "Double", "Triple"});
        multiRow.add(multiCombo);
        p.add(multiRow);

        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showIntermediate = new JCheckBox("显示中间块（便于调试）");
        optRow.add(showIntermediate);
        p.add(optRow);

        return p;
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(5, 1, 6, 6));
        p.setBorder(BorderFactory.createTitledBorder("操作"));

        JButton runButton = new JButton("执行 (根据上面设置)");
        runButton.addActionListener(e -> onRunClick());
        p.add(runButton);

        JButton mitmButton = new JButton("中间相遇攻击（双重）");
        mitmButton.addActionListener(e -> onMitmClick());
        p.add(mitmButton);

        JButton crossTestButton = new JButton("交叉测试（A->B）示例");
        crossTestButton.addActionListener(e -> onCrossTest());
        p.add(crossTestButton);

        JButton cbcTamperButton = new JButton("CBC 篡改示例（对比篡改前后解密）");
        cbcTamperButton.addActionListener(e -> onCbcTamper());
        p.add(cbcTamperButton);

        JButton clearButton = new JButton("清空输出");
        clearButton.addActionListener(e -> outputArea.setText(""));
        p.add(clearButton);

        return p;
    }

    // ---------- 事件处理（拆成小函数） ----------

    private void onRunClick() {
        try {
            String inputText = inputArea.getText().trim();
            if (inputText.isEmpty()) { showError("请输入明文或密文"); return; }

            String inputMode = (String) inputModeCombo.getSelectedItem();
            boolean isEncrypt = operationCombo.getSelectedItem().equals("Encrypt");
            String blockMode = (String) modeCombo.getSelectedItem();
            String multi = (String) multiCombo.getSelectedItem();
            boolean showMid = showIntermediate.isSelected();

            List<Integer> blocks = Utils.parseInputToBlocks(inputText, inputMode);
            if (blocks.isEmpty()) { showError("无法解析输入为 16-bit 分组，请检查输入和模式。"); return; }

            outputArea.append(String.format("处理 %d 个分组（%s 模式, %s, %s）\n",
                    blocks.size(), blockMode, multi, (isEncrypt?"Encrypt":"Decrypt")));

            // keys
            KeyBundle keys = parseKeysForMulti(multi);
            if (keys == null) return; // 错误已在 parseKeysForMulti 中提示

            // IV
            int iv = 0;
            if ("CBC".equals(blockMode)) {
                String ivText = ivField.getText().trim();
                if (ivText.isEmpty()) {
                    iv = (int) (Math.random() * 0x10000);
                    ivField.setText(String.format("0x%04X", iv));
                    outputArea.append("未指定 IV，随机生成 IV = " + ivField.getText() + "\n");
                } else {
                    try { iv = Utils.parseHex16(ivText); } catch (Exception ex) { showError("IV 解析失败: " + ex.getMessage()); return;}
                }
            }

            // process blocks
            List<Integer> outBlocks = processBlocks(blocks, blockMode, isEncrypt, multi, keys.k1, keys.k2, showMid, iv);

            // show hex output
            StringBuilder sbHex = new StringBuilder();
            for (int x : outBlocks) sbHex.append(String.format("0x%04X ", x));
            outputArea.append("输出（16-bit hex blocks）:\n" + sbHex.toString() + "\n");

            if (inputMode.startsWith("ASCII")) {
                StringBuilder ascii = new StringBuilder();
                for (int x : outBlocks) {
                    int high = (x >> 8) & 0xFF;
                    int low  = x & 0xFF;
                    ascii.append((char) high);
                    ascii.append((char) low);
                }
                outputArea.append("输出 ASCII（可能有不可打印字符）:\n" + ascii.toString() + "\n");
            }

        } catch (Exception ex) {
            showError("运行时异常: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private KeyBundle parseKeysForMulti(String multi) {
        try {
            String keyText = keyField.getText().trim();
            if (keyText.isEmpty()) { showError("请输入密钥 K（16-bit hex）, 或在双重/三重下输入 K1+K2（32-bit hex）"); return null; }
            String k1hex=null, k2hex=null;
            if ("Single".equals(multi)) {
                k1hex = keyText;
            } else {
                if (!key2Field.getText().trim().isEmpty()) {
                    k1hex = keyField.getText().trim();
                    k2hex = key2Field.getText().trim();
                } else {
                    String kt = keyText;
                    if (kt.startsWith("0x")||kt.startsWith("0X")) kt = kt.substring(2);
                    if (kt.length() == 8) {
                        k1hex = kt.substring(0,4);
                        k2hex = kt.substring(4,8);
                    } else {
                        showError(String.format("%s 加密请单独在 K/K2 中填入两个 16-bit hex，或在 K 中填写 32-bit hex（8 hex digits）", multi));
                        return null;
                    }
                }
            }
            int k1 = Utils.parseHex16(k1hex);
            int k2 = (k2hex==null) ? -1 : Utils.parseHex16(k2hex);
            return new KeyBundle(k1,k2);
        } catch (Exception ex) {
            showError("密钥解析失败: " + ex.getMessage());
            return null;
        }
    }

    private List<Integer> processBlocks(List<Integer> blocks, String blockMode, boolean isEncrypt,
                                        String multi, int k1, int k2, boolean showMid, int iv) throws Exception {
        List<Integer> outBlocks = new ArrayList<>();
        if ("ECB".equals(blockMode)) {
            for (int b : blocks) {
                int res = processSingleBlock(b, isEncrypt, multi, k1, k2, showMid);
                outBlocks.add(res);
            }
        } else { // CBC
            if (isEncrypt) {
                int previous = iv;
                for (int b : blocks) {
                    int xored = previous ^ b;
                    int cipher = processSingleBlock(xored, true, multi, k1, k2, showMid);
                    outBlocks.add(cipher);
                    previous = cipher;
                }
            } else {
                int previous = iv;
                for (int c : blocks) {
                    int decrypted = processSingleBlock(c, false, multi, k1, k2, showMid);
                    int plain = decrypted ^ previous;
                    outBlocks.add(plain);
                    previous = c;
                }
            }
        }
        return outBlocks;
    }

    private int processSingleBlock(int block, boolean encrypt, String multi, int k1, int k2, boolean showMid) throws Exception {
        if ("Single".equals(multi)) {
            return encrypt ? SAASTools.encryptBlock(block, k1) : SAASTools.decryptBlock(block, k1);
        } else if ("Double".equals(multi)) {
            if (encrypt) {
                return SAASTools.encryptBlock(SAASTools.encryptBlock(block, k1), k2);
            } else {
                return SAASTools.decryptBlock(SAASTools.decryptBlock(block, k2), k1);
            }
        } else { // Triple (E(K1, D(K2, E(K1,P))) 示例)
            if (encrypt) {
                int t1 = SAASTools.encryptBlock(block, k1);
                int t2 = SAASTools.decryptBlock(t1, k2);
                return SAASTools.encryptBlock(t2, k1);
            } else {
                int t1 = SAASTools.decryptBlock(block, k1);
                int t2 = SAASTools.encryptBlock(t1, k2);
                return SAASTools.decryptBlock(t2, k1);
            }
        }
    }

    private void onMitmClick() {
        try {
            String inputText = inputArea.getText().trim();
            if (inputText.isEmpty()) { showError("请在输入框中提供明文分组 (单个 16-bit)"); return; }
            List<Integer> blocks = Utils.parseInputToBlocks(inputText, (String) inputModeCombo.getSelectedItem());
            if (blocks.size() != 1) { showError("中间相遇攻击工具需要单个明文分组（16-bit）作为输入"); return; }
            int P = blocks.get(0);

            String cStr = JOptionPane.showInputDialog(frame, "请输入对应的密文分组 16-bit（hex）用于 MITM：", "0x0000");
            if (cStr==null) return;
            int C = Utils.parseHex16(cStr.trim());

            outputArea.append(String.format("开始中间相遇攻击：P=0x%04X, C=0x%04X\n", P, C));
            long start = System.currentTimeMillis();
            List<int[]> candidates = SAASTools.meetInTheMiddle(P, C);
            long elapsed = System.currentTimeMillis() - start;
            outputArea.append("共找到候选对: " + candidates.size() + " （耗时 " + elapsed + " ms）\n");
            for (int[] pair : candidates) {
                outputArea.append(String.format("K1=0x%04X, K2=0x%04X\n", pair[0], pair[1]));
            }
            if (candidates.isEmpty()) outputArea.append("未找到候选密钥对（可能密钥不在 16-bit 空间或不是两重加密产生）\n");
        } catch (Exception ex) { showError("MITM 异常: " + ex.getMessage()); ex.printStackTrace(); }
    }

    private void onCrossTest() {
        try {
            String inputText = inputArea.getText().trim();
            if (inputText.isEmpty()) { showError("请在输入框中提供明文"); return; }
            List<Integer> blocks = Utils.parseInputToBlocks(inputText, (String) inputModeCombo.getSelectedItem());
            if (blocks.isEmpty()) { showError("无法解析明文"); return; }

            String keyText = keyField.getText().trim();
            if (keyText.isEmpty()) { showError("请提供密钥 K"); return; }
            int k = Utils.parseHex16(keyText);

            List<Integer> cipherBlocks = new ArrayList<>();
            for (int b: blocks) cipherBlocks.add(SAASTools.encryptBlock(b, k));
            List<Integer> plainBlocks = new ArrayList<>();
            for (int c: cipherBlocks) plainBlocks.add(SAASTools.decryptBlock(c, k));

            outputArea.append("交叉测试：对比 A->B 的加解密结果\n");
            outputArea.append("原文块: " + blocks.toString() + "\n");
            outputArea.append("加密后: " + cipherBlocks.toString() + "\n");
            outputArea.append("解密后: " + plainBlocks.toString() + "\n");
            if (blocks.equals(plainBlocks)) outputArea.append("交叉测试通过：解密恢复原文\n");
            else outputArea.append("交叉测试失败：解密未恢复原文\n");

        } catch (Exception ex) { showError("交叉测试异常: " + ex.getMessage()); ex.printStackTrace(); }
    }

    private void onCbcTamper() {
        try {
            String inputText = inputArea.getText().trim();
            if (inputText.isEmpty()) { showError("请在输入框中提供明文（ASCII 或 Hex）"); return; }
            String inputMode = (String) inputModeCombo.getSelectedItem();
            List<Integer> blocks = Utils.parseInputToBlocks(inputText, inputMode);
            if (blocks.isEmpty()) { showError("无法解析输入为分组"); return; }

            String keyText = keyField.getText().trim();
            if (keyText.isEmpty()) { showError("请输入 16-bit key"); return; }
            int k = Utils.parseHex16(keyText);

            int iv = (int)(Math.random()*0x10000);
            ivField.setText(String.format("0x%04X", iv));
            outputArea.append(String.format("CBC 加密 (IV=0x%04X)\n", iv));
            List<Integer> cipher = new ArrayList<>();
            int prev = iv;
            for (int b: blocks) {
                int x = prev ^ b;
                int c = SAASTools.encryptBlock(x, k);
                cipher.add(c);
                prev = c;
            }
            outputArea.append("密文: " + cipher.toString() + "\n");

            List<Integer> tampered = new ArrayList<>(cipher);
            if (tampered.size()>=2) {
                tampered.set(1, tampered.get(1) ^ 0x0001);
                outputArea.append("对密文第二分组进行了最低位翻转（模拟篡改）\n");
            } else {
                tampered.set(0, tampered.get(0) ^ 0x0001);
                outputArea.append("对密文第一分组进行了最低位翻转（模拟篡改）\n");
            }
            outputArea.append("篡改后密文: " + tampered.toString() + "\n");

            outputArea.append("解密 — 原始密文：\n");
            List<Integer> plainOrig = new ArrayList<>();
            prev = iv;
            for (int c: cipher) {
                int d = SAASTools.decryptBlock(c, k);
                plainOrig.add(d ^ prev);
                prev = c;
            }
            outputArea.append(plainOrig.toString() + "\n");

            outputArea.append("解密 — 篡改后密文：\n");
            List<Integer> plainTam = new ArrayList<>();
            prev = iv;
            for (int c: tampered) {
                int d = SAASTools.decryptBlock(c, k);
                plainTam.add(d ^ prev);
                prev = c;
            }
            outputArea.append(plainTam.toString() + "\n");

            outputArea.append("对比：可见 CBC 下篡改对当前块和下一块会产生影响（具体依赖位置）。\n");

        } catch (Exception ex) { showError("CBC 篡改示例异常: " + ex.getMessage()); ex.printStackTrace(); }
    }

    // ---------- 小工具和数据类 ----------

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "错误", JOptionPane.ERROR_MESSAGE);
        if (outputArea != null) outputArea.append("错误: " + msg + "\n");
    }

    private static class KeyBundle {
        int k1, k2;
        KeyBundle(int k1, int k2) { this.k1 = k1; this.k2 = k2; }
    }
}
