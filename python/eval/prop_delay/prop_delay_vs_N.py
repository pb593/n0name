#!/usr/local/bin/python3
import numpy as np
import os
from time import sleep, time
from NonameInstance import NonameInstance

N = 30
M = 3
groupName = "grp"


def millis():
    return int(round(time() * 1000))


if __name__ == "__main__":

    instances = list()

    os.chdir("prop_delay")

    f = open("results/prop_delay_vs_N(4).txt", "w")

    leaderInst = NonameInstance()
    instances.append(leaderInst)

    print("Created leader instance with name %s" % leaderInst.userID)

    # leader creates a group
    leaderInst.create(groupName)
    print("Leader instance has successfully created a group with name %s" % groupName)

    for i in range(1, N):
        print("N = %d" % (i + 1))

        newInst = NonameInstance()
        instances.append(newInst)

        print("\tCreated a new instance with name %s" % newInst.userID)

        sleep(7)  # make sure everyone gets the new guy in their address books

        # leader adds the new guy
        leaderInst.add(newInst.userID, groupName)
        sleep(2)  # make sure all DH noise is gone
        print("\tLeader instance has successfully added the newcomer to the group")

        # new guy sends out messages, to see how long they propagate
        results = list()
        print("\tNewcomer sends %d messages, to estimate how long they propagate." % M)
        for j in range(M):
            txt = str("Hello, %s is here! (%d)" % (newInst.userID, j))
            newInst.sendMessage(groupName, txt)
            t0 = millis()

            haventReceived = set(map(lambda x: x.userID, instances))
            while (haventReceived):
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
