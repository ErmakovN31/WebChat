package ru.xaero.javacore.server;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

class MyServer {

    private final Vector<ClientHandler> clients = new Vector<>();
    private final AuthService authService = new BaseAuthService();
    private final String ONLINE = "/online ONLINE: \n";
    private final String WHISPER_PREFIX = "/w ";
    private final String WHISPER_FROM = ">> whisper from ";
    private final String SERVER_IS_WAITING = "Сервер ожидает подключения";
    private final String SERVER_ERROR = "Ошибка при работе сервера";
    private final String CLIENT_CONNECTED = "Клиент подключился";
    private final String MESSAGE_SEPARATOR = ": ";
    private final String NEW_LINE = "\n";
    private final String SPACE = " ";

    @NotNull
    private ServerSocket server;

    private int PORT = 8189;
    boolean isWhisper = false;

    MyServer() {
        try {
            server = new ServerSocket(PORT);
            authService.start();
            while (true) {
                System.out.println(SERVER_IS_WAITING);
                final Socket socket = server.accept();
                System.out.println(CLIENT_CONNECTED);
                newClientThreadFactory(socket);
            }
        } catch (IOException e) {
            System.out.println(SERVER_ERROR);
        } finally {
            closeSocket();
            authService.stop();
        }
    }

    private void newClientThreadFactory(@NotNull Socket socket) {
        new Thread(() -> {
            new ClientHandler(this, socket);
        }).start();
    }

    private void closeSocket() {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized boolean isNickBusy(@NotNull String nick) {
        for (ClientHandler o : clients) {
            if (o.getName().equals(nick)) return true;
        }
        return false;
    }

    synchronized void broadcastMsg(@NotNull String msg) {
        for (ClientHandler o : clients) {
            o.sendMessage(msg);
        }
    }

    synchronized void onlineRefresh() {
        StringBuilder online = new StringBuilder();
        online.append(ONLINE);
        for (ClientHandler o : clients) {
            String nickname = (o.getName() + NEW_LINE);
            online.append(nickname);
        }
        for (ClientHandler o : clients) {
            o.sendMessage(online.toString());
        }
    }

    synchronized void unsubscribe(@NotNull ClientHandler o) {
        clients.remove(o);
    }

    synchronized void subscribe(@NotNull ClientHandler o) {
        clients.add(o);
    }

    synchronized void checkWhisper(@NotNull String msg) {
        for (ClientHandler o : clients) {
            if (msg.startsWith(WHISPER_PREFIX + o.getName() + SPACE)) {
                isWhisper = true;
                StringBuilder whisper = new StringBuilder();
                String[] parts = msg.split(SPACE);
                for (int i = 2; i < parts.length; i++) {
                    whisper.append(parts[i]);
                    whisper.append(SPACE);
                }
                o.sendMessage(WHISPER_FROM + o.getName() + MESSAGE_SEPARATOR + whisper.toString());
            }
        }
    }

    @NotNull
    AuthService getAuthService() {
        return authService;
    }
}

