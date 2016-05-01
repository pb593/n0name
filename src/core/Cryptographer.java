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
    // Encryption parameters
    public final static Integer encBitLength = 128; // length of key to be used
    public final static String encAlgo = "AES"; // algorithm to be used for encryption
    public final static String modusOperandi = "CTR/PKCS5Padding";


    // MAC parameters
    public final static Integer macByteLength = 32; // length of MACs (in bytes)
    public final static String macAlgo = "HMACSHA256"; // algorithm used for MACs
    public final static Integer macB64StringLength =
                                    (int) Math.ceil(4.0 * macByteLength / 3.0); // length of Base64-encoded MAC

    // Message Digest parameters
    public final static String mdAlgo = "SHA-256"; // used for hashing contents of sealable blocks

    // generator and modulo parameters (constant and static) for Diffie-Hellman
    private static final BigInteger P =
            new BigInteger( "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                            "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                            "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                            "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                            "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                            "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
                            "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
                            "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
                            "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
                            "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
                            "43DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D7" +
                            "88719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA" +
                            "2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6" +
                            "287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED" +
                            "1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA9" +
                            "93B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934063199" +
                            "FFFFFFFFFFFFFFFF", 16);

    private static final BigInteger G = new BigInteger("2");


    /* Other member variables */

    private final SecureRandom random = new SecureRandom(); // secure random number generator
    private final Cipher cipher; // Cipher object used for encryption and decryption
    private BigInteger secretExp; // the secret exponent (aka 'shared secret')
    SecretKey secretKey; // Java object derived from the secret exponent above


    public Cryptographer() {

        secretExp = new BigInteger(encBitLength * 2, random); // initially, secret exponent chosen randomly

        byte[] secretKeyBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretKeyBytes, 0, encBitLength / 8, encAlgo); // init the private key

        Cipher ctemp = null;
        try {
            ctemp = Cipher.getInstance(encAlgo + "/" + modusOperandi);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        this.cipher = ctemp; // choose cipher


    }


    synchronized public BigInteger getDHPublicKey() {
        /* returns pow(G, secretExp) mod P, which is the public key transmitted over the wire */
        return G.modPow(secretExp, P); // return (G ^ secretExp) mod P

    }

    synchronized public void acceptDHPublicKey(BigInteger pubkey){
        /* accepts pubkey sent to us by another participant,
         * exponentiates it with our current secret and updates the secret to the new value */
        BigInteger newSecretExp = pubkey.modPow(secretExp, P); // compute new secret exponent from old one

        secretExp = newSecretExp; // update the secret exponent

        byte[] secretExpBytes = secretExp.toByteArray();
        secretKey = new SecretKeySpec(secretExpBytes, 0, encBitLength / 8, encAlgo);

                                                                // re-instantiate the secret key
    }

    synchronized public void rotateKey(SealableBlock sBlock) {
        /* updates the shared secret and rotates the key on block seal
        *       secret <--- MAC(K, secret)
        *       K <--- MAC(secret, block_contents)
        * */
        secretExp = new BigInteger(macBytes(secretExp.toByteArray())); // update the shared secret

        // temporary update for secret key
        secretKey = new SecretKeySpec(secretExp.toByteArray(), 0, encBitLength / 8, encAlgo);
        byte[] newSecretKeyBytes = macBytes(sBlock.toString().getBytes()); // MAC(secret, block_content)

        // final update to secret key
        secretKey = new SecretKeySpec(newSecretKeyBytes, 0, encBitLength / 8, encAlgo);

    }

    synchronized public String Mac(String input) {
        /* returns MAC(K, input), base64 encoded, where K is the current secret key*/

        byte[] bytes = input.getBytes();
        return Base64.encodeBase64URLSafeString(macBytes(bytes));

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
            System.exit(1); // just crash
        }


        return decrypted;

    }

    private byte[] macBytes(byte[] input) {

        Mac macObj = null;
        try {
            macObj = Mac.getInstance(macAlgo);
            macObj.init(secretKey);
        } catch (NoSuchAlgorithmException e) {
            System.err.printf("Mac algorithm %s not found\n", macAlgo);
            e.printStackTrace();
            System.exit(1);
        } catch (InvalidKeyException e) {
            System.err.println("Attempted to MAC with an invalid key");
            e.printStackTrace();
            System.exit(1);
        }

        return macObj.doFinal(input);

    }

    public static String digest(String input) {
        /* Returns secureHash(input) */
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance(mdAlgo);
            md.update(input.getBytes());
            digest = Base64.encodeBase64URLSafeString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return digest;
    }


}
