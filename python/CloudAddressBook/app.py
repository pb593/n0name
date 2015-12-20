import threading, time
import logging

from flask import Flask
app = Flask(__name__)

# configure the logger
logging.basicConfig(format = "%(asctime)s %(levelname)s %(message)s",
                    filename = 'mylog.txt', level = logging.DEBUG)

book_lock = threading.Lock()
book = dict() # dictionary: userID -> ("ipaddr:port", last_checkin_time)

@app.route("/check-in/<userID>/<addr>/<int:port>")
def checkin(userID, addr, port): # request to check in
    userID = str(userID)
    addr = str(addr)
    ts = time.time()
    logging.info("CHECK-IN by user %s from %s. TS = %s" 
        % (userID, addr + str(port), str(ts)))
    
    book_lock.acquire()
    logging.debug("book_lock:acq")
    book[userID] = (addr + ":" + str(port), ts) # put into book
    book_lock.release()
    logging.debug("book_lock:rel")
    
    return "ACK"

@app.route("/lookup/<userID>")
def lookup(userID): # lookup the address of a user
    rst = "None"
    logging.info("LOOKUP of user %s" % userID)

    book_lock.acquire()
    logging.debug("book_lock:acq")
    if userID in book:
        rst = book[userID][0] # take first element of (addr, ts) tuple
    book_lock.release()
    logging.debug("book_lock:rel")
    
    return rst
    
@app.route("/display")
def display(): # for debugging purposes
    logging.info("DISPLAY request received")
    
    rst = ""
    book_lock.acquire()
    logging.debug("book_lock:acq")
    logging.debug("Address book contains %d entries" % len(book))
    for userID in book:
        (addr, ts) = book[userID]
        rst += "%s %s <br>" % (userID.ljust(25), addr.ljust(25))
    book_lock.release()
    logging.debug("book_lock:rel")
    
    return rst
    


@app.route("/")
def main():
    return "Welcome!"
    
def validator():
    # runs in separate thread, constantly goes through the whole book
    # removing all items older than 7s
    logging.debug("[VALIDATOR] thread started")
    
    while(True):
        logging.debug("[VALIDATOR] wakes up")
        book_lock.acquire()    
        logging.debug("[VALIDATOR] book_lock:acq")
        for userID in book.keys():
            (addr, ts) = book[userID]
            if(int(time.time() - ts) > 7):  # old
                logging.debug("[VALIDATOR] deletes expired entry for %s"%userID)
                del book[userID]            # remove
        book_lock.release()
        logging.debug("[VALIDATOR] book_lock:rel")
        time.sleep(5) # sleep for 8 sec


if __name__ == "__main__":
    # start the validator
    th = threading.Thread(target = validator)
    th.setDaemon(True) # kill thread when prog exits
    th.start()
    
    # start the webapp
    app.run()