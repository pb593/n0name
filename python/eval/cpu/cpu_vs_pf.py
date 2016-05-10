import psutil
from time import sleep
from NonameInstance import NonameInstance
from time import time, sleep
import numpy as np

groupName = "grp"

def cpu_usage(pid):
    p = psutil.Process(pid)
    return p.cpu_percent(interval=1)


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

if __name__ == "__main__":

    f = open('cpu_vs_pf(10).txt', 'w')

    periods = [1000, 3000, 5000, 8000, 10000, 30000]

    for per in periods:

        instances = form_clique(10, per)

        T = int(30 * per / 1000) # measure over 30 patching periods

        data = []
        for i in range(T):
            usg = cpu_usage(instances[0].proc.pid) # 1 sec sleep built-in
            data.append(usg)
            print("%d/%d" % (i + 1, T))

        shut_down(instances)

        print('%f, %.2f' % (per, float(np.mean(data))), file=f, flush=True)

    f.close()
