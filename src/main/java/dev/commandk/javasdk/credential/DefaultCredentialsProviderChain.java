package dev.commandk.javasdk.credential;

import dev.commandk.javasdk.exception.ConfigException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DefaultCredentialsProviderChain implements CommandKCredentialsProvider {

    private List<CommandKCredentialsProvider> providers;

    DefaultCredentialsProviderChain(@Nonnull List<CommandKCredentialsProvider> providers) {
        this.providers = providers;
    }

    public static DefaultCredentialsProviderChain defaultCredentialsProviderChain() {
        return new DefaultCredentialsProviderChain(new ArrayList<CommandKCredentialsProvider>() {{
            add(new EnvironmentVariablesCredentialsProvider());
            add(new SystemPropertiesCredentialsProvider());
            add(new ConfigFileCredentialsProvider());
        }});
    }

    @Override
    public CommandKCredentials resolveCredentials() {
        for(CommandKCredentialsProvider commandKCredentialsProvider: this.providers) {
            CommandKCredentials commandKCredentials = commandKCredentialsProvider.resolveCredentials();
            if(commandKCredentials != null) return commandKCredentials;
        }
        throw new ConfigException("No valid credentials were found");
    }
}
