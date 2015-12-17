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

class Version:
    """
    This class maintains the version for a block. It consists of the
    total number of sealed blocks, and the number of messages from each author
    in the current open block.
    It exposes methods to compare two blocks, as used in the patching algorithm.
    """

    def __init__ (self, block):
        """
        Version constructor takes a block as the parameter. This block is the block for
        which this version is meant to represent. As such, it first inits the two variables:
        _block_num: the number of sealed blocks
        _msg_count: a dictionary from users to the number of messages in the current block

        It then calls update which makes this version object reflect the state of the
        parameter block
        """
        self._block_num = 0
        self._msg_count = dict()
        self.update (block)
 
    def get_messages_by(self, author):
        """
        Returns the number of messages written by the author parameter as indicated by this
        version.
        """
        return self._msg_count[author]

    def differs(self, other):
        """
        Returns true iff the two versions: self and other, differ in some position.
        """
        if self._block_num != other._block_num:
            return True
        else:
            for u in self._msg_count:
                if self._msg_count[u] != other._msg_count[u]:
                    return True
        return False

        
    def diff (self, other):
        """
        Returns the difference between the two versions: self and other.
        The difference is a tuple (difference in block_num, dictionary of the differences
        between the number of messages indexed by user)
        """
        dv = dict ()
        for u in self._msg_count:
            dv[u] = self._msg_count[u] - other._msg_count[u]
        return (self._block_num - other._block_num, dv)

    def update (self, block):
        """
        This method takes a block and sets the version to reflect the contents of the block.
        It sets the _block_num to the blocks actual number, and the number of messages to
        the length of each author field in the block.
        """
        self._block_num = block._seqnum
        for u in block._messages:
            self._msg_count[u] = block.count_messages_by(u)

  
        
    def file_trace (self,file):
        """
        Traces the version information to the file parameter
        """
        file.write("Version: (" + str( self._block_num) + ",")
        for u in self._msg_count:
            file.write( u._name + ":" +  str(self._msg_count[u]) +  ", ")
        file.write(")\n")
        
    def trace (self):
        """
        Traces the version information to stdout.
        """
        print "---Begin Version Trace---"
        print "Block number:", self._block_num
        for u in self._msg_count:
            print u, u._name,  "has", self._msg_count[u], "messages."
        print "-----End Version Trace---"

	
