package ru.xaero.javacore.server;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

@Getter
class ClientHandler {

    private final String AUTH_INIT = "/auth ";
    private final String AUTH_ACCEPT = "/authok ";
    private final String END_WORD = "/end";
    private final String HELP = "/help";
    private final String[] COMMANDLIST = {AUTH_ACCEPT, AUTH_INIT, END_WORD, HELP};

    private final String AUTHORIZATION_TIME = "У вас есть 120 секунд на авторизацию";
    private final String AUTHORIZATION_DISCONNECT = "Клиент не авторизовался и будет отключен";
    private final String TIMEOUT_DISCONNECT = "Вы не успели авторизоваться и были отключены от сервера";
    private final String ACCOUNT_IS_USED = "Учетная запись уже используется";
    private final String WRONG_LOGIN = "Неверные логин/пароль";
    private final String CREATE_ERROR = "Проблемы при создании обработчика клиента";
    private final String CONNECTION_ERROR = "Ошибка соединения";
    private final String CONNECTED = " зашел в чат";
    private final String EXIT = " вышел из чата";
    private final String FROM = "от ";
    private final String WHISPER_TO = ">> whisper to ";
    private final String CHAT_SEPARATOR = ": ";
    private final String SPLIT_SEPARATOR = "\\s";
    private final String CMD_PREFIX = "/";
    private final String EMPTY = "";
    private final String SPACE = " ";

    @NotNull
    private final Thread authorization;

    @NotNull
    private final Thread gettingMessages;

    @NotNull
    private MyServer myServer;

    @NotNull
    private Socket socket;

    @NotNull
    private DataInputStream inStream;

    @NotNull
    private DataOutputStream outStream;

    @Nullable
    private String name;

    private boolean isClientAuthorized = false;

    ClientHandler(@NotNull MyServer myServer, @NotNull Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.inStream = new DataInputStream(socket.getInputStream());
            this.outStream = new DataOutputStream(socket.getOutputStream());
            this.name = EMPTY;
            this.authorization = authorizationThreadFactory();
            this.gettingMessages = gettingMessagesThreadFactory();

            authorizationStart();
            if (isClientAuthorized) {
                authorization.interrupt();
                gettingMessages.start();
            } else {
                authorizationDisconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(CREATE_ERROR);
        }
    }

    private void authorizationStart() throws IOException {
        try {
            authorization.start();
            outStream.writeUTF(AUTHORIZATION_TIME);
            authorization.join(120000);
        } catch (InterruptedException e) {
            System.out.println(AUTHORIZATION_DISCONNECT);
        }
    }

    private void authorizationDisconnect() throws IOException {
        outStream.writeUTF(TIMEOUT_DISCONNECT);
        outStream.flush();
        authorization.interrupt();
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + EXIT);
        myServer.onlineRefresh();
        socket.close();
    }

    public void sendMessage(@NotNull String msg) {
        try {
            outStream.writeUTF(msg);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private Thread authorizationThreadFactory() {
        return new Thread(() -> {
            try {
                while (true) {
                    String str = inStream.readUTF();
                    if (str.startsWith(AUTH_INIT)) {
                        String[] parts = str.split(SPLIT_SEPARATOR);
                        String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                        if (checkLogin(nick)) break;
                    }
                }
            } catch (SocketException exc) {
                System.out.println(AUTHORIZATION_DISCONNECT);
                closeSocket();
            } catch (IOException exc) {
                System.out.println(CONNECTION_ERROR);
                closeSocket();
            }
        });
    }

    private boolean checkLogin(@Nullable String nick) throws IOException {
        if (nick == null) {
            sendMessage(WRONG_LOGIN);
            return false;
        } else {
            return checkNickname(nick);
        }
    }

    private boolean checkNickname(@NotNull String nick) throws IOException {
        if (!myServer.isNickBusy(nick)) {
            sendMessage(AUTH_ACCEPT + nick);
            name = nick;
            myServer.broadcastMsg(name + CONNECTED);
            myServer.subscribe(this);
            myServer.onlineRefresh();
            isClientAuthorized = true;
            return true;
        } else {
            sendMessage(ACCOUNT_IS_USED);
            return false;
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private Thread gettingMessagesThreadFactory() {
        return new Thread(() -> {
            try {
                while (true) {
                    String str = inStream.readUTF();
                    System.out.println(FROM + name + CHAT_SEPARATOR + str);
                    if (str.equals(END_WORD)) break;
                    if (str.equals(HELP)) writeHelpMessage();
                    chooseMessageRoute(str);
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            } finally {
                myServer.unsubscribe(this);
                myServer.broadcastMsg(name + EXIT);
                myServer.onlineRefresh();
                closeSocket();
            }
        });
    }

    private void chooseMessageRoute(@NotNull String str) throws IOException {
        myServer.checkWhisper(str);
        if (!myServer.isWhisper && !str.startsWith(CMD_PREFIX)) {
            myServer.broadcastMsg(name + CHAT_SEPARATOR + str);
        } else if (myServer.isWhisper) {
            sendWhisper(str);
        }
        myServer.isWhisper = false;
    }

    private void writeHelpMessage() throws IOException {
        StringBuilder help = new StringBuilder();
        for (String o : COMMANDLIST) {
            help.append(o).append(SPACE);
        }
        outStream.writeUTF(help.toString());
    }

    private void sendWhisper(@NotNull String str) throws IOException {
        String[] parts = str.split(SPLIT_SEPARATOR);
        String to = parts[1];
        String whisper = str.substring(4 + parts[1].length());
        outStream.writeUTF(WHISPER_TO + to + CHAT_SEPARATOR + whisper);
    }
}

