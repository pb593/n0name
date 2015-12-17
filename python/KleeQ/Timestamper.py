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

from threading import Semaphore

class Timestamper:
    """
    This class is responsible for assigning lamport times to messages.
    To get a time stamp, the time is incremented. It can also have its
    time peeked to observe the current time without modification.

    This class is used by both the transport layer and the UI and as such
    has thread safety. The time variable is locked with _time_lock.

    """
    _time = 0
    def __init__ (self, time = 0):
        """
        The constructor creates a semaphore and sets the initial lamport time
        """
        self._time_lock = Semaphore(0)
        self._time = time
        self._time_lock.release()
    def update(self, time):
        """
        Updates the current time to the maximum between parameter time and current time
        """
        self._time_lock.acquire()
        if (time > self._time):
            self._time = time
        self._time_lock.release()
            
    def peek(self):
        """
        Returns the current time without modification.
        """
        self._time_lock.acquire()
        retval = self._time
        self._time_lock.release()
        return retval
        
        
    def stamp (self):
        """
        Increments the current time and returns the new value.
        """
        self._time_lock.acquire()
        self._time += 1
        retval = self._time
        self._time_lock.release()
        return retval

    
