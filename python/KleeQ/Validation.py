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

class Validation:
    """
    This class stores information about a validation of a block, and is converted into
    a packet for transmission.
    """
    def __init__(self, block_num):
        """
        Constructor takes the block_number for which this is a validation. It maintains
        a dictionary of users to the hash_value.

        _block_num: the block number for which this is a validation
        _type: used to reconstruct after depickling
        _block_validations: a dictionary of users to hash values
        """
        
        self._block_num = block_num
        self._type = "Validation"
        self._block_validations = dict() # keys are users, values are the hash
        
        
    def add_validation(self, user, value):
        """
        Adds the valuation value (parameter) for the user parameter.
        """
        self._block_validations[user]=value
        
    def has_validation(self, user):
        """
        Returns true iff this object has a valuation for the user parameter.
        """
        return self._block_validations.has_key(user)
    
    def get_validation(self, user):
        """
        Returns the valuation for the user parameter.
        """
        if self.has_validation(user) == False:
            return None
        return self._block_validations[user]
