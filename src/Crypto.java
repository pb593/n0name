/**
 * Created by pb593 on 23/11/2015.
 */

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;


public class Crypto {

    private static BigInteger P = new BigInteger("73339591319308487697205823800652875533108435458763549763206105710" +
                                                "8169926295885215157986778370154055962516657468607138398191305808943" +
                                                "0745371908540233246491");

    private static BigInteger G = new BigInteger("10712846906184911637498247072758309591055717976656448115259289597" +
                                                "3456042802000043315059856238641864669786162808531555929624286416612" +
                                                "57635902833299624300581");


    private DHParameterSpec dhParams = new DHParameterSpec(G, P);

    KeyPair keyPair = null;

    public byte[] commonSecret = null;

    public byte[] generatePubKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParams);
        keyPair = keyGen.generateKeyPair();

        return keyPair.getPublic().getEncoded();
    }

    public void acceptPubKey(byte[] pubKeyBytes) throws Exception{
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey pubKey = KeyFactory.getInstance("DH").generatePublic(x509EncodedKeySpec);

        // create key agreement obj
        KeyAgreement agree = KeyAgreement.getInstance("DH");
        agree.init(keyPair.getPrivate());

        agree.doPhase(pubKey, true);
        commonSecret = agree.generateSecret();
    }

    public byte[] encrypt(byte[] plain) {
        byte[] codons = {0x55,(byte)0xFF};
        byte[] rst = new byte[plain.length];
        for(int i = 0; i < plain.length; i++) {
            rst[i] = (byte) (plain[i] ^ codons[i % 2]);
        }

        return rst;
    }

    public byte[] decrypt(byte[] cipher) {
        byte[] codons = {0x55, (byte)0xFF};
        byte[] rst = new byte[cipher.length];
        for(int i = 0; i < cipher.length; i++) {
            rst[i] = (byte)(cipher[i] ^ codons[i % 2]);
        }

        return rst;
    }


}
