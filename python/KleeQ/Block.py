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

from Message import Message
import sys

class Block:
    """
    The block class contains a set of messages by the clique members. It is the parent
    class for the following: openblock, sealedblock, and patchblock. It is an iterable
    class, which means that there is a cur() function that returns the current message,
    and there are first/last/prev/next methods that move around the current iterator
    to the next message in the block. Messages themselves are stored by each author
    and while iterating over the block, it maintains iterator positions for each author
    when attempting to determine the 'next' message
    """
    
    def __init__(self, users):
        """
        Constructor initializes the data structures including the dictionary. It creates
        empty lists for each author based on the users parameter (which is a list of users).

        _messages: a dictionary from users to lists of messages
        _users: a list of users
        _seqnum: the sequence number for the block, also called blocknumber
        _iteruser: an element of _users, this indicates to which user the iterator belongs
        _iterpos: a dictionary of users to their iterator positions in the message list
        _itertime: the time of the message on which is currently being iterated.
        
        """
        self._messages = dict()
        self._users = users
        self._seqnum = 0
        self._iteruser = None
        self._iterpos = dict()
        self._itertime = 0
        self.init_dict()
        
        
        
    def init_dict(self):
        """
        This method initializes the two member dictionaries for this object. It uses the
        _users variable and inserts a position in both dictionaries for each user
        """
        for user in self._users:
            self._messages[user]=list()
            self._iterpos[user] = -1
        
    def clear_messages(self):
        """
        Empties the dictionarys of messages and resets the iterators
        """
        self._messages.clear()
        self.init_dict()
        
    def get_number(self):
        """
        Returns the block number
        """
        return self._seqnum
    def get_recent_messages(self, author, number):
        """
        A helper method used in patching. This returns the last 'number' of messages in the
        block written by the author parameter.
        """
        start = len(self._messages[author]) - number
        if start < 0:
            start = 0
        return self._messages[author][start:]
        
    def get_messages(self, author, position):
        """
        This method returns all messages written by the parameter author after the
        parameter position.
        """
        return self._messages[author][position:]
        
    def count_messages_by(self, author):
        """
        returns the number of message written by the author parameter.
        """
        return len(self._messages[author])
        
    def file_trace(self, file):
        """
        Traces the contents of the block to the file parameter. Assumes the file is open.
        """
        msg = self.cur()
        self.first()
        while (self.cur() != None):
            self.cur().file_trace(file)
            self.next()
        self.first()
        if msg != None:
            while (self.cur() < msg):
                self.next()
   
    def trace(self, silent = False):
        """
        Traces the contents of the block to the stdout.
        """
        log = list()
        msg = self.cur()
        self.first()
        while (self.cur() != None):
            if silent == False:
                self.cur().trace()
            else:
                log.append(self.cur())
            self.next()
        self.first()
        if msg != None:
            while (self.cur() < msg):
                self.next()
                
        if silent:
            return log
        else:
            return

    def insert_message(self, msg):
        """
        Inserts a single message with proper ordering
        """
        print "before"
        self.trace()
        if len(self._messages[msg._author]) == 0:
            self._messages[msg._author].append(msg)
        else:
            i = 0 
            m = None #self._messages[msg._author][i]
            while (m<msg and i < len(self._messages[msg._author])):
                m = self._messages[msg._author][i]
                i += 1
            self._messages[msg._author].insert(i,msg)
            print i
            
        print "after"
        self.trace()
        print "ddd"
    def add_message(self, msg):
        """
        Adds a new message to the block. The message's author field is used to find
        out into which list the message is to be inserted.
        """
        self._messages[msg._author].append(msg)
        
    def insert_messages(self, author, messages):
        """
        This method is used to insert messages into the block. Given an author and a list
        of message, it places the new messages at the end of the list. It also considers that
        it may have some of the messages already. If there are some messages, it looks at the
        last and determines if the first message in messages is less-than-or-equal to the
        last message in its list. If so, it prints asychoronous patching (useful in debug),
        and finds the appropriate position in the list of message from which to begin inserting.
        """
        if len(messages)==0:
            return
        elif len(self._messages[author]) ==0:
            self._messages[author].extend(messages[:])
            return
        else:
            last_msg = self._messages[author][len(self._messages[author])-1]
            i = 0
            while (messages[i]._time <= last_msg._time):
                print "Asyncronous patching!"
                i+=1
                if i == len(messages):
                    return
            self._messages[author].extend(messages[i:])
        
    def _get_iter_time(self, user):
        """
        Returns the time of the current iterator
        """
        if self._iterpos[user] == len(self._messages[user]):
            return sys.maxint
        if (self._iterpos[user] == -1):
            return 0
        return self._messages[user][self._iterpos[user]]._time
    

    def cur(self):
        """
        Returns the message on which iteration is current.
        """
        if self._itertime == sys.maxint:
            return None
        if self._itertime == 0:
            return None
        #print self._iteruser._name, self._iterpos[self._iteruser]
        return self._messages[self._iteruser][self._iterpos[self._iteruser]]
    
    
    def prev(self):
        """
        Moves the iterator to the previous message (across all authors).
        """
        for user in self._iterpos:
            while self._get_iter_time(user)> self._itertime:
                self._iterpos[user]-=1 
            if user >= self._iteruser and self._get_iter_time(user) == self._itertime:
                self._iterpos[user]-=1
            
        olduser = self._iteruser
        self._iteruser = None
        for user in self._iterpos:
            theirtime = self._get_iter_time (user) 
            if (theirtime == self._itertime and olduser > user and self._iteruser == None):
                self._iteruser = user
            elif (theirtime == self._itertime and olduser > user and user > self._iteruser):
                self._iteruser = user
                
            
        if self._iteruser == None:
            oldtime = self._itertime
            self._itertime = 0
            for user in self._iterpos:
                theirtime = self._get_iter_time(user)
                
                if (theirtime > self._itertime and oldtime > theirtime) or (theirtime == self._itertime and oldtime > theirtime and user > self._iteruser):
                    self._itertime = theirtime
                    self._iteruser = user
        
    
    def next(self):
        """
        Moves the iterator to the next chronilogical message across all authors.
        """
        for user in self._iterpos:
            while self._get_iter_time(user)< self._itertime:
                self._iterpos[user]+=1 
            if user <= self._iteruser and self._get_iter_time(user) == self._itertime:
                self._iterpos[user]+=1
            
        olduser = self._iteruser
        self._iteruser = None
        for user in self._iterpos:
            theirtime = self._get_iter_time (user) 
            if (theirtime == self._itertime and olduser < user and self._iteruser == None):
                self._iteruser = user
            elif (theirtime == self._itertime and olduser <user and user < self._iteruser):
                self._iteruser = user
                
            
        if self._iteruser == None:
            oldtime = self._itertime
            self._itertime = sys.maxint
            for user in self._iterpos:
                theirtime = self._get_iter_time(user)
                
                if (theirtime < self._itertime and oldtime < theirtime) or (theirtime == self._itertime and oldtime < theirtime and user < self._iteruser):
                    self._itertime = theirtime
                    self._iteruser = user
        
    def last(self):
        """
        Moves the iterator to the last message.
        """
        for user in self._messages:
            self._iterpos[user] = len(self._messages[user]) - 1
            
        self._itertime = 0
        self._iteruser = None
        for user in self._iterpos:
            theirtime = self._get_iter_time(user)
            if self._itertime < theirtime or (self._itertime==theirtime and self._iteruser < user):
                self._itertime = theirtime
                self._iteruser = user
        
    
    
    def first(self):
        """
        Moves the iterator to the first message.
        """
        for user in self._messages:
            if len(self._messages[user])==0:
                self._iterpos[user] = -1
            else: 
                self._iterpos[user] = 0
        self._iteruser = None
        
        
        for user in self._messages:
            if len(self._messages[user]) > 0:
                curtime = self._messages[user][0]._time
                if self._iteruser == None:
                    self._iteruser = user
                    self._itertime = curtime
                else:
                    if curtime < self._itertime or (curtime == self._itertime and user < self._iteruser):
                        self._iteruser = user
                        self._itertime = curtime
