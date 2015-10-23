###
# KleeQ: a secure, multicast communication protocol and peer-to-peer client.
# Copyright (C) 2007  Alan Kligman and Joel Reardon
###
# Some of the code below comes from Khashmir and is released under
# the MIT license:
#
# Copyright (C) 2002-2003 Andrew Loewenstern
#
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation files
# (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge,
# publish, distribute, sublicense, and/or sell copies of the Software,
# and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# The Software is provided "AS IS", without warranty of any kind,
# express or implied, including but not limited to the warranties of
# merchantability,  fitness for a particular purpose and
# noninfringement. In no event shall the  authors or copyright holders
# be liable for any claim, damages or other liability, whether in an
# action of contract, tort or otherwise, arising from, out of or in
# connection with the Software or the use or other dealings in the
# Software.
###

from binascii import hexlify

def intify (hash_str):
	"""Convert a SHA 20-byte hash in big-endian to a python long integer."""
	assert len (hash_str) == 20
	return long (hash_str.encode ('hex'), 16)

def stringify (hash_int):
	"""Convert a python long integer to a 20-character string."""
	hash_str = hex (hash_int)[2:]
	if hash_str[-1] == 'L':		# Remove the trailing L from the converted integer. 
		hash_str = hash_str[:-1]
	if len (hash_str) % 2 != 0:	# Pad the string if it has an odd number of characters.
		hash_str = '0' + hash_str
	hash_str = hash_str.decode ('hex')
	return (20 - len (hash_str)) *'\x00' + hash_str # Pad the resulting string with 0's if necessary.

def hexify (hash_str):
	"""Convert a SHA 20-byte hash in big-endian to an alpha-numeric string representation."""
	return hexlify (hash_str);
