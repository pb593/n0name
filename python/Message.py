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

class messages.Message(object):
    """
    This class represents a single message sent in the clique. It consists of the
    author, the text, the time (lamport time), and the type of message.
    There are different types of messages that are used, but the formost is
    kleeq/text, which is a text message sent to the clique.

    """
    def __init__(self, author, text, time  = 0 , type='kleeq/text'):
        """
        Initializes the member variables of the message. There is an author, text,
        time and type as described in the class doc.
        """
        self._author = author
        self._text = text
        self._time = time 
        self._type = type
    
    def __lt__(self, other):
        """
        a method to see if one message is less than another. The critia are that anything < None
        except None itself. Then any message occuring at an earlier time is less than later.
        If both happen at the same time, then it based on an ordering (alphabetically) of the
        author's names
        """
        if other == None:
            return True
        elif self == None:
            return False
        elif self._time < other._time:
            return True
        elif self._time > other._time:
            return False
        else:
            return self._author < other._author
    
    def __gt__(self,other):
        """
        override for greater than. Returns the opposite of less than
        """
        if other == None:
            return False
        elif self == None:
            return True
        else:
            return not self.__lt__(other)
    
    def file_trace(self,file):
        """
        a debug method to trace this class to a file
        """
        file.write("Time:" +str(self._time)+"\tAuthor:"+ self._author._name+ "\tText="+ self._text+'\n')

    def trace(self):
        """
        a debug method to trace this class to stdout
        """
        print "Time:", self._time, "Author:", self._author._name, "Text=", self._text

    def string(self):
        """
        a method to return this message as a string.
        """
        return str(self._time) + "#" + self._author._name + "#" + self._text +"##"
