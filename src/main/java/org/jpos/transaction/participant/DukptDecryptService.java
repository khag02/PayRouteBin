package org.jpos.transaction.participant;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOUtil;
import org.jpos.security.*;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

import java.util.Properties;

public class DukptDecryptService {

    public static void main(String[] args) throws Exception {
        String bdkAlias = "test-bdk";
        String ksnHex = "FFFF9876543210E00002";
        String encryptedPinBlockHex = "B76997F83C1479DB";
        String pan = "4012345678909";

        byte[] ksnBytes = ISOUtil.hex2byte(ksnHex);
        byte[] pinBlockBytes = ISOUtil.hex2byte(encryptedPinBlockHex);

        Logger logger = new Logger();
        logger.addListener(new SimpleLogListener());

        JCESecurityModule sm = new JCESecurityModule();
        sm.setLogger(logger, "SM");

        Properties props = new Properties();
        props.put("lmk", "src/main/resources/org/jpos/security/lmk-test");
        sm.setConfiguration(new SimpleConfiguration(props));

        SecureKeyStore keyStore = new SimpleKeyFile("src/main/resources/org/jpos/security/keys-test");
        ((LogSource) keyStore).setLogger(logger, "KEYSTORE");

        SecureDESKey bdk = keyStore.getKey(bdkAlias);

        KeySerialNumber ksn = new KeySerialNumber(ksnBytes);
        EncryptedPIN encryptedPIN = new EncryptedPIN(pinBlockBytes, SMAdapter.FORMAT01, pan);

        EncryptedPIN pinUnderLMK = sm.importPIN(encryptedPIN, ksn, bdk);
        String clearPIN = sm.decryptPIN(pinUnderLMK);

        System.out.println("✅ Decrypted PIN: " + clearPIN);
    }
}
