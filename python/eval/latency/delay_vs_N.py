import random
from time import sleep, time

import numpy as np

from NonameInstance import NonameInstance

Nmax = 20
groupName = "grp"

PERIOD = 3000


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

    f = open('latency_vs_N(%d)_' % PERIOD, 'w')

    for N in range(2, Nmax):

        print("N=%d" % N)

        instances = form_clique(N, PERIOD)


        M = 50
        print("Need to send %d messages" % M)
        results = list()
        for i in range(M):

            author = instances[random.randint(0, len(instances)-1)]
            txt = "%d, %d" % (N, i)

            author.sendMessage(groupName, txt)

            t0 = millis()

            haventReceived = set([x.userID for x in instances])
            while haventReceived:
                for inst in instances:
                    hist = inst.getHistory(groupName)
                    l = [(d["author"], d["text"]) for d in hist]
                    if (author.userID, txt) in l:
                        haventReceived.discard(inst.userID)
                sleep(0.05)

            t1 = millis()

            results.append((t1-t0) / 1000.0)

            print("Done %d out of %d" % (i+1, M))

        print("Average for N = %d is %.2f" % (N, np.mean(results)))

        s = ','.join([str(r) for r in results])
        print("%d: %s" % (N, s), file = f, flush=True)

        shut_down(instances)

