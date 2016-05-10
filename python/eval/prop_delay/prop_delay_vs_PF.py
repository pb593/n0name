#!/usr/local/bin/python3
import numpy as np
import os
from time import sleep, time
from NonameInstance import NonameInstance
import random

M = 30 # number of trials per given patch freq
groupName = "grp"


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
        sleep(2)  # make sure all DH noise is gone
        print("\t%d: Done %d out of %d" % (time() - t0, i + 1, N - 1))

    print("Clique of size %d with patching period %f has been created." % (size, period))
    return instances


def shut_down(instances):
    # shut down all instances
    for inst in instances:
        inst.exit()


if __name__ == "__main__":

    f = open("results/prop_delay_vs_PF(10)_no_seal.txt", 'w')

    periods = [1000, 3000, 5000, 8000, 10000, 20000, 30000]
    for per in periods:
        print("Period = %f" % per)
        instances = form_clique(10, per)

        print("Testing for propagation delay")
        results = list()
        for i in range(M):
            author = instances[0]

            txt = "Hello, %s is here (%d)" % (author.userID, i)
            author.sendMessage(groupName, txt)
            t0 = millis()

            haventReceived = set([inst.userID for inst in instances])
            while (haventReceived):
                for inst in instances:
                    hist = inst.getHistory(groupName)
                    l = [(d["author"], d["text"]) for d in hist]
                    if (author.userID, txt) in l:
                        haventReceived.discard(inst.userID)
                sleep(0.05)

            t1 = millis()

            results.append((t1-t0)/1000.0)
            print("Done %d out of %d" % (i+1, M))

        shut_down(instances)
        print("%.2f, %.2f" % (per, np.mean(results)), file=f, flush=True) # output the mean of M runs to a file

    f.close()
