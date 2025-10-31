import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SAASTools {
    private static final int[] SBOX = {
            0x9, 0x4, 0xA, 0xB,
            0xD, 0x1, 0x8, 0x5,
            0x6, 0x2, 0x0, 0x3,
            0xC, 0xE, 0xF, 0x7
    };
    private static final int[] SBOX_INV = invertSbox(SBOX);

    private static int[] invertSbox(int[] s) {
        int[] inv = new int[16];
        for (int i=0;i<16;i++) inv[s[i]] = i;
        return inv;
    }

    private static int gfMul(int a, int b) {
        int res = 0;
        for (int i=0;i<4;i++) {
            if (((b>>i)&1) != 0) res ^= (a<<i);
        }
        int mod = 0x13;
        for (int shift=7; shift>=4; shift--) {
            if (((res>>shift)&1) != 0) {
                res ^= (mod << (shift-4));
            }
        }
        return res & 0xF;
    }

    private static int[] mixColumns(int[] state) {
        int a = state[0], b = state[1], c = state[2], d = state[3];
        int r0 = (gfMul(1,a) ^ gfMul(4,b)) & 0xF;
        int r1 = (gfMul(4,a) ^ gfMul(1,b)) & 0xF;
        int r2 = (gfMul(1,c) ^ gfMul(4,d)) & 0xF;
        int r3 = (gfMul(4,c) ^ gfMul(1,d)) & 0xF;
        return new int[]{r0,r1,r2,r3};
    }

    private static int[] invMixColumns(int[] state) {
        int a = state[0], b = state[1], c = state[2], d = state[3];
        int r0 = (gfMul(9,a) ^ gfMul(2,b)) & 0xF;
        int r1 = (gfMul(2,a) ^ gfMul(9,b)) & 0xF;
        int r2 = (gfMul(9,c) ^ gfMul(2,d)) & 0xF;
        int r3 = (gfMul(2,c) ^ gfMul(9,d)) & 0xF;
        return new int[]{r0,r1,r2,r3};
    }

    private static int[] expandKeyBytes(int key16) {
        int w0 = (key16 >> 8) & 0xFF;
        int w1 = key16 & 0xFF;
        int[] w = new int[6];
        w[0] = w0; w[1] = w1;
        int[] rcons = new int[]{0x80, 0x30};
        for (int i=0;i<2;i++) {
            int t = w[2*i+1];
            t = subWord(rotNibble(t));
            t ^= rcons[i];
            w[2*i+2] = w[2*i] ^ t;
            w[2*i+3] = w[2*i+2] ^ w[2*i+1];
        }
        return w;
    }

    private static int[] expandKey(int key16) {
        int[] w = expandKeyBytes(key16);
        int k0 = ((w[0]&0xFF)<<8) | (w[1]&0xFF);
        int k1 = ((w[2]&0xFF)<<8) | (w[3]&0xFF);
        int k2 = ((w[4]&0xFF)<<8) | (w[5]&0xFF);
        return new int[]{k0,k1,k2};
    }

    private static int rotNibble(int t) {
        int high = (t >> 4) & 0xF;
        int low  = t & 0xF;
        return ((low<<4) | high) & 0xFF;
    }

    private static int subWord(int t) {
        int high = (t >> 4) & 0xF;
        int low  = t & 0xF;
        int rhigh = SBOX[high];
        int rlow  = SBOX[low];
        return ((rhigh<<4) | rlow) & 0xFF;
    }

    private static int nibbleSub(int state16) {
        int out = 0;
        for (int i=0;i<4;i++) {
            int nib = (state16 >> (12 - 4*i)) & 0xF;
            int mapped = SBOX[nib] & 0xF;
            out |= (mapped << (12 - 4*i));
        }
        return out & 0xFFFF;
    }
    private static int nibbleSubInv(int state16) {
        int out = 0;
        for (int i=0;i<4;i++) {
            int nib = (state16 >> (12 - 4*i)) & 0xF;
            int mapped = SBOX_INV[nib] & 0xF;
            out |= (mapped << (12 - 4*i));
        }
        return out & 0xFFFF;
    }

    private static int shiftRows(int state16) {
        int s0 = (state16 >> 12) & 0xF;
        int s1 = (state16 >> 8) & 0xF;
        int s2 = (state16 >> 4) & 0xF;
        int s3 = (state16) & 0xF;
        return (s0<<12) | (s1<<8) | (s3<<4) | s2;
    }
    private static int shiftRowsInv(int state16) {
        // 对于 2x2 的换位，此处与 shiftRows 本身互为逆（交换下行两个元素）
        int s0 = (state16 >> 12) & 0xF;
        int s1 = (state16 >> 8) & 0xF;
        int s2 = (state16 >> 4) & 0xF;
        int s3 = (state16) & 0xF;
        return (s0<<12) | (s1<<8) | (s3<<4) | s2;
    }

    private static int addRoundKey(int state16, int roundKey) {
        return state16 ^ (roundKey & 0xFFFF);
    }

    public static int encryptBlock(int plain16, int key16) {
        int[] roundKeys = expandKey(key16);
        int state = addRoundKey(plain16, roundKeys[0]);
        state = nibbleSub(state);
        state = shiftRows(state);
        state = packNibbles(mixColumns(unpackNibbles(state)));
        state = addRoundKey(state, roundKeys[1]);
        state = nibbleSub(state);
        state = shiftRows(state);
        state = addRoundKey(state, roundKeys[2]);
        return state & 0xFFFF;
    }

    public static int decryptBlock(int cipher16, int key16) {
        int[] roundKeys = expandKey(key16);
        int state = addRoundKey(cipher16, roundKeys[2]);
        state = shiftRowsInv(state);
        state = nibbleSubInv(state);
        state = addRoundKey(state, roundKeys[1]);
        int[] nibbles = unpackNibbles(state);
        nibbles = invMixColumns(nibbles);
        state = packNibbles(nibbles);
        state = shiftRowsInv(state);
        state = nibbleSubInv(state);
        state = addRoundKey(state, roundKeys[0]);
        return state & 0xFFFF;
    }

    private static int[] unpackNibbles(int state16) {
        return new int[]{
                (state16 >> 12) & 0xF,
                (state16 >> 8) & 0xF,
                (state16 >> 4) & 0xF,
                (state16) & 0xF
        };
    }
    private static int packNibbles(int[] n) {
        return ((n[0]&0xF)<<12) | ((n[1]&0xF)<<8) | ((n[2]&0xF)<<4) | (n[3]&0xF);
    }

    public static List<int[]> meetInTheMiddle(int P, int C) {
        HashMap<Integer, ArrayList<Integer>> midMap = new HashMap<>();
        for (int k1=0;k1<0x10000;k1++) {
            int mid = encryptBlock(P, k1);
            midMap.computeIfAbsent(mid, kk -> new ArrayList<>()).add(k1);
        }
        List<int[]> result = new ArrayList<>();
        for (int k2=0;k2<0x10000;k2++) {
            int midDec = decryptBlock(C, k2);
            ArrayList<Integer> possibleK1 = midMap.get(midDec);
            if (possibleK1 != null) {
                for (int k1 : possibleK1) {
                    result.add(new int[]{k1,k2});
                }
            }
        }
        return result;
    }
}
