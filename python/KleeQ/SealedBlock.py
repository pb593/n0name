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
from Message import Message
from Util import Hash

import pickle

class SealedBlock(Block):
    """
    The SealedBlock class inherits from the Block class. It exposes functionality
    to manage validations that it has computed from other clique members, and also
    can compute the hash of its own contents.
    """
    
    def __init__(self, users):
        """
        A sealed block is created and the parent constructor is called. Additionally:

        _validations: a dictionary mapping users to the validations tuple (hash, bool has_it)
                      we need a tuple since it is possible that any value is a received
                      hash, and so a special 0==>no hash cannot be assumed.
        _complete: a boolean indicating whether or not this block has received validations
                   from all other users
        _hash_value: the cryptographic hash value computed from the message contents
        
        """
        Block.__init__(self, users)
        self._validations = dict()
        self._complete = False
        self._hash_value = None
        self.empty_validations()
    
    def fill_validations(self, validation):
        """
        This method takes a validation object, which holds hash values for some subset of
        clique members. For each user, if their hash exists in validation, then it is added
        into the dictionary of hashs stored in this class.
        """
        for user in self._users:
            if self.has_validation(user):
                validation.add_validation(user, self.get_hash_by_user(user))

    def get_hash_by_user(self, user):
        """
        Returns the hash of the user parameter for this block
        """
        return self._validations[user][0]
    
    def get_hash(self):
        """
        returns the hash for this block's contents (as this user would determine). It
        it is not stored locally in _hash_value it is computed (singleton.)
        """
        if self._hash_value == None:
            self.compute_hash()
        return self._hash_value
    
    def has_validation(self, user):
        """
        Returns true iff the user parameter has submitted a hash value for this block
        """
        if self._validations.has_key(user) == False:
            return False
        return self._validations[user][0] != None
        
    def empty_validations(self):
        """
        sets the hash tuple for each user to (None, None)
        """
        for user in self._users:
            self._validations[user]=(None, None)
    
    def as_string(self):
        """
        Gets a string representation of the block's contents, consisting of all the messages.
        This is used to compute hashes.
        """
        s = ''
        self.first()
        msg = self.cur()
        while msg != None:
            s = s + msg.string()
            self.next()
            msg = self.cur()
        return s
    
    def compute_hash(self):
        """
        Computes the cryptographic hash of this object's string reprenetation
        """
        
        self._hash_value = Hash.hash(self.as_string()) 
        

    def submit_validation(self, user, hash_value):
        """
        Submits a hash value (parameter) for the user (parameter) into this block.
        It then checks of the block is complete.
        """
        if self._hash_value == hash_value:
            self._validations[user]=(hash_value, True)
        elif hash_value == None: #de facto verification
            self._validations[user]=(self._validations[user][0], True)
        else:
            self._validations[user]=(hash_value, False)
            print user._name, self.get_number(),  self._validations[user]
            return False
        self.check_complete()
        return True
        
    def is_complete(self):
        """
        Returns true iff all users have submitted a hash value
        """
        return self._complete
        
    def check_complete(self):
        """
        Looks at all the users and checks if they have submitted a hash. If one has not
        then it returns false. If all have submitted it returns true.
        """
        self._complete = True
        for check in self._validations:
            if self._validations[check][1]==False or self._validations[check][1]==None :
                self._complete = False
                return False
        
        return True
        
    def get_number_validations(self):
        """
        Returns the number of users who have submitted hash validations.
        """
        i=0
        for check in self._validations:
            if self._validations[check][1]==True:
                i+=1
        return i
