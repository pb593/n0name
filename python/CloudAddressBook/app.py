import threading, time
from IPy import IP

from flask import Flask, request
app = Flask(__name__)


######### address book functionality

book = dict() # dictionary: userID -> ("ipaddr:port", last_checkin_time)

# start the validator thread
def validator():
    # goes through the whole book
    # removing all items older than 7s
    
    for userID in book.keys():
        (addr, ts) = book[userID]
        if(int(time.time() - ts) > 7):  # old
            del book[userID]            # remove

@app.route("/check-in/<userID>/<addr>/<int:port>")
def checkin(userID, addr, port): # request to check in

    validator() # validate all records
    
    userID = str(userID)
    addr = str(addr)
    ts = time.time()

    # 0.0.0.0:0 is an indicator for clients to use store-n-forward instead of P2P
    if(IP(addr).iptype() is "PRIVATE"): # if the ip address in private (perhaps user behind NAT)
        addr = "0.0.0.0"
        port = 0
    
    book[userID] = (addr + ":" + str(port), ts) # put into book
    
    return "ACK"

@app.route("/lookup/<userID>")
def lookup(userID): # lookup the address of a user
    
    validator() # validate all records
    
    rst = "None"
    if userID in book:
        rst = book[userID][0] # take first element of (addr, ts) tuple
    
    return rst
    
@app.route("/display")
def display():
    
    validator() # validate all records
    
    rst = ""
    for userID in book:
        (addr, ts) = book[userID]
        rst += "%s %s <br>" % (userID.ljust(25), addr.ljust(25))
    
    return rst
  

######### store-n-forward (saf) functionality


messages = dict() # dictionary: userID -> list{message} 

@app.route("/saf/view/<userID>")
def saf_view(userID): # for debugging purposes, just shows what's in the postbox

    userID = str(userID)
    if (userID in messages) and (messages[userID]):
        return "<br>".join(messages[userID])
    else:
        return "This store-n-forward postbox is empty/non-existent"


@app.route("/saf/retrieve/<userID>")
def saf_retrieve(userID): # check the postbox to see if there are any new messages
                    # HAS SIDE EFFECT: deletes messages after returning them
    
    userID = str(userID)
    if userID in messages:
        rst = "\n".join(messages[userID])
        del messages[userID]
        return rst
    else:
        return "None"

@app.route("/saf/store/<userID>", methods = ['POST'])
def saf_store(userID): # used for sending messages to the user using the store-n-forward service

    userID = str(userID)
    msg = str(request.form["msg"])

    if userID not in messages: # if postbox does not exist
        messages[userID] = list() # create new list

    messages[userID].append(msg) # append message into the postbox

    return "ACK"




######### Other functions

@app.route("/")
def welcome():
    
    validator() # validate all records
    
    return "Welcome!"
    
@app.errorhandler(404)
def page_not_found(e):
    
    validator() # validate all records
    
    return 'Sorry, nothing at this URL.', 404

#only for debugging, delete in deployment    
if __name__ == "__main__":
    app.run()