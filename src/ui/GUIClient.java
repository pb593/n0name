package ui;

import core.Client;
import core.Clique;
import exception.MessengerOfflineException;
import exception.UserIDTakenException;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import message.TextMessage;
import org.apache.commons.lang3.StringUtils;
import scaffolding.AddressBook;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * Created by pb593 on 17/03/2016.
 */
public class GUIClient extends Client {

    private JFrame frame;

    private JList groupList;
    private DefaultListModel<String> groupListModel = new DefaultListModel<>();

    private JList history;
    private JButton sendButton;
    private JButton addParticipantButton;
    private JTextField userIDField;
    private JButton newGroupButton;
    private JTextField inputMsgField;
    private JPanel rootPanel;
    private JTextField groupParticipantsField;
    private JLabel onlineIndicator;
    private JScrollPane historyScrollPane;

    private ImageIcon[] status_icon = new ImageIcon[2];

    private final String prompt_txt = "Type message here...";

    public GUIClient(String userID) throws UserIDTakenException, MessengerOfflineException {
        super(userID);

        frame = new JFrame("NoNaMe Chat");

        userIDField.setText(userID);

        frame.setContentPane(rootPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        groupList.setModel(groupListModel);

        // load online and offline icons
        status_icon[1] = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("online_dot.png"));
        status_icon[0] = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("offline_dot.png"));

        onlineIndicator.setIcon(status_icon[1]); // set 'online'

        new javafx.embed.swing.JFXPanel(); // forces JavaFX init

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
                if(selectedGroup!= null && !msgText.isEmpty() && !msgText.equals(prompt_txt)
                                                                                && cliques.containsKey(selectedGroup)) {
                    // if the message is non-empty and a group is selected and known
                    cliques.get(selectedGroup).sendMessage(new TextMessage(msgText, userID, selectedGroup));
                    inputMsgField.setText("");
                    updateContent(); // refresh GUI
                }
            }
        });
        addParticipantButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // add participant to current group
                String groupID = (String) groupList.getSelectedValue();
                if(groupID != null) {
                    Object[] users = AddressBook.getAll().keySet().toArray();
                    String userID = (String) JOptionPane.showInputDialog(frame,
                            "Please choose the user you would like to add...",
                            "Add a conversation participant",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            users,
                            "select user...");

                    if(userID != null && !userID.equals(myself.getUserID()) && cliques.containsKey(groupID)) {
                        // if user did not press 'Cancel' and group is known
                        // also, can't add yourself
                        boolean success = cliques.get(groupID).addMember(userID);
                        if(success) updateContent(); // refresh GUI
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
                    if (newGroupID.equals("") || cliques.containsKey(newGroupID)) // if invalid group
                        JOptionPane.showMessageDialog(frame,
                                "Invalid group name or this group already exists",
                                "Something went wrong",
                                JOptionPane.WARNING_MESSAGE);
                    else {
                        Clique c = new Clique(newGroupID, myself, comm);
                        cliques.put(newGroupID, c); // insert new clique
                        addressTags.put(c.getCurrentAddressTag(), newGroupID);
                        c.start(); // patching and sealing component
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
                    inputMsgField.setText(prompt_txt); // remove the default text
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

    private void playSound() {
        Media icq = new Media(Thread.currentThread().getContextClassLoader().getResource("sound.mp3").toString());
        MediaPlayer mp = new MediaPlayer(icq);
        mp.play();
    }

    @Override
    protected void updateContent() { // this method is used by the client to force the GUI update its content

        // add all missing items
        for (String g : cliques.keySet()) {
            if (!groupListModel.contains(g))
                groupListModel.addElement(g);

            if(cliques.get(g).tapNewMessagesAvailable()) { // if have new messages
                playSound(); // produce annoying sound
            }
        }

        // TODO: remove all redundant items (deleted groups etc)

        Object selectedObj = groupList.getSelectedValue();

        if (selectedObj != null) { // if a group is selected, fetch the message history

            String selectedGroup = selectedObj.toString();

            // update participants field
            groupParticipantsField.setText(StringUtils.join(cliques.get(selectedGroup).getUserList(), ", "));

            // update message history
            List<TextMessage> msgs = cliques.get(selectedGroup).getHistory();
            String[] entries = new String[msgs.size()];

            for (int i = 0; i < msgs.size(); i++) {
                entries[i] = String.format("%-20s: %s", msgs.get(i).author, msgs.get(i).text);
            }

            history.setListData(entries);


            historyScrollPane.getVerticalScrollBar().setValue(historyScrollPane.getVerticalScrollBar().getMaximum());
            // scroll all the way down
        }

    }

    @Override
    protected void setIsOnline(boolean isOnline) { // called by Client to change GUI look depending on net status
        onlineIndicator.setIcon(status_icon[isOnline ? 1 : 0]); // status icon
        addParticipantButton.setEnabled(isOnline); //  ability to add participants
        newGroupButton.setEnabled(isOnline); //  ability to create new conversations
    }


    @Override
    public void run() {
        super.run();
        frame.setVisible(true); // set myself visible to the user
    }
}
