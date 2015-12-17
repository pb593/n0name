###
# KleeQ: a secure, multicast communication protocol and peer-to-peer client.
# Copyright (C) 2007  Alan Kligman and Joel Reardon
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
###

from Block import Block
from threading import Semaphore

class Listener:
    """
    This class stores message logs when new messages appear. Different viewers can use
    the resulting messages in the manner they prefer.
    """
    def __init__(self, users):
        """
        Constructor for the listener class.
        _lock: Used for thread safety in the producer consumer problem
        _block: Locally stores messages from the import queue
        """
        self._queue_lock = Semaphore(0)
        self._block_lock = Semaphore(0)
        self._block = Block(users)
        self._message_queue = list()
        self._block_lock.release()
        self._queue_lock.release()
        


    def add_message(self, message):
        print "queueadd:acq"
        self._queue_lock.acquire()
        self._message_queue.append(message)
        print "queueadd:rel"
        
        self._queue_lock.release()


    def add_messages(self, messages):
        print "queueadds:acq"
        self._queue_lock.acquire()
        self._message_queue.extend(messages)
        print "queueadds:rel"
        self._queue_lock.release()

    def merge_messages(self):
        """
        Messages are added to a queue so that clique does not need to wait when
        giving messages to the listener. Merge messages will add the queue messages
        into a different data structure as needed
        """
        print "queuemerge:acq"
        self._queue_lock.acquire()
        messages  = self._message_queue[:]
        self._message_queue = list()
        print "queuemerge:rel"
        self._queue_lock.release()
        
        if (len(messages)==0):
            return False
            
        print "blocklock:acq"
        self._block_lock.acquire()
        for msg in messages:
            self._block.add_message(msg)
            
        print "blocklock:rel"
        self._block_lock.release()
        
        return True
        
    def get_as_string(self):
        print "getas:acq"
        self._block_lock.acquire()
        retval = ""
        self._block.first()
        while self._block.cur() != None:
            retval = retval + self._block.cur()._author._name + ": " + self._block.cur()._text + "\n"
            self._block.next()
        print "getas:rel"
        self._block_lock.release()
        return retval
