package dev.commandk.javasdk.credential;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import dev.commandk.javasdk.exception.ConfigException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class ConfigFileCredentialsProviderUtilities {
    public @Nonnull CommandKCredentials readConfigFromFile(String filePath){

        JavaPropsMapper javaPropsMapper = new JavaPropsMapper();
        File configFile = new File(filePath);
        CommandKCredentials commandKCredentials;

        try {
            commandKCredentials = javaPropsMapper.readerFor(CommandKCredentials.class).readValue(configFile);
        } catch (IOException e) {
            throw new ConfigException(String.format("%s: %s", e.getClass().toString(), e.getMessage()));
        }

        if(commandKCredentials.host == null || commandKCredentials.apiToken == null) {
            throw new ConfigException(String.format("Missing configuration values in %s", filePath));
        }
        return commandKCredentials;
    }
}