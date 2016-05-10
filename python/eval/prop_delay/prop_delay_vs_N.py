#!/usr/local/bin/python3
import numpy as np
import random
from time import sleep, time
from NonameInstance import NonameInstance

N = 10
groupName = "grp"


def millis():
    return int(round(time() * 1000))


if __name__ == "__main__":

    instances = list()

    f = open("results/prop_delay_vs_N(3000)_no_seal.txt", "w")

    leaderInst = NonameInstance(patch_period=3000)
    instances.append(leaderInst)

    print("Created leader instance with name %s" % leaderInst.userID)

    # leader creates a group
    leaderInst.create(groupName)
    print("Leader instance has successfully created a group with name %s" % groupName)

    for i in range(1, N):
        print("N = %d" % (i + 1))

        newInst = NonameInstance(patch_period=3000)
        instances.append(newInst)

        print("\tCreated a new instance with name %s" % newInst.userID)

        sleep(5)  # make sure everyone gets the new guy in their address books

        # leader adds the new guy
        leaderInst.add(newInst.userID, groupName)
        sleep(5)  # make sure all DH noise is gone
        print("\tLeader instance has successfully added the newcomer to the group")

        results = list()
        M = 5*(i+1)
        print("\tNewcomer sends %d messages, to estimate how long they propagate." % M)
        for j in range(M):
            txt = str("Hello, %s is here! (%d,%d)" % (newInst.userID, i, j))
            newInst.sendMessage(groupName, txt)
            t0 = millis()

            haventReceived = set([x.userID for x in instances])
            while (haventReceived):
                for inst in instances:
                    hist = inst.getHistory(groupName)
                    l = [(d["author"], d["text"]) for d in hist]
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
