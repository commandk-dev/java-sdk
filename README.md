# CommandK Java SDK
The CommandK SDK enables users to fetch secrets programmatically, either directly from within the apps to use or as part of a build pipeline to inject them into the app's artifact.

## Installation

### Gradle users
```gradle
repositories {
    maven {
        url = uri("https://mvn.cmdk.sh")
    }
    mavenCentral()
}

dependencies {
    implementation 'dev.commandk:java-sdk:0.1.0:all'
}
```

### Maven users
In your project's `pom.xml` file add the repository and dependency:
```xml
<project>

   <repositories>
      <repository>
         <id>commandk</id>
         <url>https://mvn.cmdk.sh</url>
      </repository>
   </repositories>

   <dependencies>
      <dependency>
         <groupId>dev.commandk</groupId>
         <artifactId>java-sdk</artifactId>
         <version>0.1.0</version>
         <classifier>all</classifier>
      </dependency>
   </dependencies>
</project>
```

## Usage
Let's look at a very basic example that configures the client and retrieves secrets. 
```java
package dev.commandk.example;

import dev.commandk.javasdk.CommandKClient;
import dev.commandk.javasdk.models.RenderedAppSecret;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // The host and access token need to be provided to the client somehow.
        // One of the ways the client tries to access it is by looking them up in the
        // system properties with the `commandk.host` and `commandk.apiToken` properties.
        System.setProperty("commandk.host", "https://api.commandk.dev");
        System.setProperty("commandk.apiToken", "<api_token>");

        // With the default configuration, the client will pick up the credentials from
        // the system properties we set.
        CommandKClient commandKClient = CommandKClient.withDefaults();

        // To fetch secrets of an app we need to specify the app id and the environment id
        List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(
                "<app_id>",
                "<environment>",  // staging, production, sandbox, development,
                List.of()
        );

        // And it's ready to be consumed
        renderedAppSecrets.forEach(renderedAppSecret -> {
            System.out.printf("%s: %s\n", renderedAppSecret.getKey(), renderedAppSecret.getSerializedValue());
        });
    }
}
```
This uses the default configuration for the client and should be enough to start consuming secrets from CommandK. Depending on how you want to configure the client you will have to either setup some system variables or implement some classes.

## Configuring the client
The CommandK client needs two values to communicate with the CommandK system. The domain/host where CommandK is installed and the API token to authenticate the requests to it. There are two ways this can be configured:
1. Using the default configuration with `CommandKClient.withDefaults();`
2. Providing a custom configuration with `CommandKClient(kvStoreFactory, credentialsProvider)`

### Default Configuration
```java
CommandKClient commandKClient = CommandKClient.withDefaults();
```
The default configuration is what the example above uses. It will look up the host and access token for your CommandK installation in this order and use up the first one it finds.
1. Environment variables: `COMMANDK_HOST` and `COMMANDK_API_TOKEN`
2. Java system properties: `commandk.host` and `commandk.apiToken`
3. Configuration file:
   3.1 At the location provided in the environment variable `COMMANDK_CONFIG_FILE`
   3.2 At the location provided in the java system property `commandk.configFile`

*In our example the client finds the credentials we set in the java system properties.*
> **NOTE** If you are using an on-prem installation of commandk, your host value would look like this `https://api.<installation-name>.commandk.dev`
> For the value of `host`, refer to the Customer Information Sheet that would have been shared by the CommandK team for your installation. Usually, if you access your dashboard at `app.<name>.commandk.dev`, then the host for your commandk installation would be `https://api.<name>.commandk.dev`

#### Using the configuration file
Instead of setting the credentials directly in the system properties you could also load them up from a file.
1. Create a file at `~/commandk.config`
```
host: https://api.commandk.dev
apiToken: <api_token>
```
2. Set the environment variable
```shell
export COMMANDK_CONFIG_FILE=~/commandk.config
```
or the system property through the `application.yml` file
```yaml
# application.yml
commandk.configFile=~/commandk.config
```
or like in the example set the system property in the application
```java
// Replace these line in the example

// The host and access token need to be provided to the client somehow.
// One of the ways the client tries to access it is by looking them up in the
// system properties with the `commandk.host` and `commandk.apiToken` properties.
System.setProperty("commandk.host", "https://api.commandk.dev");
System.setProperty("commandk.apiToken", "<api_token>");

// with this
System.setProperty("commandk.configFile", "~/commandk.config");
```
### Custom Configuration

If you want to override the way the credentials are provided then you construct the client by providing a CommandKCredentialProvider. A very simple one would look like this:
```java
class CustomCredentialsProvider implements CommandKCredentialsProvider {

    @Override
    public CommandKCredentials resolveCredentials() {
        return new CommandKCredentials("https://api.commandk.dev", "<api_token>");
    }
}
```
Construct the client like this:
```java
CommandKClient commandKClient = CommandKClient(new GlobalKVStoreFactory(), new CustomCredentialsProvider())
```
The client when constructed manually requires two things:
1. A `KVStoreFactory`, a factory class that will create a key value store for the client to use cache for fetched secrets. For this we will use the default one, `GlobalKVStoreFactory`, that ships with the client. 
2. A `CommandKCredentialsProvider`, in this case it is an instance of our `CustomCredentialProvider`.

Below is a full example:
```java
package dev.commandk.example;

import dev.commandk.javasdk.CommandKClient;
import dev.commandk.javasdk.credential.CommandKCredentials;
import dev.commandk.javasdk.credential.CommandKCredentialsProvider;
import dev.commandk.javasdk.kvstore.GlobalKVStoreFactory;
import dev.commandk.javasdk.models.RenderedAppSecret;
import java.util.List;

public class Main {
   public static void main(String[] args) {
      CommandKClient commandKClient = new CommandKClient(new GlobalKVStoreFactory(), new CredentialProviderExample());

      // To fetch secrets of an app we need to specify the app id and the environment id
      List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(
              "<app_id>",
              "<environment>",
              List.of()
      );

      // And it's ready to be consumed
      renderedAppSecrets.forEach(renderedAppSecret -> {
         System.out.printf("%s: %s\n", renderedAppSecret.getKey(), renderedAppSecret.getSerializedValue());
      });
   }
}

class CredentialProviderExample implements CommandKCredentialsProvider {

   @Override
   public CommandKCredentials resolveCredentials() {
      return new CommandKCredentials("https://api.commandk.dev", "<api_token>");
   }
}
```

*Note: The default `KVStoreFactory`, `GlobalKvStoreFactory`, used by the CommandKClient uses a thread safe static object as the underlying store. So all instances of it will use the same store. Hence, all default instantiations of the CommandKClient will also share the same KVStore. If your application/pipeline is multithreaded then please make sure that any custom implementations of `KvStore` and `KvStoreFactory` are also thread safe.*