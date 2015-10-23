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

from Util import Hash
from Util import MAC
from Util.Cipher import *

from Packet import Packet
import pickle
from threading import Semaphore
class KeyManager:
    """
    This class is responsible for managing keys for all cliques in which
    the user is a member. This simplefies routing of packets because the
    address header can be used to uniquely determine if the user is
    capable of decryption, and which clique should be used for this
    task.

    It exposes methods to add and remove keys as they are created. When
    the clique user wishes to shut down, this class must be marshaled
    securely in the event of the compromise of a device.

    This class is used by both the UI and the Transport layer, and so
    it exposes thread safety on the following public methods:
    * get_key
    * initial_key
    * register_key
    * deregister_key
    * encrypt
    * decrypt
    * get_clique
    """


    def __init__(self):
        """
        This initiaizes the member variables:
        _key_lock: the lock for thread safety
        _keys: a dictionary mapping address tags to the tuple (cliquename, key)
        _namenumber_address: a dictionary mapping the tuple (cliquename, blocknum) to address tag
        """
        self._key_lock = Semaphore(0)
        self._keys = dict() #_keys[add ress tag] = the key
        self._namenumber_address = dict()
        self._key_lock.release()
    
    def get_key(self, clique_name, block_num):
        """
        Returns the key that corresponds with the parameters for the clique name and block number
        """
        print "getkey:acq"
        self._key_lock.acquire()
        address = self._namenumber_address[(clique_name, block_num)]
        (dummy, key) = self._keys[address]
        print "getkey:rel"
        self._key_lock.release()
        return key

    def get_clique(self, address_tag):
        """
        Returns the name of the clique that corresponds with the address tag
        """
        print "getclique:acq"
        self._key_lock.acquire()
        if (self._keys.has_key(address_tag) == False):
            self._key_lock.release()
            return None
        retval = self._keys[address_tag][0] #gets the clique name
        print "getclique:rel"
        self._key_lock.release()
        return retval
    
    def __get_address_tag(self, clique_name, key):
        """
        private method to compute the address tag given a key and clique name
        """
        return MAC.mac(key, clique_name)
    
    def initial_key(self, clique_name, key):
        """
        Inserts the initial key for a new clique that is derived from the inital secret.
        This is isomorphic to register_key with blocknumber 0 but added for clairity.
        """
        self._key_lock.acquire()
        address = self.__get_address_tag(clique_name, key)
        self._keys[address] = (clique_name, key)
        self._namenumber_address[(clique_name, 0)] = address
        self._key_lock.release()
        
    def register_key(self, clique_name, key, block_num):
        """
        Registers a newly computed key into the dictionaries based on the parameters:
        the block number, clique name and the key itself.
        """
        print "regkey:acq"
        self._key_lock.acquire()
        address = self.__get_address_tag(clique_name, key)
        self._keys[address] = (clique_name, key)
        self._namenumber_address[(clique_name, block_num)] = address
        print "regkey:rel"
        self._key_lock.release()

    def deregister_key(self, clique_name, block_num):
        """
        Removes a key from memory when it is no longer needed. This is called when the clique
        clique_name erases the block numbered block_num from its memory.
        """
        print "dereg:acq"
        self._key_lock.acquire()
        i =  len(self._keys)
        
        address = self._namenumber_address[(clique_name, block_num)]
        
        
        self._keys.pop(address)
        self._namenumber_address.pop((clique_name, block_num))
        
        if i-1 != len(self._keys):
            print "dereg not happeend"
        print "dereg:rel"
        self._key_lock.release()
        
    def encrypt(self, clique_name, block_num, message):
        """
        This method is given a message along with the clique name and block number that
        indicate which key to use for encryption. It prepares a packet object,
        encryptes the message, determines the address tag, and provides the MAC to the packet.
        The final packet is then returned. This method assumes the key exists, but releases
        the lock if an exception is thrown
        """
        
        print "encrypt:acq"
        self._key_lock.acquire()
        
        packet = Packet()
        try:
            packet._address = self._namenumber_address[(clique_name, block_num)]
            (dummy, key) = self._keys[packet._address]
            packet._payload = encrypt(key, message)
        
            packet._mac = MAC.mac(key, packet.get_authenticated_component())
            print "encrypt:rel"
            self._key_lock.release()
            return packet
        except KeyError:
            print "The key (", clique_name,",",  block_num , ")cannot be found"
            print "encrypt:rel"
            self._key_lock.release()
            return None

        
    def decrypt(self, packet):
        """
        This method decrypts a packet and redumps the resulting message.
        It first looks up the address tag to determine if it has the correspnding key
        It then gets the key if it has it, uses it to first check the MAC, and if
        it was not altered then it decrypts the corresponding message. The MAC is important
        since we use python and messages are pickled objects, so hostile classes could be
        pickled and sent but a valid MAC cannot be computed.
        """

        #todo throw exceptions
        self._key_lock.acquire()
        
        if not self._keys.has_key(packet._address):
            print "key not available - ignoring"
            self._key_lock.release()
            return None
        (clique_name, key) = self._keys[packet._address]
        if (MAC.mac(key, packet.get_authenticated_component()) != packet._mac):
            print "error in xmission", MAC.mac(key, clique_name+message), clique_name, packet._mac
            print message
            self._key_lock.release()
            return None


        message = decrypt(key, packet._payload)
        if message[len(message)-1] != '.':
            message = message + '.'
        self._key_lock.release()
        return (clique_name, message) 
