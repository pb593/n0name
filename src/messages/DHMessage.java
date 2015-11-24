package messages;

import java.io.Serializable;

/**
 * Created by pb593 on 24/11/2015.
 */
public class DHMessage extends Message implements Serializable {

    public final byte[] publicKey;

    public DHMessage(byte[] publicKey) {
        super("");
        this.publicKey = publicKey;
    }

}

