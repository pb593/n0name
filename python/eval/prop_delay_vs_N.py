#!/usr/local/bin/python3

import random
import subprocess as sp
from time import sleep, time

from NonameInstance import NonameInstance

N = 10
instances = list()
groupName = "grp"

if __name__ == "__main__":


    for i in range(N):
        instances.append(NonameInstance())
    print("%d instances created." % (N))

    print("Wait for 5 sec for everyone to get a fresh copy of AddressBook")
    sleep(5) # wait for a bit for noise to settle down

    # nominate leader who creates a clique and adds everyone
    leaderInst = instances[0]
    print("User %s takes the lead to create a group" % (leaderInst.userID))
    leaderInst.create(groupName) # create group
    print("Group with name %s created." % (groupName))
    t0 = time()
    for i in instances:
        if i.userID != leaderInst.userID: # add everyone except myself
            if leaderInst.add(i.userID, groupName):
                print("Successfully added user %s" % (i.userID))

    t1 = time()
    print("All %d successfully added, it took %d" % (N, t1-t0))


    print("Wait for 3 sec for all noise to settle down")
    sleep(3) # wait for a bit for noise to settle down

    # leader sends a message – measure how much time it takes to propagate




    for i in instances:
        i.exit()