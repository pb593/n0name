package gui;

import com.sun.deploy.util.StringUtils;
import core.Client;
import exception.UserIDTakenException;
import message.TextMessage;
import scaffolding.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.util.List;

/**
 * Created by pb593 on 17/03/2016.
 */
public class ClientGUI extends JFrame {

    private Client client; // reference back to the Client instance
    private JFrame frame = this; // for use in action listeners

    private JList groupList;
    private DefaultListModel<String> groupListModel = new DefaultListModel<>();

    private JList history;
    private JButton sendButton;
    private JTextArea groupParticipantsField;
    private JButton addParticipantButton;
    private JTextField userIDField;
    private JButton newGroupButton;
    private JTextField inputMsgField;
    private JPanel rootPanel;

    public ClientGUI(Client client) {
        super("NoNaMe Chat");

        this.client = client; // reference back to the client instance
        userIDField.setText(client.getUserID());

        setContentPane(rootPanel);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        groupList.setModel(groupListModel);

        groupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) { // a group in list is selected
                if (!e.getValueIsAdjusting()) {
                    updateContent(); // refresh
                }
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // send button is pressed
                String selectedGroup = (String) groupList.getSelectedValue();
                String msgText = inputMsgField.getText();
                if(selectedGroup!= null && !msgText.equals("")) { // if the message is non-empty and a group is selected
                    boolean success = client.sendMessage(selectedGroup, msgText); // send
                    inputMsgField.setText(inputMsgField.getToolTipText());
                    if (success)
                        updateContent(); // refresh GUI
                }
            }
        });
        addParticipantButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // add participant to current group
                String groupID = (String) groupList.getSelectedValue();
                if(groupID != null) {
                    Object[] users = client.getOnlineUsers().toArray();
                    String userID = (String) JOptionPane.showInputDialog(frame,
                            "Please choose the user you would like to add...",
                            "Add a conversation participant",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            users,
                            "select user...");

                    if(userID != null) { // if user did not press 'Cancel'
                        boolean success = client.addParticipant(groupID, userID); // request Client to add the corresponding user

                        if (success)
                            updateContent(); // refresh
                    }
                }
            }
        });
        newGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // create group button pressed
                String newGroupID = (String) JOptionPane.showInputDialog(frame,
                        "Please enter the name of the new group...",
                        "Create a new group",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        null);

                if (newGroupID != null) { // if user did not press 'Cancel'
                    if (newGroupID.equals("") || client.getGroupList().contains(newGroupID)) // if invalid group
                        JOptionPane.showMessageDialog(frame,
                                "Invalid group name or this group already exists",
                                "Something went wrong",
                                JOptionPane.WARNING_MESSAGE);
                    else {
                        boolean success = client.createGroup(newGroupID); // request creation of a new group
                        if (success)
                            updateContent(); // refresh
                    }
                }
            }
        });
        inputMsgField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { // user pressed on the input field
                super.focusGained(e);
                inputMsgField.setText(""); // remove the default text
            }
        });
        inputMsgField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { // message input field loses focus
                super.focusLost(e);
                if (inputMsgField.getText().equals("")) {
                    inputMsgField.setText("Type message here..."); // remove the default text
                }
            }
        });
        inputMsgField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // 'Enter' key hit
                sendButton.doClick();
            }
        });
    }

    public void updateContent() { // this method is used by the client to force the GUI update its content

        List<String> groups = client.getGroupList();
        for(String g : groups) {
            if(!groupListModel.contains(g))
                groupListModel.addElement(g);
        }

        Object selectedObj = groupList.getSelectedValue();

        if(selectedObj != null) { // if a group is selected, fetch the message history

            String selectedGroup = selectedObj.toString();

            // update participants field
            groupParticipantsField.setText(StringUtils.join(client.getGroupParticipants(selectedGroup), ", "));

            // update message history
            List<TextMessage> msgs = client.getMessageHistory(selectedGroup);
            String[] entries = new String[msgs.size()];

            for(int i = 0; i < msgs.size(); i++) {
                entries[i] = String.format("%-20s: %s", msgs.get(i).author, msgs.get(i).text);
            }

            history.setListData(entries);
        }

    }

    public static void main(String[] args) {
        Client cl = null;
        boolean try_again = false;
        while(true) {
            String userID = (String) JOptionPane.showInputDialog(
                                null,
                                !try_again ? "Please pick a username" : "This username is taken. Please try another one.",
                                "Pick username",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                null,
                                Utils.randomAlphaNumeric(10));

            if(userID == null) // 'Cancel' button pressed
                System.exit(0); // just exit
            else { // if a userID has been entered
                try {
                    cl = new Client(userID);
                    break; // exit loop if Client successfully constructed
                } catch (UserIDTakenException e) {
                    try_again = true; // flag for window
                }
            }
        }
        cl.run(); // run the client in the same thread
    }


}
