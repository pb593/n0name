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

class User(object):
    """
    A class that holds the infomration about a clique user, which is
    currently solely their name. It also overrides a variety of methods
    including hash and cmp, so that users can be pickled and unpickled and
    still be used in dictionaries without problem.
    """
      
    def __lt__(self, other):
        """
        Override to determine if one user is 'less than' another
        """
        if (self == None):
            return False
        if (other == None):
            return True
        if (self._name < other._name):
            return True
        else:
            return False
            
    def __gt__(self, other):
        """
        override to determine if one user is 'greater than' another
        """
        return not self.__lt__(other)
        
    def __init__(self, name):
        """
        Initializes the name of the clique user.
        """
        self._name = name
    
    def __eq__(self,other):
        """
        Determines if two clique users are the same
        """
        if other == None:
            return False
        else:
            return self._name == other._name
    
    
    def __hash__(self):
        """
        Override the hash function used by the dictionary. Instead of hashing the
        memory address its a hash of their name.
        """
        return hash(self._name)
        
        
    def __cmp__(self, other):
        """
        Used to compare two clique members in the dictionary context.
        """
        if self.__eq__(other):
            return 0
        elif self.__lt__(other):
            return -1
        else:
            return 1
    
