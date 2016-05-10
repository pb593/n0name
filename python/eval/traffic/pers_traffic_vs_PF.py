#!/usr/local/bin/python3
import numpy as np
import os
from time import sleep, time
from NonameInstance import NonameInstance
import random
import urllib.request

M = 30 # number of trials per given patch freq
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

def millis():
    return int(round(time() * 1000))

def form_clique(size, period): # generate the stats

    N = size
    instances = list()

    t0 = time()

    print("Clique formation")
    # create a leader instance
    leaderInst =  NonameInstance(patch_period=period)
    instances.append(leaderInst)
    print("\t%d: Leader instance created with name %s" % (time() - t0, leaderInst.userID))
    # create a new group
    leaderInst.create(groupName)
    print("\t%d: Leader instance created a group named %s" % (time() - t0, groupName))

    # create N-1 more instances of NoNaMe
    for i in range(N-1):
        # create a new instance
        newInst = NonameInstance(patch_period=period)
        instances.append(newInst)

        print("\t%d: Created a new instance with name %s" % (time() - t0, newInst.userID))

        print("\t%d: Wait a bit to make sure everyone gets refreshed AddressBook" % (time() - t0))
        sleep(6) # wait for some time, so that everyone got the new AddressBook

        # leader adds new guy to group
        print("\t%d: Leader instance has added the new guy to group" % (time() - t0))
        leaderInst.add(newInst.userID, groupName)

        print("\t%d: Wait a bit to finalise DH" % (time() - t0))
        sleep(4)  # make sure all DH noise is gone
        print("\t%d: Done %d out of %d" % (time() - t0, i + 1, N - 1))

    print("Clique of size %d with patching period %f has been created." % (size, period))
    return instances


def shut_down(instances):
    # shut down all instances
    for inst in instances:
        inst.exit()


def random_chatting(instances, length):

    print("Random chatting")

    wait_low = instances[0].patch_period - 10 * np.sqrt(instances[0].patch_period)
    wait_high = instances[0].patch_period + 10 * np.sqrt(instances[0].patch_period)


    for i in range(length):
        # pick a random instance
        author = instances[random.randint(0, len(instances)-1)]
        author.sendMessage(groupName, random_message())

        sleep(random.uniform(wait_low, wait_high) / 1000.0)
        print("\tDone %d out of %d" % (i + 1, length))


def process_stats(userID):

    time_slice = 1

    ## process results
    data = dict()
    for row in urllib.request.urlopen("http://pberkovich1994.pythonanywhere.com/saf/stats/get/%s" % userID).read().decode('UTF-8').strip().split("<br>"):
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

if __name__ == "__main__":

    f = open("results/pers_traffic_vs_PF(5).txt", 'w')

    periods = [1000, 3000, 5000, 8000, 10000, 20000, 30000]
    for per in periods:
        print("Period = %f" % per)
        instances = form_clique(5, per)

        start_stats()
        random_chatting(instances, 50)
        #sleep(20 * per / 1000.0)
        stop_stats()

        shut_down(instances)

        times, trafs = process_stats(instances[0].userID)

        print("%.2f, %.2f" % (per, float(np.sum(trafs)) / float((times[-1] - times[0]))), file=f, flush=True) # output the mean of M runs to a file

    f.close()
