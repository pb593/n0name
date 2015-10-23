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

import SocketServer
import threading
import sys
import socket
from ConfigParser import SafeConfigParser
from Clique import Clique
from KeyManager import KeyManager
from Message import Message
from User import User
from Packet import Packet
from Util import Hash

from Tkinter import *


class CliqueServer (SocketServer.ThreadingTCPServer):
    km = KeyManager ()	# Statically allocate a key manager.
    cliques = {}		# Allocate a dictionary for cliques.
    cnf = None

class CliqueRequestHandler (SocketServer.StreamRequestHandler):
    def handle (self):
        request = self.rfile.read ()	# Retrieve the request.
        p = Packet ()
        p.set_as_string (request)
        c = self.server.km.get_clique (p._address)	# Determine which clique to pass this to.
        if c == None:
            return
#        else:
  #          (c, m) = dp
        response = self.server.cliques[c].received_packet (p)	# Pass the message to the appropriate clique.
        if response != None:
            s = socket.socket (socket.AF_INET, socket.SOCK_STREAM)
            addr = (self.server.cnf.get (response._user, 'machine'), int (self.server.cnf.get (response._user, 'port')))
            s.connect (addr)
            s.send (response.get_as_string ())
            s.close ()	

class CliqueServerDaemon (threading.Thread):
    def __init__ (self, srv):
        self._srv = srv
        threading.Thread.__init__ (self)
        
    def run (self):
        self._srv.serve_forever ()
   
    
    """
    
    while True:	# Loop forever and process keyboard interactions.
		msgs = srv.cliques[active].get_log ()
		for m in msgs:
			sys.stdout.write (m._author._name + ": " + m._text + '\n')
		sys.stdout.write ("> ")
		l = sys.stdin.readline ()
		if l[0] == ':':
			cmd = l[1:l.find (' ')]
			arg = l[l.find (' ') + 1:].strip ('\n')
			if cmd == 'q' or cmd == 'quit':
				sys.stdout.write ("kleeq: Bye!\n")
				break
			elif cmd == 's' or cmd == 'send':
				s = socket.socket (socket.AF_INET, socket.SOCK_STREAM)
				if cp.has_section (arg):
					addr = (cp.get (arg, 'machine'), int (cp.get (arg, 'port')))
					p = srv.cliques[active].patch_request ()
					s.connect (addr)
					s.send (p.get_as_string ())
					s.close ()
				else:
					sys.stdout.write ("kleeq: user not found.\n")
			elif cmd == 'l' or cmd == 'log':
				srv.cliques[active]._block_mgr._open_block.trace ()
			elif cmd == 'u' or cmd == 'use':
				if arg in srv.cliques:
					active = arg
					sys.stdout.write ("kleeq: active clique updated.\n")
				else:
					sys.stdout.write ("kleeq: clique not found.\n")
			elif cmd == 'h' or cmd == 'help':
				sys.stdout.write (
"""
#	q,quit: exit
#	s,send <user>: sync with target
#	l,log: show a trace of open block
#	u,use: change cliques
#	h,help: show this message
""")

		else:
			if active == None:
				sys.stdout.write ("error: no active clique.\n")
			else:
				if l:
					srv.cliques[active].write_message (Message (srv.cliques[active]._user, l.strip ('\n')))

"""
