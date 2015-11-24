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

# File: kleeqgui.py

from Tkinter import *
import SocketServer
import threading
import sys
import socket
from ConfigParser import SafeConfigParser
from Clique import Clique
from KeyManager import KeyManager

from User import User
from Packet import Packet
from Util import Hash
from Engine import *
from messages.Message import messages.Message
from Listener import Listener

class KleeqGui:

    def __init__(self, master):

        frame = Frame(master)
        frame.pack()
        self._info_frame = Frame()
        self._convo_frame = Frame()
        

        self._conversation = Text(self._convo_frame, width="53")
        self._entry =  Entry(self._convo_frame, width="43")
        self._entry.bind("<Return>", self.__send_event)
        self._button_send = Button(self._convo_frame, text="SEND" , command=self.__send)
        self._button_quit = Button(self._convo_frame, text="QUIT", command=frame.quit)
        
        self._list_cliques_label = Label(self._info_frame,  text="List of Cliques")
        self._list_cliques = Listbox(self._info_frame, height="8")
        self._list_cliques.bind("<<ListboxSelect>>", self.__change_cliques_event)

        self._list_peers_label = Label(self._info_frame,  text="Clique Members")
        self._list_peers = Listbox(self._info_frame,height="8")
        self._list_peers.bind("<<ListboxSelect>>", self.__sync_event)        
        
        self._info_frame.pack(side=RIGHT)
        self._convo_frame.pack()
        self._conversation.pack()
        
        self._list_cliques_label.pack(side=TOP)
        self._list_cliques.pack(side=TOP)
        
        self._list_peers_label.pack(side=TOP)
        self._list_peers.pack(side=TOP)
        
        self._button_quit.pack(side=RIGHT)
        self._button_send.pack(side=RIGHT)        
        self._entry.pack(side=BOTTOM)
    
    
        cp = SafeConfigParser ()
        cp.read ('kleeq.conf')
        self._bind_address = ('', int (cp.get ('connection', 'port')))
        self._clique_list = cp.get ('cliques', 'list').split (',')
        self._srv = CliqueServer (self._bind_address, CliqueRequestHandler)
        self._srv.cnf = cp
        self._listeners = dict()
        for c in self._clique_list:
            me = User (cp.get (c, 'user'))
            peers = []
            for peer in cp.get (c, 'users').split (','):
                peers.append (User (peer))
            self._srv.cliques[c] = Clique (c, me, peers, Hash.hash (cp.get (c, 'secret')), self._srv.km)
            l = Listener(peers)
            self._srv.cliques[c].register_listener(l)
            self._listeners[c] = l 
            self._list_cliques.insert(END, c)
        self._sd = CliqueServerDaemon (self._srv)
        self._sd.setDaemon (True)
        self._sd.start ()
    
        self._active = None
        if cp.has_option ('cliques', 'default'):
            self.__change_cliques(cp.get ('cliques', 'default'))
        
    def __send(self):
        s = self._entry.get()
        m =messages.Message (self._srv.cliques[self._active]._user, s)
        self._srv.cliques[self._active].write_message (m)
        self._entry.delete(0,END)
        self.__update()
        
    def __send_event(self,event):
        self.__send()
    def __update(self, force=False):
        if self._listeners[self._active].merge_messages() or force:
            self._conversation.delete("0.0",END)
            self._conversation.insert (INSERT, self._listeners[self._active].get_as_string())
        self._conversation.see(END)
 
    def __change_cliques(self, clique_name):
        print "Change clique to" , clique_name
        if clique_name in self._srv.cliques:
            self._active = clique_name
            self._list_peers.delete(0,END)
            for peer in self._srv.cliques[self._active]._users:
                self._list_peers.insert(END, peer._name)
            self.__update(True)
        else:
            sys.stdout.write ("kleeq: clique not found.\n")
 
    def __change_cliques_event(self, event):
        print event.widget
        print event.type
        print event.widget.get(ACTIVE)
        self.__change_cliques(self._list_cliques.get(ACTIVE))
    def __sync_event(self, event):
        arg = self._list_peers.get(ACTIVE)

        s = socket.socket (socket.AF_INET, socket.SOCK_STREAM)
        if self._srv.cnf.has_section (arg):
            addr = (self._srv.cnf.get (arg, 'machine'), int (self._srv.cnf.get (arg, 'port')))
            p = self._srv.cliques[self._active].patch_request ()
            s.connect (addr)
            s.send (p.get_as_string ())
            s.close ()
            self.__update()
        
if __name__=="__main__":
    root = Tk()
    app = KleeqGui(root)
    root.mainloop()


        
