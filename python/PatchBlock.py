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

class PatchBlock(Block):
    """
    PatchBlock is a class that is used to swap messages used in the patching algorithm.
    It inherents from the parent Block class
    """
    
    def __init__(self, users):
        """
        PatchBlocks need to share version information and user information. As such, the
        extra variables are:
        _sender_version: the version of the sender
        _receiver_version: the version of the receiver
        _sender: the user object for the sender
        _receiver: the user object for the receiver
        _time: the lamport time of the sender
        _type: used in pickle.loads
      
        """
        Block.__init__(self,users)
        self._sender_version = None
        self._receiver_version = None
        self._sender = None
        self._receiver = None
        self._time = 0
        self._type = "PatchBlock"
      
    def set_time( self, time):
        """
        Sets the lamport time
        """
        self._time = time
        
    def get_time(self):
        """
        Gets the lamport time
        """
        return self._time
    
    def min_block_num(self):
        """
        Returns the min block number between the sender and receiver's versions
        """
        if self.get_sender_block_num() < self.get_receiver_block_num():
            return self.get_sender_block_num()
        return self.get_receiver_block_num
        
    def get_sender_block_num(self):
        """
        Returns the block number of the sender
        """
        return self._sender_version._block_num
    def get_receiver_block_num(self):
        """
        Returns the block number of the receiver
        """
        return self._receiver_version._block_num    
    def swap_direction(self):
        """
        Swaps the direction of the patch block. The sender's values become the
        receiver's values, and vice versa
        """
        (self._sender_version, self._receiver_version) = (self._receiver_version, self._sender_version)
        (self._sender, self._receiver) = (self._receiver, self._sender)
        

