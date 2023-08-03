package dev.commandk.javasdk.credential;

public class EnvironmentVariablesCredentialsProvider implements CommandKCredentialsProvider{

    @Override
    public CommandKCredentials resolveCredentials() {
        String commandKHostEnvVariable = System.getenv("COMMANDK_HOST");
        String commandKApiTokenEnvVariable = System.getenv("COMMANDK_API_TOKEN");

        if(commandKHostEnvVariable != null && commandKApiTokenEnvVariable !=null) {
            return new CommandKCredentials(commandKHostEnvVariable, commandKApiTokenEnvVariable);
        } else {
            return null;
        }
    }
}
