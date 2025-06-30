package org.jpos.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jpos.iso.ISOUtil;

public class TripleDES {
    public static String encrypt(String plaintext, String key) {
        try {
            byte[] keyBytes = ISOUtil.hex2byte(key);
            byte[] plaintextBytes = ISOUtil.hex2byte(plaintext);
            byte[] iv = new byte[8];

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encryptedBytes = cipher.doFinal(plaintextBytes);

            return ISOUtil.byte2hex(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String decrypt(String ciphertext, String key) {
        try {
            byte[] keyBytes = ISOUtil.hex2byte(key);
            byte[] ciphertextBytes = ISOUtil.hex2byte(ciphertext);
            byte[] iv = new byte[8];

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
            return ISOUtil.byte2hex(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
