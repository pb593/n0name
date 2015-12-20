import threading, time
import logging

from flask import Flask
app = Flask(__name__)

# configure the logger
logging.basicConfig(format = "%(asctime)s %(levelname)s %(message)s",
                    filename = 'mylog.txt', level = logging.DEBUG)

book = dict() # dictionary: userID -> ("ipaddr:port", last_checkin_time)

# start the validator thread
def validator():
    # goes through the whole book
    # removing all items older than 7s
    logging.info("[VALIDATOR] method called")
    
    for userID in book.keys():
        (addr, ts) = book[userID]
        if(int(time.time() - ts) > 7):  # old
            logging.info("[VALIDATOR] deletes expired entry for %s"%userID)
            del book[userID]            # remove

@app.route("/check-in/<userID>/<addr>/<int:port>")
def checkin(userID, addr, port): # request to check in

    validator() # validate all records
    
    userID = str(userID)
    addr = str(addr)
    ts = time.time()
    logging.info("CHECK-IN by user %s from %s. TS = %s" 
        % (userID, addr + str(port), str(ts)))
    
    book[userID] = (addr + ":" + str(port), ts) # put into book
    
    return "ACK"

@app.route("/lookup/<userID>")
def lookup(userID): # lookup the address of a user

    rst = "None"
    logging.info("LOOKUP of user %s" % userID)
    
    validator() # validate all records

    if userID in book:
        rst = book[userID][0] # take first element of (addr, ts) tuple
    
    return rst
    
@app.route("/display")
def display(): # for debugging purposes

    logging.info("DISPLAY request received")
    
    validator() # validate all records
    
    rst = ""
    logging.info("Address book contains %d entries" % len(book))
    for userID in book:
        (addr, ts) = book[userID]
        rst += "%s %s <br>" % (userID.ljust(25), addr.ljust(25))
    
    return rst
    


@app.route("/")
def main():
    
    validator() # validate all records
    
    return "Welcome!"
    
@app.errorhandler(404)
def page_not_found(e):
    
    validator() # validate all records
    
    return 'Sorry, nothing at this URL.', 404

# only for debugging, delete in deployment    
#if __name__ == "__main__":
#    app.run()