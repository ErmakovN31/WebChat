package ru.xaero.javacore.client;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

class MyWindow extends JFrame {

    private final String SERVER_ADDR = "localhost";
    private final int SERVER_PORT = 8189;
    private final String AUTH_ACCEPT = "/authok ";
    private final String END_WORD = "/end";
    private final String AUTH_INIT = "/auth ";
    private final String ONLINE_REFRESH = "/online";
    private final String CONNECTION_ERROR = "Ошибка соединения. Попробуйте перезапустить программу";

    private final JPanel main = new JPanel();
    private final JPanel online = new JPanel();
    private final JPanel loginPanel = new JPanel();
    private final JPanel bottomPanel = new JPanel(new BorderLayout());
    private final JButton loginButton = new JButton();
    private final JTextArea onlineArea = new JTextArea();
    private final JTextArea messageArea = new JTextArea();
    private final JTextArea login = new JTextArea();
    private final JTextArea password = new JTextArea();
    private final JTextField messageField = new JTextField();
    private final JTextField loginField = new JTextField();
    private final JTextField passwordField = new JTextField();
    private final JScrollPane scrollMessages = new JScrollPane(messageArea);

    @NotNull
    private Socket socket;

    @NotNull
    private DataInputStream inStream;

    @NotNull
    private DataOutputStream outStream;

    MyWindow() {
        createWindow();
        try {
            socket = new Socket(SERVER_ADDR, SERVER_PORT);
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            Thread chatThread = chatThreadFactory();
            chatThread.setDaemon(true);
            chatThread.start();
            chatThreadFactory();
        } catch (SocketException exc) {
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWindow() {
        setBounds(600, 300, 490, 490);
        setResizable(false);
        setTitle("client");

        JButton jbSend = new JButton("SEND");
        main.setLayout(new GridLayout(1, 2));
        messageArea.setLineWrap(true);

        jbSend.addActionListener(sendAction());
        messageField.addActionListener(sendMsg());
        addWindowListener(closingWindow());

        add(main, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        main.add(scrollMessages);
        bottomPanel.add(jbSend, BorderLayout.EAST);
        bottomPanel.add(messageField, BorderLayout.CENTER);

        setVisible(true);
    }

    @NotNull
    private ActionListener sendAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!messageField.getText().trim().isEmpty()) {
                    sendMsg();
                    messageField.grabFocus();
                }
            }
        };
    }

    @NotNull
    private WindowAdapter closingWindow() {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    outStream.writeUTF(END_WORD);
                    outStream.flush();
                    socket.close();
                    outStream.close();
                    inStream.close();
                } catch (SocketException exc) {
                    System.exit(0);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
                super.windowClosing(e);
            }
        };
    }

    private void setAuthorized(boolean isAuthorized) {
        if (isAuthorized) {
            removeLoginPanel();
        } else {
            addLoginPanel();
        }
    }

    @NotNull
    private ActionListener sendMsg() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outStream.writeUTF(messageField.getText());
                    outStream.flush();
                    messageField.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        };
    }

    private void removeLoginPanel() {
        scrollMessages.setBounds(0, 0, 380, 435);
        online.setBounds(380, 0, 104, 434);
        online.setLayout(new BorderLayout());
        online.add(onlineArea, BorderLayout.CENTER);
        online.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        main.remove(loginPanel);
        main.setLayout(null);
        main.add(online);

        main.revalidate();
        online.revalidate();
    }

    private void addLoginPanel() {
        login.setBounds(0, 0, 250, 17);
        loginField.setBounds(0, 17, 250, 20);
        password.setBounds(0, 37, 250, 17);
        passwordField.setBounds(0, 54, 250, 20);
        loginButton.setBounds(0, 74, 80, 20);

        loginPanel.setLayout(null);
        login.setText("Login");
        login.setEditable(false);
        password.setText("Password");
        password.setEditable(false);
        loginButton.setText("Login");
        passwordField.addActionListener(passwordAction());
        loginButton.addActionListener(loginAction());

        main.add(loginPanel);
        loginPanel.add(login);
        loginPanel.add(loginField);
        loginPanel.add(password);
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
    }

    @NotNull
    private ActionListener loginAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outStream.writeUTF(AUTH_INIT + loginField.getText() + " " + passwordField.getText());
                    outStream.flush();
                    loginField.setText("");
                    passwordField.setText("");
                } catch (SocketException exc) {
                    System.exit(0);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        };
    }

    @NotNull
    private ActionListener passwordAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    outStream.writeUTF(AUTH_INIT + loginField.getText() + " " + passwordField.getText());
                    outStream.flush();
                    loginField.setText("");
                    passwordField.setText("");
                } catch (SocketException exc) {
                    System.exit(0);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        };
    }

    @NotNull
    private Thread chatThreadFactory() {
        return new Thread(() -> {
            try {
                authorizationCycle();
                gettingMessagesCycle();
            } catch (SocketException exc) {
                System.exit(0);
            } catch (EOFException e) {
                System.out.println(CONNECTION_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeSocket();
            }
        });
    }

    private void authorizationCycle() throws IOException {
        while (true) {
            String str = inStream.readUTF();
            if (str.startsWith(AUTH_ACCEPT)) {
                setAuthorized(true);
                break;
            }
            messageArea.append(str + "\n");
        }
    }

    private void gettingMessagesCycle() throws IOException {
        while (true) {
            String str = inStream.readUTF();
            if (str.equals(END_WORD)) {
                break;
            } else if (str.startsWith(ONLINE_REFRESH)) {
                String onlineMsg = str.substring(8);
                onlineArea.setText(onlineMsg);
            }
            if (!str.startsWith(ONLINE_REFRESH)) {
                messageArea.append(str + "\n");
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
