#!/usr/local/bin/python3

import random
import subprocess as sp
from time import sleep

from NonameInstance import NonameInstance

N = 10

if __name__ == "__main__":

    noname1 = NonameInstance()
    noname2 = NonameInstance()

    noname1.create("g")
    noname1.add(noname2.userID, "g")
    print(noname1.memberList("g"))
    print(noname2.memberList("g"))
    noname1.exit()
    noname2.exit()