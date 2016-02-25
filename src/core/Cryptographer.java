package core; /**
 * Created by pb593 on 23/11/2015.
 */

import message.Message;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;

public class Cryptographer {

    /* Static constants */

    public final static Integer encBitLength = 256; // length of key to be used

    public final static String encAlgo = "AES"; // algorithm to be used for encryption

    public final static String modusOperandi = "CTR/PKCS5Padding";

    public final static Integer macByteLength = 32; // length of MACs (in bytes)

    public final static String macAlgo = "HmacSHA256"; // algorithm used for MACs

    public final static Integer macB64StringLength =
                                    (int) Math.ceil(4.0 * macByteLength / 3.0); // length of Base64-encoded MAC

    // generator and modulo parameters (constant and static)
    private static final BigInteger P = new BigInteger("733395913193084876972058238006528755331084354587635497632061" +
                                            "057108169926295885215157986778370154055962516657468607138398191305808943" +
                                            "0745371908540233246491");

    private static final BigInteger G = new BigInteger("1071284690618491163749824707275830959105571797665644811525928" +
                                            "95973456042802000043315059856238641864669786162808531555929624286416612" +
                                            "57635902833299624300581");


    /* Other member variables */

    private final SecureRandom random = new SecureRandom();

    private final Cipher cipher;

    private BigInteger secretExp; // the secret exponent (aka 'shared secret')

    SecretKey secretKey; // Java object representing secretExp in encryption / decryption procedures


    public Cryptographer() {

        secretExp = new BigInteger(encBitLength, random); // initially, secret exponent chosen randomly

        byte[] secretKeyBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretKeyBytes, 0, encBitLength / 8, encAlgo); // init the private key

        Cipher ctemp = null;
        try {
            ctemp = Cipher.getInstance(encAlgo + "/" + modusOperandi);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        this.cipher = ctemp; // choose cipher


    }


    synchronized public BigInteger getPublicKey() {
        /* returns pow(G, secretExp) mod P, which is the public key transmitted over the wire */
        return G.modPow(secretExp, P); // return (G ^ secretExp) mod P

    }

    synchronized public void acceptPublicKey(BigInteger pubkey){
        /* accepts pubkey sent to us by another participant,
         * exponentiates it with our current secret and updates the secret to the new value */
        BigInteger newSecretExp = pubkey.modPow(secretExp, P); // compute new secret exponent from old one

        secretExp = newSecretExp; // update the secret exponent

        byte[] secretExpBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretExpBytes, 0, encBitLength / 8, "AES");

                                                                // re-instantiate the secret key

        // System.out.printf("New secret exponent: %s\n", new String(secretKey.getEncoded()));
        // TODO: remove in prod version

    }

    synchronized public String Mac(String input) {

        Mac macObj = null;
        try {
            macObj = Mac.getInstance(macAlgo);
            macObj.init(secretKey);
        } catch (NoSuchAlgorithmException e) {
            System.err.printf("Mac algorithm %s not found\n", macAlgo);
            e.printStackTrace();
            System.exit(-1);
        } catch (InvalidKeyException e) {
            System.err.println("Attempted to MAC with an invalid key");
            e.printStackTrace();
            System.exit(-1);
        }

        return Base64.encodeBase64URLSafeString(macObj.doFinal(input.getBytes()));

    }

    synchronized public String encryptMsg(Message msg) {
        byte[] bytes = msg.toJSON().toJSONString().getBytes();
        byte[] bytesEncrypted = encryptBytes(bytes);
        return Base64.encodeBase64URLSafeString(bytesEncrypted);
    }

    synchronized public Message decryptMsg(String urlSafeString) {
        byte[] bytesEncrypted = Base64.decodeBase64(urlSafeString);
        byte[] bytesDecrypted = decryptBytes(bytesEncrypted);
        String jsonString = new String(bytesDecrypted);
        Message msg = null;
        try {
            msg = Message.fromJSON(jsonString);
        } catch (ParseException e) {
            System.err.printf("Unable to decrypt message\n");
            e.printStackTrace();
        }

        return msg; // will return null if an error happens
    }

    private byte[] encryptBytes(byte[] plaintext) {

        // generate a random initial vector
        byte[] ivBytes = new byte[cipher.getBlockSize()];
        random.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        byte[] encrypted = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            encrypted =  cipher.doFinal(plaintext);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace(); // will just crash with stack trace if something goes wrong
        }

        // concatenate IV and encrypted bytes
        byte[] ivAndEncrypted = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, ivAndEncrypted, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, ivAndEncrypted, ivBytes.length, encrypted.length);

        return ivAndEncrypted;

    }

    private byte[] decryptBytes(byte[] ivAndCiphertext) {

        // extract the IV from message
        byte[] ivBytes = new byte[cipher.getBlockSize()];
        System.arraycopy(ivAndCiphertext, 0, ivBytes, 0, cipher.getBlockSize()); // extract iv bytes
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        // extract the ciphertext bytes
        byte[] ciphertext = new byte[ivAndCiphertext.length - cipher.getBlockSize()];
        System.arraycopy(ivAndCiphertext, cipher.getBlockSize(), ciphertext, 0, ciphertext.length);

        byte[] decrypted = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            decrypted = cipher.doFinal(ciphertext);
        } catch(BadPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            System.exit(-1); // just crash
        }


        return decrypted;

    }


}