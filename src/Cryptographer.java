/**
 * Created by pb593 on 23/11/2015.
 */

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public class Cryptographer {


    // generator and modulo parameters (constant and static)
    private static final BigInteger P = new BigInteger("733395913193084876972058238006528755331084354587635497632061" +
                                            "057108169926295885215157986778370154055962516657468607138398191305808943" +
                                            "0745371908540233246491");

    private static final BigInteger G = new BigInteger("1071284690618491163749824707275830959105571797665644811525928" +
                                            "95973456042802000043315059856238641864669786162808531555929624286416612" +
                                            "57635902833299624300581");


    private final SecureRandom random = new SecureRandom();



    private final Integer bitLength; // length of key to be used

    private final String algorithm; // algorithm to be used for encryption

    private final Cipher cipher;

    private BigInteger secretExp; // the secret exponent (aka 'shared secret')

    SecretKey secretKey; // Java object representing secretExp in encryption / decryption procedures


    public Cryptographer(String algorithm, Integer bitLength) {

        this.algorithm = algorithm; // block cipher + modus operandi
        this.bitLength = bitLength; // number of bits in a key

        secretExp = new BigInteger(bitLength, random); // initially, secret exponent chosen randomly

        byte[] secretKeyBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, algorithm); // init the private key

        Cipher ctemp = null;
        try {
            ctemp = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        this.cipher = ctemp; // choose cipher


    }


    public BigInteger getPublicKey() {
        /* returns pow(G, secretExp) mod P, which is the public key transmitted over the wire */
        return G.modPow(secretExp, P); // return (G ^ secretExp) mod P

    }

    public void acceptPublicKey(BigInteger pubkey){
        /* accepts pubkey sent to us by another participant,
         * exponentiates it with our current secret and updates the secret to the new value */
        BigInteger newSecretExp = pubkey.modPow(secretExp, P); // compute new secret exponent from old one

        secretExp = newSecretExp; // update the secret exponent

        byte[] secretExpBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretExpBytes, 0, secretExpBytes.length, "AES");
                                                                // re-instantiate the secret key

        System.out.printf("New secret exponent: %s\n", secretExp.toString()); //TODO: remove in prod version

    }

    public byte[] encrypt(byte[] plaintext) {

        byte[] encrypted = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            encrypted =  cipher.doFinal(plaintext);
        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            e.printStackTrace(); // will just crash with stack trace if something goes wrong
        }

        return encrypted;

        /*
        // fictitious encrypt for debugging purposes
        byte[] codons = {0x55,(byte)0xFF};
        byte[] rst = new byte[plain.length];
        for(int i = 0; i < plain.length; i++) {
            rst[i] = (byte) (plain[i] ^ codons[i % 2]);
        }

        return rst;
        */
    }

    public byte[] decrypt(byte[] ciphertext) {

        byte[] decrypted = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            decrypted = cipher.doFinal(ciphertext);
        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException e) {
            e.printStackTrace(); // will just crash with stack trace if something goes wrong
        }


        return decrypted;

        /*
        // fictitious decrypt, for debugging
        byte[] codons = {0x55, (byte)0xFF};
        byte[] rst = new byte[cipher.length];
        for(int i = 0; i < cipher.length; i++) {
            rst[i] = (byte)(cipher[i] ^ codons[i % 2]);
        }

        return rst;
        */
    }


}
