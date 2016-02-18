package core;

import interfaces.JSONizable;
import org.json.simple.JSONObject;

import java.util.HashMap;

/**
 * Created by pb593 on 18/02/2016.
 */
public class VectorClock implements JSONizable{

    private final HashMap<String, Integer> vectorClk;

    public VectorClock() {
        vectorClk = new HashMap<>();
    }

    public VectorClock(JSONObject jObj) {
        // jObj is a previously parsed JSONObject
        vectorClk = (HashMap<String, Integer>)jObj;
    }

    public void increment(String userID) {
        if(!vectorClk.containsKey(userID)){ // no such user in map
            vectorClk.put(userID, 1);
        }
        else {
            Integer clk = vectorClk.get(userID);
            vectorClk.put(userID, clk + 1); // put the increment in
        }

    }

    public Integer get(String userID) {
        // return the msg count if user is known and 0 otherwise
        return vectorClk.containsKey(userID) ? vectorClk.get(userID) : 0;
    }

    public void set(String userID, Integer clk) {
        vectorClk.put(userID, clk);
    }

    public static VectorClock diff(VectorClock v1, VectorClock v2) {
        /*
        *   Returns a new VectorClock which is v1 - v2.
        * */
        VectorClock result = new VectorClock();

        for(String userID: v1.vectorClk.keySet()) {
            Integer n1 = v1.vectorClk.get(userID);
            Integer n2 = v2.vectorClk.containsKey(userID) ? v2.vectorClk.get(userID) : 0;
            result.vectorClk.put(userID, n1 - n2);
        }

        return result;


    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject(vectorClk);
        return obj;

    }

    public void print() {
        // for debugging
        for (String userID : vectorClk.keySet()) {
            System.out.printf("%s: %s, ", userID, vectorClk.get(userID));
        }
        System.out.println();

    }
}
