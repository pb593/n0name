import threading, time, os, sys
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

# stats
t0 = time.time()
stats_dir = "stats"
all_stat_file = "all.txt"
keep_stats = False

@app.route("/saf/view/<userID>")
def saf_view(userID): # for debugging purposes, just shows what's in the postbox

    userID = str(userID)
    if (userID in messages) and (messages[userID]):
        return "<br>".join(messages[userID])
    else:
        return "This store-n-forward postbox is empty/non-existent"


@app.route("/saf/retrieve/<userID>")
def saf_retrieve(userID): # check the postbox to see if there are new messages
    # HAS SIDE EFFECT: deletes messages after returning them
    
    userID = str(userID)
    if userID in messages:
        rst = "\n".join(messages[userID])
        del messages[userID]
        print(keep_stats)
        if keep_stats:
            print("Wrote stats to files!")
            t = time.time()
            size = sys.getsizeof(rst)
            # record user-specific traffic consumption
            with open(stats_dir + "/" + userID + ".txt", 'a') as f:
                f.write("%d, %d\n" % (t - t0, size))
                f.flush()
            # record system-wide traffic consumption
            with open(stats_dir + "/" + all_stat_file, 'a') as f:
                f.write("%d, %d\n" % (t - t0, size))
                f.flush()
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

@app.route("/saf/reset")
def saf_reset():
    messages.clear() # just clear all the postboxes
    return "ACK"
        
@app.route("/saf/stats/start")
def stats_start():
    global t0
    t0 = time.time() # reset the time
    global keep_stats
    keep_stats = True
    print("Switched on stats!")
    return "ACK"
    
@app.route("/saf/stats/stop")
def stats_stop():
    global keep_stats
    keep_stats = False
    print("Switched off stats!")
    return "ACK"
    
@app.route("/saf/stats/reset")
def stats_reset():
    # clear all stat files
    for fname in os.listdir(stats_dir):
        try:
            file_path = os.path.join(stats_dir, fname)
            if os.path.isfile(file_path):
                os.unlink(file_path)
        except Exception as e:
            rst = "Error"
    rst = "ACK"
    return rst
    
@app.route("/saf/stats/get_all")
def stats_reset():
    if(os.path.isfile('stats/all.txt')):
        rst = ""
        with open('stats/all.txt', 'r') as f:
            for line in f:
                rst+=(line+"\n")
        return rst
    else:
        return "Stats file does not exist"
    
@app.route("/saf/stats/get/<userID>")
def stats_reset(userID):
    if(os.path.isfile('stats/%s.txt') % userID):
        rst = ""
        with open('stats/.txt', 'r') as f:
            for line in f:
                rst+=(line+"\n")
        return rst
    else:
        return "Stats file does not exist"
                
    
    
    

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
#if __name__ == "__main__":
#    app.run()