package ru.xaero.javacore.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BaseAuthService implements AuthService {

    private class Entry {

        @NotNull
        private String login;

        @NotNull
        private String pass;

        @NotNull
        private String nick;

        Entry(@NotNull String login,
              @NotNull String pass,
              @NotNull String nick) {
            this.login = login;
            this.pass = pass;
            this.nick = nick;
        }
    }

    @NotNull
    private ArrayList<Entry> entries;

    BaseAuthService() {
        entries = new ArrayList<>();
        entries.add(new Entry("login1", "pass1", "nick1"));
        entries.add(new Entry("login2", "pass2", "nick2"));
        entries.add(new Entry("login3", "pass3", "nick3"));
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Nullable
    @Override
    public String getNickByLoginPass(@NotNull String login, @NotNull String pass) {
        for (Entry o : entries) {
            if (o.login.equals(login) && o.pass.equals(pass)) return o.nick;
        }
        return null;
    }
}
