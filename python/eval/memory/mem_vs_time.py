import psutil
import random
from time import time, sleep
from NonameInstance import NonameInstance
import threading, numpy as np

groupName = "grp"

def mem_usage(pid): # returns memory used in MB
    p = psutil.Process(pid)
    return p.memory_info().rss / 1000000.0

def random_message():
    l = random.randint(5, 200)
    return ''.join(random.choice('0123456789abcdefghijklmnopqrstuvwxyz ') for i in range(l))

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


def random_chatting(instances, length):

    print("Random chatting")
    for i in range(length):
        # pick a random instance
        author = instances[random.randint(0, len(instances)-2)]
        author.sendMessage(groupName, random_message())

        sleep(random.uniform(author.patch_period - 10 * np.sqrt(author.patch_period),
                                                    author.patch_period - 10 * np.sqrt(author.patch_period)) / 1000.0)
        print("\tDone %d out of %d" % (i + 1, length))


def mem_profiler(pid):
    f = open('mem_vs_time_(10,noseal).txt', 'w')
    while True:
        print('%.2f' % mem_usage(pid), file=f, flush=True)
        sleep(1)

def shut_down(instances):
    # shut down all instances
    for inst in instances:
        inst.exit()



if __name__ == "__main__":

    instances = form_clique(10)

    th = threading.Thread(target=mem_profiler, args=(instances[0].proc.pid,))
    th.setDaemon(True)
    th.start()

    P = 100
    print("Measuring memory usage over %d patching periods" % P)
    random_chatting(instances, P) # measure over 30 patch periods
    shut_down(instances)


