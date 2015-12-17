import threading, time

from flask import Flask
app = Flask(__name__)

lock = threading.Lock()
book = dict() # dictionary: userID -> ("ipaddr:port", last_checkin_time)

@app.route("/check-in/<userID>/<addr>/<int:port>")
def checkin(userID, addr, port): # request to check in
    userID = str(userID)
    addr = str(addr)
    ts = time.time()
    
    lock.acquire()
    book[userID] = (addr + ":" + str(port), ts) # put into book
    lock.release()
    
    return "ACK"

@app.route("/lookup/<userID>")
def lookup(userID): # lookup the address of a user
    rst = "None"
    lock.acquire()
    if userID in book:
        rst = book[userID][0] # take first element of (addr, ts) tuple
    lock.release()
    
    return rst
    
@app.route("/display")
def display(): # for debugging purposes
    rst = ""
    lock.acquire()
    for userID in book:
        (addr, ts) = book[userID]
        rst += "%s %s <br>" % (userID.ljust(25), addr.ljust(25))
    lock.release()
    
    return rst
    


@app.route("/")
def main():
    return "Welcome!"
    
def validator():
    # runs in separate thread, constantly goes through the whole book
    # removing all items older than 7s
    while(True):
        lock.acquire()    
        for userID in book.keys():
            (addr, ts) = book[userID]
            if(int(time.time() - ts) > 7):  # old
                del book[userID]            # remove
        lock.release()
        time.sleep(5) # sleep for 8 sec


if __name__ == "__main__":
    # start the validator
    th = threading.Thread(target = validator)
    th.setDaemon(True) # kil thread when prog exits
    th.start()
    
    # start the webapp
    app.run()