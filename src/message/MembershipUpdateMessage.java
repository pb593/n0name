package message;

import com.sun.deploy.util.StringUtils;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pb593 on 18/03/2016.
 */
public class MembershipUpdateMessage extends Message {

    public final List<String> members;

    public MembershipUpdateMessage(List<String> members, String author, String cliqueName) {
        super(author, cliqueName);
        this.members = members;
    }

    public MembershipUpdateMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));
        this.members = Arrays.asList(((String)json.get("members")).split(",\\s+"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("members", StringUtils.join(members, ", "));

        return obj;

    }

}
