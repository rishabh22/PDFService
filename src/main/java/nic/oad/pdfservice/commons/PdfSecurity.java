package nic.oad.pdfservice.commons;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PdfSecurity {
    public static String encrypt(String input, String key) {
        byte[] crypted = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            crypted = cipher.doFinal(input.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return crypted != null ? new String(Base64.encodeBase64(crypted)) : "";
    }

    public static String decrypt(String input, String key) {
        byte[] output = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skey);
            output = cipher.doFinal(Base64.decodeBase64(input));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output != null ? new String(output) : "";
    }

    /*public static void main(String[] args) {
        String key = "DiGnIcPrOcEsSiD!";
        String data = "example";
        System.out.println(PdfSecurity.decrypt(PdfSecurity.encrypt(data, key), key));
        System.out.println(PdfSecurity.encrypt(data, key));
    }*/
}