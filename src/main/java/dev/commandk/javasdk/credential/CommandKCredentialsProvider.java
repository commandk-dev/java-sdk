package dev.commandk.javasdk.credential;

import javax.annotation.Nullable;

public interface CommandKCredentialsProvider {
    CommandKCredentials resolveCredentials();
}