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

from OpenBlock import OpenBlock
from KeyManager import KeyManager
from SealedBlock import SealedBlock
from Version import Version
from PatchBlock import PatchBlock
from Timestamper import Timestamper
from BlockManager import BlockManager
from Packet import Packet
from messages.Message import messages.Message
from Util import Hash
from Util import MAC
from Util import Cipher

import pickle
from threading import Semaphore

class Clique:
    """
    The clique class manages the affairs of a single clique in Kleeq. It manages message
    through the block manager, manages keys through the key manages, and keeps track of
    the current secrets.

    It is used by both the transport layer and the UI, and as such is a threadsafe class
    The variable _lock is used to ensure threadsafety.
    """
    def __init__ (self, name, user, users, secret, key_manager):
        """
        The init method constructs the clique class. It takes five parameters, each of
        which are used to set one of the following member variables:
        _name: the name of the clique, from parameter name
        _user: the user object for whom this clique is constructed, from parameter user
        _users: the list of all the users in the clique, from parameter users
        _secret: the secret for this clique, from the parameter secret
        _key_mgr: the key manager, from the parameter key_manager. The key manager is a single
                  class across all cliques, since a user may have messages directed to any
                  number of cliques. As such, new keys are added to a central repository.


        Additionally, there are the following member variables:
        _lock: the lock used for threadsafety
        _listener_lock: another lock used for writting new messages to a list where they can be
                       grabbed by another viewing application (thread safety)
        _listeners: A list that holds each message that is written in the clique. Each new message
                  is added to this list once.
        _last_seen: a dictionary that maps users to the lamport time where they were last
                    patched by the clique member _user.
        _timestamper: the class that manages lamport times
        file: an output file for debug purposes
        _block_mgr: the class that manages the blocks and messages

        This class also translates packets into objects, and routes the objects accordingly.
        It is used to also translate objects into packets for transport.
        """
        self._lock = Semaphore(0)
        self._listener_lock = Semaphore(0)
        self._listeners = list()
        self._name = name
        self._user = user
        self._users = users
        self._secret = secret
        self._key_mgr = key_manager
        self._key_mgr.initial_key(name,  MAC.mac(secret, name))
        self._last_seen = dict()
        self._timestamper = Timestamper ()
        self.file = open(self._user._name + ".clique", "w")
        self._block_mgr = BlockManager(self._user, self._users)
        self._lock.release()
        self._listener_lock.release()
    
        
    def __validate(self, validation):
        """
        This private method is given a validation object and sends it to the block manager
        where it can be properly handled. The result is then convered into a packet
        and returned.
        """
        print "__validate"
        retval = self.validate_to_packet(self._block_mgr.validate(validation))
        return retval
    
#    def validate_request(self):
#        """
#        The beginning of the validation proeceedure. This method retreives a validation
#        from the block manager and converts it into a packet to be sent to the appropriate
#        user
#        """
#        self._lock.acquire()
#        retval = self.validate_to_packet(self._block_mgr.get_validation())
#        self._lock.release()
#        return retval

        
    def validate_request(self, user):
        """
        The beginning of the validation proeceedure. This method retreives a validation
        from the block manager for the user parameter and converts it into a packet
        to be sent to that user.
        user
        """
        print "valreq:acq"
        self._lock.acquire()
        retval = self.validate_to_packet(self._block_mgr.get_validation(user))
        print "valreq:rel"
        self._lock.release()
        return retval
        
    def add_sequencing(self):
        """
        This method is used to add a sequencing message that indicates the user
        has been involving in a patching.
        """
        message = messages.Message(self._user, " give a patching.", self._timestamper.stamp())
        self._block_mgr._open_block.add_message(message)
        self._block_mgr.update_version() #todo optimize


    def register_listener(self, listener):
        print "register:acq"
        self._listener_lock.acquire()
        self._listeners.append(listener)
        print "register:rel"
        self._listener_lock.release()

    def deregister_listener(self, listener):
        print "Listener_lock:dereg:acq"
        self._listener_lock.acquire()
        self._listener.remove(listener)
        
        print "Listener_lock:dereg:rel"
        self._listener_lock.release()
   
    def __peek_messages(self, patch_block):
        print "__peek"
        messages = list()
        patch_block.first()

        while patch_block.cur() != None:
            messages.append(patch_block.cur())
            patch_block.next()
        
        print "peeks:acq"
        self._listener_lock.acquire()
        for listener in self._listeners:
            listener.add_messages(messages)
            
        print "peeks:rel"
        self._listener_lock.release()

    def __peek_message(self, message):

        print "peek:acq"
        self._listener_lock.acquire()

        for listener in self._listeners:
            listener.add_message(message)

        print "peek:rel"
        self._listener_lock.release()

    
    
    def patch_request(self):
        """
        This method is the beginning of the patching protocol. It gets a patch request
        generated from the block manager, sets the time of the request, and returns
        the packet to the calling layer.
        """
        print "patch_req:acq"
        self._lock.acquire()
        patchreq = self._block_mgr.patch_request()
        patchreq.set_time(self._timestamper.peek())
        retval =self.patch_to_packet(patchreq)
        self._lock.release() 
        print "patch_req:rel"
        
        return  retval
 
    def __patch (self, patch_block):
        """
        This method implements the second and third phases of the patching protocol.
        It is provided a patch_block and behaves accordingly. First, it inserts
        all the messages from the block into the msg_log for viewer retreival.
        Then it calls patch on the block manager which returns the result (could be
        another phase of patching). Finally, it rotates the keys for the blocks if
        new blocks have been sealed, and also calls remove blocks since some blocks
        may have been verified by the defacto method.
        Finally, it converts the responce block, if given, into a packet and returns
        the packet
        """
        
        if patch_block == None:
            return None
            
        #This is used to determine after calling patch if new
        #blocks were sealed and as such if new keys must be added
        blocks = self._block_mgr.get_block_count()

        #Update the time if needed
        self._timestamper.update(patch_block.get_time())
        

        #update the last patching time for the receiver
        self._last_seen[patch_block._sender] = self._timestamper.peek()


        #Add all the new messages from the patch into the list of messages
        #so that a viewing application will be able to see them.

        self.__peek_messages(patch_block)
      

        #invoke patch on the block managers version
        retpatch = self._block_mgr.patch(patch_block)
        #self.add_sequencing(patch_block._sender)


        self.rotate_keys(blocks) #rotate the keys starting from the old block count
        self._block_mgr.remove_blocks() #attempt to remove old blocks

        if retpatch == None:
            return None
            
        comment = '''
        message = messages.Message(self._user, " give a patching.", self._timestamper.stamp(), "sequence")
        self._block_mgr._open_block.add_message(message)
        self._block_mgr.update_version() #todo optimize
        retpatch.add_message(message)
        retpatch.set_time(self._timestamper.peek())
        retpatch._sender_version = self._block_mgr._version
        retpatch.trace()'''


        packet = self.patch_to_packet(retpatch) #get the resulting package
        
        return packet
        
    def rotate_keys(self, start):
        """
        The rotate_keys method is given a starting position start (paramter) and
        and if there are new blocks beyond start, it will generate the new keys
        to be used with those blocks.

        It also removes old keys corresponding to old blocks, where the list of
        old blocks is given in the block mgr.
        """
        
        #todo  - keep old secrets if bad rotation
        stop = self._block_mgr.get_block_count()
        
        #while there are blocks in the removed blocks list, deregister the corresponding
        #key and remove that block from the list
        while len(self._block_mgr._removed_blocks) >0:
            self._key_mgr.deregister_key( self._name, self._block_mgr._removed_blocks[0])
            self._block_mgr._removed_blocks = self._block_mgr._removed_blocks[1:]

        #while there are new sealed blocks since our last key rotation, generate the new
        #secret and the new key and register that key in the key manager.
        while start < stop:
            self.file.write("Rotate keys: " + str(start) + ' of '+str(stop) +'\nKey\n')
            block = self._block_mgr.get_block_num(start)            
            key = self._key_mgr.get_key(self._name, start)
            self.file.write(key + '\nSecret\n')
            self._secret = MAC.mac(key, self._secret)
            self.file.write('\nNew Key\n')
            newkey = MAC.mac(self._secret,  block.as_string())
            self.file.write(newkey + '\nRegister\n')
            start += 1
            self.file.write(self._name + ' ' + str(start) +'\n')
            self._key_mgr.register_key(self._name,newkey ,start)
 
    def write_message(self, message):
        """
        This method writes a single message by the author into the clique.
        The message is given as a parameter, but its timestamp is given
        by the clique
        """
        print "write:acq"
        self._lock.acquire()
        message._time = self._timestamper.stamp()
    
        self._block_mgr.write_message(message)
        self._block_mgr.update_version() #todo optimize
        
    
        self.__peek_message(message)
        print "write:rel"
        self._lock.release()
        
    def patch_to_packet(self, patch_block):
        """
        This method translates a patch_block into a packet. It considers the
        version number of the sender for encryption, but if it is on the reply
        phase of patching, and the receiver's version number has a lower block_num
        then it uses the older version number so that the receiver will have the
        corresponding key for decryption.

        It returns the resulting packet
        """
        if patch_block == None:
            return None
        
        block_num = patch_block._sender_version._block_num
        if patch_block._receiver_version != None:
            block_num = patch_block._receiver_version._block_num
        
        return self.wrap_packet(patch_block, block_num)
        
        
    def validate_to_packet(self, validation):
        """
        This method converts a validation object into a packet object and returns it
        """
        if validation == None:
            return None
        block_num = validation._block_num
        return self.wrap_packet(validation, block_num)
        
    def unwrap_packet(self, packet):
        """
        This method is given a packet. First, it decrypts it if possible, and returns
        None if the key is missing or the MAC is invalid. Then it calls pickle.loads
        of the resulting message and returns the constructed object.
        """
        result = self._key_mgr.decrypt(packet)
        
        if result == None:
            return None
        (clique_name, message) = result
        if (self._name != clique_name):
            print "name check fail clique.77", self._name , clique_name
            return None
            
        return pickle.loads(message)
        
    def wrap_packet(self, object_data, block_num ):
        """
        This method is given an objetc and a block_num (which corresponds to the key
        that will be used for encryption). It first does a pickle.dump of the object
        and then returns the packet that is constructed by the key_manager's encrypt method.
        """
        data = pickle.dumps(object_data)
        print "halfwrapped"
        return self._key_mgr.encrypt(self._name, block_num, data)
        
        
    def received_packet(self, packet):
        """
        This method is the general handler for receiving a packet. First, it is unwraped
        if possible into an object. Then the type is considered. If it is a patch_block
        then the patch method is called. If it is a validation then the validate method
        is called.
        """
        print "recpack:acq"
        self._lock.acquire()
        data = self.unwrap_packet(packet)
        retval = None
        if (data == None):
            retval = None
        elif (data._type == "Validation"):
            retval = self.__validate(data)
        elif (data._type == "PatchBlock"):
            retval = self.__patch(data)
            if retval != None:
                retval._user = data._receiver._name
        print "recpack:rel"
        self._lock.release()
        return retval
