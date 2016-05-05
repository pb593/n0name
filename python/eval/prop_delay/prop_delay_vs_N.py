#!/usr/local/bin/python3

import subprocess as sp
from time import sleep, time

import numpy as np

from common.NonameInstance import NonameInstance

N = 30
groupName = "grp"

def millis():
    return int(round(time() * 1000))

def prop_delay_vs_N(Nmax):


    instances = list()

    f = open("prop_delay_vs_N(3).txt", "w")


    leaderInst = NonameInstance()
    instances.append(leaderInst)

    print("Created leader instance with name %s" % leaderInst.userID)

    # leader creates a group
    leaderInst.create(groupName)
    print("Leader instance has successfully created a group with name %s" % groupName)

    for i in range(1, Nmax):
        print("N = %d" % (i + 1))

        newInst = NonameInstance()
        instances.append(newInst)

        print("\tCreated a new instance with name %s" % newInst.userID)

        sleep(10) # make sure everyone gets the new guy in their address books

        # leader adds the new guy
        leaderInst.add(newInst.userID, groupName)
        sleep(5) # make sure all DH noise is gone
        print("\tLeader instance has successfully added the newcomer to the group")

        # new guy sends out messages, to see how long they propagate
        M = 50
        results = list()
        print("\tNewcomer sends %d messages, to estimate how long they propagate." % M)
        for j in range(M):
            txt = str("Hello, %s is here! (%d)" % (newInst.userID, j))
            newInst.sendMessage(groupName, txt)
            t0 = millis()

            haventReceived = set(map(lambda x: x.userID, instances))
            while(haventReceived):
                for inst in instances:
                    hist = inst.getHistory(groupName)
                    l = list(map(lambda d: (d["author"], d["text"]), hist))
                    if (newInst.userID, txt) in l:
                        haventReceived.discard(inst.userID)
                sleep(0.05)

            t1 = millis()

            t_delta = t1 - t0
            results.append(float(t_delta) / 1000.0)
            print("\tDone %d out of %d" % (j + 1, M))

        average = np.mean(results)
        print("\tAverage delay for N=%d is %.2f" % (i + 1, average))
        print("%d, %.3f" % (i + 1, average), file=f, flush=True)

    f.close()

    # shut down all instances
    for inst in instances:
        inst.exit()

    print("Finished!")


def snapshot():

    f = open("nettop.txt", "r")

    while(True):
        for line in f:
            print(line)

        f.seek(0)
        sp.call("clear")


if __name__ == "__main__":

    prop_delay_vs_N(10)