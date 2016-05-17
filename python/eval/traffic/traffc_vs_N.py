import random, os
import urllib.request
from NonameInstance import NonameInstance
from time import sleep, time
import matplotlib.pyplot as plt
import numpy as np

groupName = "grp"

def random_message():
    l = random.randint(5, 200)
    return ''.join(random.choice('0123456789abcdefghijklmnopqrstuvwxyz ') for i in range(l))

def reset_saf():
    # reset SaF
    print("Reset SaF")
    urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/reset").read()

def reset_stats():
    # reset stat engine
    print("Reset stat engine")
    urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/stats/reset").read()

def start_stats():
    # start the stat measurement
    print("Start recording stats")
    urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/stats/start").read()


def stop_stats():
    # stop the stat measurement
    urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/stats/stop").read()
    print("Stop measuring stats")

def shut_down(instances):
    # shut down all instances
    for inst in instances:
        inst.exit()


def form_clique(size): # generate the stats

    N = size
    instances = list()

    t0 = time()

    # create a leader instance
    leaderInst =  NonameInstance()
    instances.append(leaderInst)
    print("%d: Leader instance created with name %s" % (time() - t0, leaderInst.userID))
    # create a new group
    leaderInst.create(groupName)
    print("%d: Leader instance created a group named %s" % (time() - t0, groupName))

    # create N-1 more instances of NoNaMe
    for i in range(N-1):
        # create a new instance
        newInst = NonameInstance()
        instances.append(newInst)

        print("%d: Created a new instance with name %s" % (time() - t0, newInst.userID))

        print("%d: Wait a bit to make sure everyone gets refreshed AddressBook" % (time() - t0))
        sleep(6) # wait for some time, so that everyone got the new AddressBook

        # leader adds new guy to group
        print("%d: Leader instance has added the new guy to group" % (time() - t0))
        leaderInst.add(newInst.userID, groupName)

        print("%d: Wait a bit to finalise DH" % (time() - t0))
        sleep(2)  # make sure all DH noise is gone
        print("%d: Done %d out of %d" % (time() - t0, i + 1, N - 1))

    return instances

def process_stats():

    time_slice = 1

    ## process results
    data = dict()
    for row in urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/stats/get_all").read().decode('UTF-8').strip().split("<br>"):
        tokens = row.replace(" ", "").split(",")
        t = int(tokens[0])
        quant = int(tokens[1])
        t = t - t % time_slice
        if t in data:
            data[t]+= quant
        else:
            data[t] = quant

    for t in data:
        data[t] = float(data[t]) / float(time_slice)

    t_vec = sorted(list(data.keys()))
    traffic_vec = [data[t] for t in t_vec]

    return t_vec, traffic_vec


def random_chatting(instances, length):

    print("Random chatting")
    for i in range(length):
        # pick a random instance
        author = instances[random.randint(1, len(instances)-1)]
        author.sendMessage(groupName, random_message())

        sleep(random.uniform(3.0, 6.0))
        print("\tDone %d out of %d" % (i + 1, length))



if __name__ == "__main__":

    f = open('results/alltraffic_vs_N(15)_err.txt', 'w')

    for N in range(2, 12, 3):
        # reset everything
        reset_saf()
        reset_stats()

        # form a clique of 5 members
        instances = form_clique(N)

        start_stats()
        random_chatting(instances, (N-1)*2) # chat in it
        stop_stats()

        shut_down(instances) # shut down instances

        # see what the traffic was like
        t_vec, traf_vec = process_stats()

        print("%d, %.2f, %.2f" % (N, float(np.sum(traf_vec)) / float((t_vec[-1] - t_vec[0])), 1.96 * np.std(np.diff(traf_vec)) / np.sqrt(len(traf_vec))), file=f, flush=True)

    f.close()




