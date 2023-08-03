package dev.commandk.javasdk.credential;

import dev.commandk.javasdk.exception.ConfigException;

import javax.annotation.Nullable;

public class ConfigFileCredentialsProvider implements CommandKCredentialsProvider{
    @Override
    public CommandKCredentials resolveCredentials() {
        String commandKConfigFilePathEnvVariable = System.getenv("COMMANDK_CONFIG_FILE");
        String commandKConfigFilePathProperty = System.getProperty("commandk.configFile");

        String configFilePath;
        if(commandKConfigFilePathEnvVariable !=null ) {
            configFilePath = commandKConfigFilePathEnvVariable;
        } else if(commandKConfigFilePathProperty !=null ) {
            configFilePath = commandKConfigFilePathProperty;
        } else {
            return null;
        }
        CommandKCredentials commandKCredentials = (new ConfigFileCredentialsProviderUtilities()).readConfigFromFile(configFilePath);
        if(commandKCredentials == null) throw new ConfigException(String.format("Missing configuration in file '%s'", commandKConfigFilePathProperty));
        return commandKCredentials;
    }
}
