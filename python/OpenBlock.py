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
from SealedBlock import SealedBlock
from Message import Message


class OpenBlock(Block):
    """
    This class models an open block that can have new message inserted, patches applyed, and
    blocks found. It inherents from Block
    """

    def __init__(self, user):
        """
        Constructor only calls parent constructor.
        """
        Block.__init__(self,user)
        

    def apply_patch(self, patch):
        """
        Given a patch, it inserts all the patch messages into this open block.
        Note that insert_messages considers message position, asychononous patching, etc.
        """
        for author in patch._messages:
            self.insert_messages(author, patch._messages[author])
            

        
    def seal_block(self):
        """
        Gets the first block in the open block and seals it. It also increments the block
        number for this open block. The find block method stops at the position of the end
        of the first block, so this just pulls out the cur() and restarts iterator, removing
        each next() until the end of block pos is reached.
        """
        block = SealedBlock(self._messages.keys())
        block._seqnum = self._seqnum
        self._seqnum +=1
        msg = self.cur()
        self.first()
        while (self.cur() < msg):
            block.add_message(self.cur())
            self._messages[self._iteruser].remove(self.cur())
            self.next()
        block.add_message(self.cur())
        self._messages[self._iteruser].remove(self.cur())
        self.next()
        return block
            
    
    def find_block(self):
        """
        Find block attempts to find a block in the sequence of blocks. It implements the
        block finding algorithm. It sets the cur() to the end position of the block, and
        returns true iff there is a block. cur() is set to None if there is no block.
        """
        t = 0
        p = dict()
        for user in self._messages:
            p[user]= None
        self.last()
        while self.cur() != None:
            i = self.cur()._author
            if (p[i] == None):
                p[i]=self.cur()
                t += 1
                if t == len(self._messages):
                    self.prev()
                    break; #sealable set begins before iterator
            self.prev()
        if t != len(self._messages): 
            return False
        t = 0
        for user in self._messages:
            p[user]= None
        seal_end = self.cur()
        self.first()
        
        while self.cur() != seal_end:
             #print self.cur()._author._name, self.cur()._time, self.cur()._text
             i = self.cur()._author
             if (p[i] == None):
                p[i]=self.cur()
                t += 1
                if t == len(self._messages):
                    return True
             else:
                p[i] = self.cur()
             self.next()
        return False         
                    
                    
        
