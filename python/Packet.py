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

class Packet:
    def __init__(self):
        self._payload = ''
        self._mac = ''
        self._address = ''
	self._user = ''
     
     
    def get_authenticated_component(self):
        return self._address + self._payload
   
    def get_as_string(self):
        return self._address + self._mac + self._payload
    
    
    def set_as_string(self, data):
        self._address = data[0:16]
        self._mac = data[16:32]
        self._payload = data[32:]
