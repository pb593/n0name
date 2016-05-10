import json
import random
import re
import subprocess as sp
from time import sleep


class NonameInstance:
    def __init__(self, userID=None, patch_period=3000):
        if userID is None:
            userID = ''.join(random.choice('0123456789abcdef') for i in range(10))

        self.userID = userID
        self.patch_period = patch_period
        self.proc = sp.Popen(["java", "-jar", "../part2proj.jar", "-m", str(patch_period) , userID], stdin=sp.PIPE, stdout=sp.PIPE)

    def __command(self, cmd):
        cmd = cmd + "\n"
        self.proc.stdin.write(cmd.encode("UTF-8"))
        self.proc.stdin.flush()
        return self.proc.stdout.readline().decode("UTF-8").strip()

    def groupList(self):
        raw = self.__command("groups")
        return re.split("\s+", raw)

    def peerList(self):
        raw = self.__command("peers")
        return re.split("\s+", raw)

    def isOnline(self):
        raw = self.__command("status")
        print(raw)
        if raw == "online":
            return True
        else:
            return False

    def create(self, groupName):
        raw = self.__command("create %s" % (groupName))
        if raw == "ACK":
            return True
        else:
            return False

    def memberList(self, groupName):
        raw = self.__command("members %s" % (groupName))
        return re.split("\s+", raw)

    def add(self, userID, groupName):
        raw = self.__command("add %s %s" % (userID, groupName))
        if raw == "ACK":
            # wait for DH to go through
            while(userID not in self.memberList(groupName)):
                sleep(0.3)
            return True
        else:
            return False

    def sendMessage(self, groupName, txt):
        raw = self.__command("msg %s %s" % (groupName, txt))
        if raw == "ACK":
            return True
        else:
            return False

    def getHistory(self, groupName):
        raw = self.__command("history %s" % (groupName))
        return json.loads(raw)

    def exit(self):
        raw = self.__command("exit")
        if raw == "ACK":
            return True
        else:
            return False

