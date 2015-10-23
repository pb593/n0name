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
from SealedBlock import SealedBlock
from Version import Version
from PatchBlock import PatchBlock
from Validation import Validation
import sys

class BlockManager:
    """
    This class is responsible for managing the blocks of conversation and reducing the
    responsibility on the clique class. It is only to be used with clique and as such there
    is no need for thread safety.
    This class is an iterator class, which means it maintains a 'cur' block that can be moved
    to the first/last/next/prev block. If the cur moves past the last sealed block then it
    moves to the open_block, followed by 'None'
    """
    def __init__ (self, user, users):
        """
        Initializes the member variables:

        _user: a reference to the clique member this blockmanager is used by
        _users: a list of all the users
        _block_iter: an integer that keeps track of which block to return with cur()
        _sealed_blocks: a list of the blocks that have been sealed after being found
        _open_block: the current opened block to which writting is allowed
        _version: the version of the text
        _validated: a dictionary that maps clique users to the smallest unverified block number
        _removed_blocks: a list of blocks that have been recently deleted. used by clique
                         to removed unneeded keys
        _tau: a parameter for organic subcliques - to be added in a conf.

        file/errfile/bfile are used to dump output information
        """

        self._user = user
        self._users = users
        self._block_iter = -1
        self._sealed_blocks = list ()
        self._open_block = OpenBlock (users)
        self._version = Version (self._open_block)
        self._validated = dict()
        self._removed_blocks = list()
        
        self._tau = 5
        
        for u in self._users:
            self._validated[u]=-1 #position of the last validated block
        
        
        self.file = open(self._user._name + ".blockmgr", "w")
        self.errfile = open(self._user._name + ".blockinfo","w")
        self.bfile = open (self._user._name + ".blockataglance","w")

    def write_message(self, message):
        """
        Throughput method to add the parameter message to the open block
        """
        self._open_block.add_message(message)
        
        
    def get_block_count(self):
        """
        returns the number of the current open block. This number is not the number
        of blocks currently being managed, but rather the total number of blocks
        that have been created
        """
        return self._open_block.get_number()
        
    def min_validation(self):
        """
        returns the user for which the smallest numbered block is waiting validation
        """
        min = sys.maxint
        min_user = None
        for u in self._validated:
            if self._validated[u] < min:
                min = self._validated[u] 
                min_user = u
        return min_user
        
    def get_validation(self, user = None):
        """
        returns the a validation object that can be sent to another clique member
        to validate sealed blocks. If a user is directly passed then it returns
        a properly formed validation object for the smallest numbered unverified block
        for that user.
        If the user is not directed passed then it returns a properly formed validation
        object for the smallest numbered unverified block (across all users).
        If there is no such unverified block (or we have not yet computed it) then this
        returns None.
        """
        if user == None:
            user = self.min_validation()
        block_num = self._validated[user]
        block_num +=1
        if self._open_block._seqnum <= block_num:
            return None # we have not computed this block to verify
        else:
            block = self.get_block_num(block_num)
            valid = Validation(block_num)
            block.fill_validations(valid)
            return valid
        
    def validate(self, validation):
        """
        This method performs the validation of blocks. It takes a validation object and
        gets the corresponding block number. It retreives the block by the get_block_num
        method. If this method returns None (we have deleted or not computed this block)
        or the block returned is the open_block, then we return None to indicate this
        validation attempt failed.

        Otherwise, for each user in the clique, we submit the corresponding validation.
        Either the validation will be lacking for that user, in which case we continue,
        or we will have already validated for that user, in which case we continue,
        or we can actually submit the validation. On a success we update the lowest validated
        block. We also return a reply validation if our own validation was not included
        in the set (because this implies the first phase of bidirectional validation).
        """

        if validation == None:
            return None
        
        block_num = validation._block_num
        block = self.get_block_num(block_num)
        if block == None or block == self._open_block:
            return None
        for u in self._users:
            if validation.has_validation(u) == False:
                continue
            if self._validated[u] >= validation._block_num:
                continue
            if block.submit_validation(u, validation.get_validation(u)):
                self.block_glance()
                self._validated[u] = block_num
            
        block.fill_validations(validation)
        self.block_glance()
        return validation
        
    def de_facto_validate(self, user, end_num):
        """
        De_facto_validation is a form of validation that occurs becuase two clique
        members have successfully patched over a future key implying they both computed
        the same key. Hence all prior blocks must be identical (assuming collsion resistence)
        This method fills validations for the user parameter for all blocks strictly less than
        the parameter end_num.
        """
        self.first()
        while (self.cur()._seqnum < end_num and self.cur() != self._open_block):
            self.cur().submit_validation(user, self.cur().get_hash())
            self.next()
        if self._validated[user] < end_num:
            self._validated[user] = end_num
        self.block_glance()
        

    def block_glance(self):
        """
        An output method that shows how many blocks are currently being managed
        along with the first blocks blocknum and the number of validation
        that have been received for the block (this only works for <10 clique members)
        """

        if len(self._sealed_blocks) == 0:
            return
        block  = self._sealed_blocks[0]
        self.bfile.write(str(block._seqnum) + "\t")
        i = block._seqnum
        
        for block in self._sealed_blocks:
            while i < block._seqnum:
                self.bfile.write( '-')
                i +=1
            self.bfile.write(str(block.get_number_validations()))
            i +=1
        self.bfile.write('\n')
        
    def cur(self):
        """
        Returns the current block being managed by block iter. Returns None if out of bounds

        """
        if self._block_iter == len(self._sealed_blocks):
            return self._open_block
        elif self._block_iter > len(self._sealed_blocks):
            return None
        elif self._block_iter < 0:
            return None
        else:
            return self._sealed_blocks[self._block_iter]
            
    def first(self):
        """
        Sets the iterator to the first block.
        """
        self._block_iter = 0 #could be open block so no bound check
        
    def last(self):
        """
        Sets the iterator to the last block.
        """
        self._block_iter = len(self._sealed_blocks) #return open block
        
    def next(self):
        """
        Sets the block iterator to the next block. Returns true iff still in bounds.
        """
        self._block_iter += 1
        if self._block_iter > len(self._sealed_blocks):
            return False
        return True
    
    def prev(self):
        """
        Sets the block iterator to the previous block. Returns true iff still in bounds.
        """
        self._block_iter -= 1
        if self._block_iter < 0:
            return False
        return True
    
    def update_version(self):
        """
        Updates the _version member variable to the new value
        """
        #todo : decide if check for new blocks should be here
        self._version.update(self._open_block)

    def remove_block(self, block):
        """
        This method removes the block (passed as parameter) from the list of sealed blocks.
        """
        
    #remove from dict if random access is also needed
        self._sealed_blocks.remove(block)
        self._removed_blocks.append(block._seqnum)
        self.errfile.write("Removing block: " + str(block._seqnum)+'\n')
        
    def add_block(self, block):
        """
        This method adds a new block (passed as parameter) after being found by found block
        """
        #add to dict if random access is also neeeded
        self._sealed_blocks.append(block) 
        self.errfile.write("Adding block: " + str(block._seqnum) +'\n')
        self.block_glance()
        
    def remove_blocks(self):
        """
        This method goes through all the sealed blocks and determines which can
        be removed. Removal criteria is that all clique members have validated
        this block (including defacto validations).

        """
        for block in self._sealed_blocks[:]:
            if block.is_complete():
                self.remove_block(block)
        self.block_glance()
                
    def seal_blocks(self):
        """
        This method goes through the open block and if it can find a sealable block,
        then it seals it. It ends by updating the version.
        """
        while (self._open_block.find_block()):
            block = self._open_block.seal_block()
            block.file_trace(self.file)
            self.errfile.write("Sealing block: " +str(block._seqnum)+'\n')
            self.file.write("----\n")
            block.submit_validation(self._user, block.get_hash())
            self.add_block(block)
        self.update_version()
        self._version.file_trace(self.errfile)
        
    def patch_request(self):
        """
        This method returns a properly formed patchblock that consists solely of the version
        number and user name, intended to be filled by the recipiant. It is the first phase
        of the patching protocol.
        """
        patchblock = PatchBlock(self._users)
        patchblock._sender_version = self._version
        patchblock._sender = self._user
        patchblock._receiver_version = None
        patchblock._receiver = None
        return patchblock

    def patch (self, patch_block):
        """
        This method implements the second and third phases of the patching protocol. It is
        given a patch_block, and this block is merged into the open block, and blocks are
        attempted to be sealed. After performing the defacto validation, the new version
        and the old version are compared. If they differ we must be in the 2nd phase of
        patching and a reply is generated. Otherwise, None is returned to signify the completion
        of the patching protocol.
        """
        self._version.file_trace(self.errfile)
        if patch_block._receiver_version == None:
            return self.generate_patch(patch_block)
        else:
            
            self._open_block.apply_patch(patch_block)
            self.seal_blocks()
            self.de_facto_validate(patch_block._sender, patch_block._sender_version._block_num)
            if patch_block._sender_version.differs(self._version):
                #reply phase of patching
                return self.generate_patch(patch_block)
            return None
            
    def generate_patch(self, patch_block):
        """
        This method is the workhorse of the patching protocol. Given a patch request,
        it computes the differences in version and generates the appropriate patch.
        There are three main cases:
        1) The block numbers for both version are the same.
           Simply return the missing messages from the open block.
        2) The block number for the other version is higher:
            In theory we could have newer blocks to send, but in reality
            this means that the patch request was sent with a newer key
            so this case will never actually execute.
        3) The block number is lower for the patchee. Search through
            all the old sealed blocks to find the corresponding position from
            which messages are missing and send those
        """


        #prepares the patch block for the return message
        patch_block.clear_messages()
        patch_block._receiver_version = self._version
        patch_block._receiver = self._user
        patch_block.swap_direction()

        #Computes the difference between the versions
        delta = patch_block._sender_version.diff(patch_block._receiver_version)
        delta_block = delta[0]
        delta_messages = delta[1]

        #Case 1: Simply return the 'n' most recent messages if the difference in version is +n.
        if delta_block == 0: 
            for author in self._users:
                if delta_messages[author] > 0: #we have newer messages
                    patch_block.insert_messages(author, self._open_block.get_recent_messages(author, delta_messages[author]))
        #Case 2: write to the error file, this should not happen
        elif delta_block < 0:
            patch_block._receiver_version.trace()
            patch_block._sender_version.trace()
            self.errfile.write( "patch request with higher version - this should not happen")
        #Case 3: Find the messages to insert for each author.
        else: #delta_block > 0
            for author in self._users:
                #first, get the block that the older versioned user is considering
                cur_block_num = patch_block._receiver_version._block_num
                cur_block = self.get_block_num(cur_block_num)

                #then move across each block in sequence, subtracting the number of
                #messages in each block until the correct position is found
                #This is because a user may have not sealed off many blocks but still
                #have messages that will inevitably appear in later blocks.
                pos = patch_block._receiver_version._msg_count[author]
                while (cur_block != None and pos >= cur_block.count_messages_by(author) ):
                    pos -= cur_block.count_messages_by(author)
                    cur_block_num +=1
                    cur_block = self.get_block_num(cur_block_num)
                #once the position is found, keep moving along the blocks until all the
                #messages from the same author are added into the patch block
                while (cur_block != None):
                    patch_block.insert_messages(author, cur_block.get_messages(author, pos))
                    pos = 0
                    cur_block_num+=1
                    cur_block = self.get_block_num(cur_block_num)
        return patch_block


            
    def get_block_num(self, num):
        """
        This method returns the block whose blocknum is that of this method. It looks in
        the sealed block list and the open block.
        """
        if (self._open_block.get_number() == num):
            return self._open_block
        for block in self._sealed_blocks: #todo blocks as array indexed by seqnum
            if (block.get_number() == num):
                return block
        return None

        
    
    def organic_subclique_finder(self):
        """
        This method implements the organic_subclique_finder algorithm, that returns
        whether a set of blocks greater than tau can be sealed if we consider a reduce
        clique size. It also returns such a reduced clique.
        """
        authors = set()
        n = 0
        c=0
        for author in self._users:
            p[author] = None
        self.last()
        while (self.cur() != None):
            a = self.cur()._author
            if not a in authors:
                authors.add(a)
                if c > self._tau:
                    return (True, c, authors)
                else:
                    c =0
                    authors.add(a)
                if p[a] == None:
                    n += 1
                    if n == len(self._users):
                        n = 0
                        for author in self._users:
                            p[author] = None 
                        c+=1
                else:
                    p[a]= self.cur()
            if len (authors) == len(self._users):
                return None
            self.prev()
