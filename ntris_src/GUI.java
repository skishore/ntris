package ntris_src;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.text.MaskFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.text.ParseException;
import java.util.*;

public class GUI {
    private static class EnterButton extends KeyAdapter {
        private final ActionListener listener;
        private final JButton button;

        public EnterButton(ActionListener l, JButton b) {
            listener = l;
            button = b;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                listener.actionPerformed(new ActionEvent(button, 0, button.getActionCommand()));
        }
    }

    private static final int SPACING = 16;

    public static final int OFFLINEGUI = 0;
    public static final int DISABLEDGUI = 1;
    public static final int ONLINEGUI = 2;

    private Container container;
    private ActionListener listener;
    private boolean autoMatch;

    // CardLayout and two GUI cards
    private CardLayout cardLayout;
    private JPanel buttonCard;
    private JPanel gameCard;

    // Upper-left quadrant components
    private JLabel status = new JLabel("Status: offline");
    private JButton play = new JButton("Play ntris");
    private JButton invite = new JButton("Send game invite");
    private JButton auto = new JButton("Auto-match a game");
    private JButton help = new JButton("Help");
    private JButton scores = new JButton("High scores");

    // Upper-right quadrant components
    private JLabel nameLabel = new JLabel("Username: ");
    private JTextField name = new JTextField();
    private JLabel passwordLabel = new JLabel("Password: ");
    private JPasswordField password = new JPasswordField();
    private JButton logon = new JButton("Log on");
    private JButton signup = new JButton("Sign up");

    // Lower-left quadrant components
    private JTextArea sentMessages = new JTextArea();
    private JTextField message = new JTextField();
    private JButton send = new JButton("Send");

    // Lower-right quadrant components
    private JList namesList = new JList();
    private JScrollPane namesScrollPane = new JScrollPane(namesList);
    private JLabel key = new JLabel("<html>Regular text: online<br><b>Bold: seeking a game</b><br><i>Italic: playing</i>");

    // Array of components and online and offline subsets
    private Component[] components =
            {status, play, invite, auto, help, scores,
             nameLabel, name, passwordLabel, password, logon, signup,
             sentMessages, message, send, namesList, key};
    private Component[] foci =
            {status, play, invite, auto, help, scores,
             name, password, logon, signup,
             message, send};
    private boolean[][] componentState =
            {{true, false, true}, {true, false, true}, {false, false, true},
             {false, false, true}, {true, false, true}, {false, false, true},

             {true, false, false}, {true, false, false}, {true, false, false},
             {true, false, false}, {true, false, false}, {true, false, true},

             {false, false, true}, {false, false, true}, {false, false, true},
             {false, false, true}, {false, false, true}};

    public GUI(Container container, ActionListener listener) {
        this.container = container;
        this.listener = listener;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
  
        cardLayout = new CardLayout();
        container.setLayout(cardLayout);
        for (Component component : foci)
            component.setFocusable(true);

        buttonCard = new JPanel();
        buttonCard.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(SPACING - 8, SPACING, SPACING, SPACING);
        constraints.weightx = 1.0;
        constraints.weighty = 0.04;

        // Set up the button group in the upper left
        constraints.gridx = 0;
        constraints.gridx = 0;

        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());

        GridBagConstraints subconstraints = new GridBagConstraints();
        subconstraints.fill = GridBagConstraints.BOTH;

        subconstraints.weightx = 1.0;
        subconstraints.weighty = 1.0;

        int inset = SPACING/4;
        subconstraints.insets = new Insets(0, inset, 3*inset, 0);
        subconstraints.gridx = 0;
        subconstraints.gridy = 0;
        subconstraints.gridwidth = 3;
        pane.add(status, subconstraints);

        subconstraints.insets = new Insets(0, 0, 0, 0);
        subconstraints.gridx = 0;
        subconstraints.gridy = 1;
        subconstraints.gridwidth = 1;
        subconstraints.gridheight = 2;
        createAction(play, "play");
        pane.add(play, subconstraints);

        subconstraints.gridx = 1;
        subconstraints.gridheight = 1;
        createAction(invite, "invite");
        pane.add(invite, subconstraints);

        subconstraints.gridy = 2;
        createAction(auto, "auto");
        pane.add(auto, subconstraints);

        subconstraints.gridx = 2;
        createAction(scores, "scores");
        pane.add(scores, subconstraints);

        subconstraints.gridy = 1;
        createAction(help, "help");
        pane.add(help, subconstraints);

        buttonCard.add(pane, constraints);

        // Set up the logon fields in the upper right
        constraints.insets = new Insets(SPACING - 8, 0, SPACING, SPACING);
        constraints.weightx = 0.1;
        constraints.gridx = 1;
        JPanel logonPane = new JPanel();
        logonPane.setLayout(new BoxLayout(logonPane, BoxLayout.PAGE_AXIS));

        Dimension size = new Dimension(
                (int)(container.getSize().getWidth()/8), (int)name.getPreferredSize().getHeight());
        name.setPreferredSize(size);
        name.addKeyListener(new EnterButton(listener, logon));
        nameLabel.setLabelFor(name);
        logonPane.add(pair(nameLabel, name));

        password.setPreferredSize(size);
        password.addKeyListener(new EnterButton(listener, logon));
        passwordLabel.setLabelFor(password);
        logonPane.add(pair(passwordLabel, password));

        createAction(logon, "logon");
        createAction(signup, "signup");
        logonPane.add(pair(logon, signup));
        buttonCard.add(logonPane, constraints);

        // Set up the usernames list in the lower right
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, SPACING, SPACING);
        constraints.weighty = 1.0;
        constraints.gridy = 1;

        namesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel namesPane = new JPanel();
        namesPane.setLayout(new BorderLayout());
        namesPane.add(namesScrollPane, BorderLayout.CENTER);

        //namesPane.add(key, BorderLayout.SOUTH);
        buttonCard.add(namesPane, constraints);

        // Set up the chat box in the lower left
        constraints.insets = new Insets(0, SPACING, SPACING, SPACING);
        constraints.weightx = 1.0;
        constraints.gridx = 0;

        JPanel chatPane = new JPanel();
        chatPane.setLayout(new BorderLayout());
        sentMessages.setEditable(false);
        sentMessages.setLineWrap(true);
        chatPane.add(new JScrollPane(sentMessages), BorderLayout.CENTER);

        JPanel messagePane = new JPanel();
        messagePane.setLayout(new BorderLayout());

        message.addKeyListener(new EnterButton(listener, send));
        messagePane.add(message, BorderLayout.CENTER);
        createAction(send, "send");
        messagePane.add(send, BorderLayout.EAST);
        chatPane.add(messagePane, BorderLayout.SOUTH);
        buttonCard.add(chatPane, constraints); 

        container.add(buttonCard, "buttonCard");
    }

    private void createAction(JButton button, String action) {
        button.setActionCommand(action);
        button.addActionListener(listener);
    }
    
    private static JPanel pair(Component left, Component right) {
        return pair(left, right, false);
    }

    private static JPanel pair(Component left, Component right, boolean vertical) {
        JPanel pane = new JPanel();
        if (vertical) {
            pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        } else {
            pane.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        }
        pane.add(left);
        pane.add(right);
        return pane;
    }

    public void createGamePanel(final Image buffer, final ImageObserver observer) {
        gameCard = new JPanel() {
            public void paintComponent(Graphics g) {
                g.drawImage(buffer, 0, 0, observer);
            }
        };
        container.add(gameCard, "gameCard");
    }

    public void setMode(int mode) {
        for (int i = 0; i < components.length; i++)
            components[i].setEnabled(componentState[i][mode]);
        invite.setEnabled(invite.isEnabled() && !autoMatch);

        final Color enabled = (key.isEnabled() ? Color.black : Color.gray);
        key.setForeground(enabled);
        namesList.setForeground(enabled);

        if (mode == OFFLINEGUI) {
            signup.setText("Sign up");
            signup.setActionCommand("signup");
            namesList.clearSelection();
        } else if (mode == ONLINEGUI) {
            signup.setText("Log off");
            signup.setActionCommand("logoff");
        }
    }

    public void setStatus(String newStatus) {
        status.setText("Status: " + newStatus);
    }

    public void changeMatchButtons(boolean invited, boolean seeking) {
        autoMatch = seeking; 
        invited = invited || seeking;

        if (seeking) {
            invite.setText("Send game invite");
            invite.setEnabled(false);

            auto.setText("Cancel search");
            auto.setActionCommand("cancel");
        } else if (invited) {
            invite.setText("Cancel invite");
            invite.setActionCommand("cancel");
        } else {
            invite.setText("Send game invite");
            invite.setActionCommand("invite");
            invite.setEnabled(true);

            auto.setText("Auto-match a game");
            auto.setActionCommand("auto");
        }
    }

    public String getLogonInfo() {
        return name.getText() + "." + password.getText();
    }

    public String getSelectedName() {
        Object selection = namesList.getSelectedValue();
        if ((selection != null) && (selection instanceof UserData))
            return ((UserData)selection).name;
        return null;
    }

    public String getMessage() {
        String text = message.getText();
        message.setText("");
        return text;
    }

    public void addMessage(String message) {
        if (!sentMessages.getText().equals(""))
            sentMessages.setText(sentMessages.getText() + "\n");
        sentMessages.setText(sentMessages.getText() + message);
        sentMessages.setCaretPosition(sentMessages.getDocument().getLength());
    }

    public void clearMessages() {
        sentMessages.setText("");
    }

    public void displayNames(Set<UserData> names) {
        Object selection = namesList.getSelectedValue();

        namesList.setListData(names.toArray());
        if ((selection != null) && names.contains(selection))
            namesList.setSelectedValue(selection, true);

        namesScrollPane.validate();
        namesScrollPane.repaint();
    }

    public void showButtonCard() {
        cardLayout.show(container, "buttonCard");
        gameCard.requestFocus();
    }

    public void showGameCard() {
        cardLayout.show(container, "gameCard");
        gameCard.requestFocus();
    }

    public void requestFocus() {
        gameCard.requestFocus();
    }

    public void addFocusListener(FocusListener listener) {
        gameCard.addFocusListener(listener);
    }

    public void addKeyListener(KeyListener listener) {
        gameCard.addKeyListener(listener);
    }

    public void removeKeyListener(KeyListener listener) {
        gameCard.removeKeyListener(listener);
    }

    public void showDialog(String text) {
        JOptionPane.showMessageDialog(container, text);
    }

    public void showDialog(final String text, boolean blocking) {
        if (blocking) {
            JOptionPane.showMessageDialog(container, text);
        } else {
            new Thread(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(container, text);
                }
            }).start();
        }
    }

    public void repaint() {
        gameCard.repaint();
    }
}
