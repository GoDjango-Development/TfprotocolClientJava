package dev.godjango.tfprotocol.security;

import javax.crypto.Cipher;

import dev.godjango.tfprotocol.TFExceptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class Cryptography {
    public static byte[] getRandomBytes(int length){
        byte[] result = new byte[length];
        byte distanceFactor = 1;
        for (int i = 0; i < length; i++){
            result[i] = (byte) (((System.currentTimeMillis() * distanceFactor)%126)+1);
            distanceFactor += (i+1)*result[i];
        }
        return result;
    }
    public static byte[] sha256(byte[] array){
        try{
            byte[] hex_bytes = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(array);
            byte[] chars = new byte[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int t = bytes[i] & 0xff;
                chars[i*2] = hex_bytes[t >>> 4];
                chars[i*2 + 1] = hex_bytes[t & 0x0f];
            }
            String st = "0x" + new String(chars, "US-ASCII").toLowerCase();
            return st.getBytes("UTF-8");
        }catch(Exception ignored){
            System.out.println("An error ocurred and was ignored in sha256");
            return new byte[0];
        }
    }
    public static byte[] sha256(InputStream is){
        try{ 
            byte[] hex_bytes = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
            byte[] bytes = new DigestInputStream(is, MessageDigest.getInstance("SHA-256")).getMessageDigest().digest();
            byte[] chars = new byte[bytes.length * 2];
            for (int i = 2; i < bytes.length; i++) {
                int t = bytes[i] & 0xff;
                chars[i*2] = hex_bytes[t >>> 4];
                chars[i*2 + 1] = hex_bytes[t & 0x0f];
            }
            String st = "0x" + new String(chars, "US-ASCII").toLowerCase();
            return st.getBytes("UTF-8");
        }catch(Exception ignored){
            System.out.println("An error ocurred and was ignored in sha256");
            return new byte[0];
        }
    }
    public static class RSA{
        public static byte[] encrypt(byte[] payload, String publicKey){
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] parsedPublicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----","").
                        replaceAll("\n", "").
                        replace("-----END PUBLIC KEY-----", "")
                        .getBytes();
                Base64.decode(parsedPublicKey);
                PublicKey publicKeyObject = keyFactory.generatePublic(new X509EncodedKeySpec(parsedPublicKey));
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKeyObject);
                return cipher.doFinal(payload,0, payload.length);
            } catch (Exception e) {
                throw new TFExceptions(e);
            }
        }
    }
    public static class Xor{
        private static byte[] sessionKey;
        private byte[] key;
        private long seed;
        public Xor(byte[] key){
            Cryptography.Xor.sessionKey = key.clone();
            this.key = key.clone();
            this.seed = this.getNewSeed();
        }
        private long getNewSeed(){
            /*return key[0] | key[1] << 8 | key[2] << 16 | key[3] << 24 |
                (long)key[4] << 32 |  (long)key[5] << 40 | (long)key[6] << 48 |
	            (long) key[7] << 56;*/
            return ByteBuffer.wrap(Arrays.copyOfRange(this.key, 0, Long.BYTES))
                    .order(ByteOrder.LITTLE_ENDIAN).getLong();
        }
        public static byte[] getStaticSessionKey(){
            return sessionKey;
        }
        public byte[] encrypt(byte[] payload){
            for (int c = 0; c < payload.length; c++) {
                payload[c] ^= this.key[c%this.key.length];
                /* Pack data to send */
                payload[c] += (byte)(this.seed >> 56 & 0xFF);
                /* Change seed and encryption key */
                this.seed = this.seed * (this.seed >> 8 & 0xFFFFFFFFL) +
                        (this.seed >> 40 & 0xFFFF);
                if (this.seed == 0){
                    this.seed = getNewSeed();
                }
                this.key[c%this.key.length] = (byte) (this.seed % 256);
            }
            return payload;
        }
        public byte[] decrypt(byte[] payload){
            for (int c=0; c < payload.length; c++) {
                /* Unpack received data */
                payload[c] -=  (byte)(this.seed >> 56 & 0xFF);
                /* Decrypt received data */
                payload[c] ^= this.key[c%this.key.length];
                this.seed = this.seed * (this.seed >> 8 & 0xFFFFFFFFL) +
                        (this.seed >> 40 & 0xFFFF);
                if (this.seed == 0){
                    this.seed = getNewSeed();
                }
                this.key[c%this.key.length] = (byte) (this.seed % 256);
            }
            return payload;
        }
        public long getSeed(){
            return this.seed;
        }
    }
    public static class AES{
    }
}
