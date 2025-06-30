package org.jpos.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
    public static String decrypt(String encryptedData, String sk)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] secretKey = Base64.getDecoder().decode(sk);
        byte[] encryptedDataByte = Base64.getDecoder().decode(encryptedData);
        byte[] iv = new byte[16];
        byte[] encryptedBytes = new byte[encryptedDataByte.length - 16];

        System.arraycopy(encryptedDataByte, 0, iv, 0, 16);
        System.arraycopy(encryptedDataByte, 16, encryptedBytes, 0, encryptedDataByte.length - 16);

        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }
}
