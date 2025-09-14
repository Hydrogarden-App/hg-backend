package com.hydrogarden.business.device.core;

public class BinaryUtils {
    public static byte[] packBooleans(boolean[] states) {
        int len = (states.length + 7) / 8;
        byte[] result = new byte[len];
        for (int i = 0; i < states.length; i++) {
            if (states[i]) {
                result[i / 8] |= 1 << (7 - (i % 8)); // MSB first
            }
        }
        return result;
    }

    public static boolean[] unpackBooleans(byte[] data, int numBits) {
        boolean[] result = new boolean[numBits];
        for (int i = 0; i < numBits; i++) {
            result[i] = (data[i / 8] & (1 << (7 - (i % 8)))) != 0;
        }
        return result;
    }
}
