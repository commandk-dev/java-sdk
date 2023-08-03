package dev.commandk.javasdk.credential;

public class SystemPropertiesCredentialsProvider implements CommandKCredentialsProvider{

    @Override
    public CommandKCredentials resolveCredentials() {
        String commandKHostProperty = System.getProperty("commandk.host");
        String commandKApiTokenProperty = System.getProperty("commandk.apiToken");

        if(commandKHostProperty != null && commandKApiTokenProperty != null) {
            return new CommandKCredentials(commandKHostProperty, commandKApiTokenProperty);
        } else {
            return null;
        }
    }
}