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

from Crypto.Cipher import AES

def encrypt (key, text):
	crypt = AES.new (key[0:16], AES.MODE_CBC)
	if len (text) % 16 != 0:
		msg = text + '\x80' + ((16 - len (text) % 16) - 1) *'\x00'
	else:
		msg = text
	assert len (msg) % 16 == 0
	ciph = crypt.encrypt (msg)
	return ciph

def decrypt (key, ciph):
	crypt = AES.new (key[0:16], AES.MODE_CBC)
	msg = crypt.decrypt (ciph)
	return msg[:msg.find ('\x80')]
