#!/usr/local/bin/python3

import random
import subprocess as sp
from time import sleep, time
from datetime import datetime
import numpy as np
import matplotlib.pyplot as plt
from NonameInstance import NonameInstance

N = 30
groupName = "grp"

def millis():
    return int(round(time() * 1000))

def prop_delay_vs_N(Nmax):


    instances = list()
    Ns = []
    delays = []


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

        sleep(5) # make sure everyone gets the new guy in their address books

        # leader adds the new guy
        leaderInst.add(newInst.userID, groupName)
        sleep(2) # make sure all DH noise is gone
        print("\tLeader instance has successfully added the newcomer to the group")

        # new guy sends out messages, to see how long they propagate
        M = 10
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
        Ns.append(i+1)
        delays.append(average)

    # shut down all instances
    for inst in instances:
        inst.exit()

    # plot the results
    plt.plot(Ns, delays)
    plt.show()


if __name__ == "__main__":


    prop_delay_vs_N(20)

    # for i in range(N):
    #     instances.append(NonameInstance())
    # print("%d instances created." % (N))
    #
    # print("Wait for 5 sec for everyone to get a fresh copy of AddressBook")
    # sleep(5) # wait for a bit for noise to settle down
    #
    # # nominate leader who creates a clique and adds everyone
    # leaderInst = instances[0]
    # print("User %s takes the lead to create a group" % (leaderInst.userID))
    # leaderInst.create(groupName) # create group
    # print("Group with name %s created." % (groupName))
    # t0 = time()
    # for i in instances:
    #     if i.userID != leaderInst.userID: # add everyone except myself
    #         if leaderInst.add(i.userID, groupName):
    #             print("Successfully added user %s" % (i.userID))
    #
    # t1 = time()
    # print("All %d successfully added, it took %d" % (N, t1-t0))
    #
    #
    # print("Wait for 3 sec for all noise to settle down")
    # sleep(3) # wait for a bit for noise to settle down
    #
    # # leader sends a message – measure how much time it takes to propagate
    # txt = "Hello, everyone!"
    # print("Sending a test message to everyone in group")
    # leaderInst.sendMessage(groupName, txt)
    # t2 = time()
    # haventReceived = set(map(lambda x:x.userID, instances))
    # print(haventReceived)
    # while(haventReceived):
    #     for i in instances:
    #         hist = i.getHistory(groupName)
    #         l = list(map(lambda d: (d["author"], d["text"]), hist))
    #         if (leaderInst.userID, txt) in l:
    #             haventReceived.discard(i.userID)
    #     sleep(0.05)
    #
    #
    # t3 = time()
    # print("Everyone received it! It took %d s" % (t3 - t2))
    #
    #
    # for i in instances:
    #     i.exit()