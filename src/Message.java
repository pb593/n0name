import java.io.Serializable;

public class Message implements Serializable{

    public String str;

    static final long serialVersionUID = 42L;

    public Message(String str){
        this.str = str;
    }

}
