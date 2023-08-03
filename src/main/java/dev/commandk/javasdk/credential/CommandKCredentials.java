package dev.commandk.javasdk.credential;

import javax.annotation.Nonnull;

public class CommandKCredentials {

    @Nonnull
    public String host, apiToken;

    public CommandKCredentials(){}

    public CommandKCredentials(@Nonnull String host, @Nonnull String apiToken) {
        this.host = host;
        this.apiToken = apiToken;
    }
}