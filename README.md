# CommandK Java SDK
The CommandK SDK enables users to fetch secrets programmatically, either directly from within the apps to use or as part of a build pipeline to inject them into the app's artifact.

## Installation
Maven users:
```xml
<dependency>
   <groupId>dev.commandk</groupId>
   <artifactId>sdk</artifactId>
   <version>1.1.1</version>
</dependency>
```

Gradle users:
```gradle
dependencies {
    implementation 'dev.commandk:sdk:1.0.0'
}
```
## Usage
```java
CommandKClient commandKClient = CommandKClient.withDefaults();
List<RenderedAppSecret> appSecrets = commandKClient.getRenderedAppSecrets( "<APP_ID>", "<ENVIRONMENT_ID>");
```
This uses the default configuration for the client and should be enough to start consuming secrets from CommandK. Depending on how you want to configure the client you will have to either setup some system variables or implement some classes.

## Configuring the client
The CommandK client needs two values to communicate with the CommandK system. The domain/host where CommandK is installed and the API token to authenticate the requests to it.
### Default Configuration
```java
CommandKClient commandKClient = CommandKClient.withDefaults();
```
This will lookup the `HOST` and `API_TOKEN` for your CommandK installation from the following locations in the given order and pick up the first one it finds.
1. Environment variables: `COMMANDK_HOST` and `COMMANDK_API_TOKEN`
2. Java system properties: `commandk.host` and `commandk.apiToken`
3. Configuration file:
   3.1 At location provided in the environment variable `COMMANDK_CONFIG_FILE`
   3.2 At location provided in the java system property `commandk.configFile`

#### Using the configuration file
1. Create a file at `~/commandk.config`
```
host: https://api.<company>.commandk.dev
apiToken: <api_token>
```
2. Set the environment variable
```
export COMMANDK_CONFIG_FILE=~/commandk.config
```
Alternatively you could also use the system variable commandk.configFile
```
commandk.configFile=~/commandk.config
```
### Custom Configuration

If you want to override these defaults then you can use:
```java
CommandKClient commandKClient = CommandKClient(credentialsProvider, kvStoreFactory)
```

Below is a full example:
```java
package dev.commandk.javasdkexample;

import dev.commandk.javasdk.CommandKClient;
import dev.commandk.javasdk.credential.CommandKCredentials;
import dev.commandk.javasdk.credential.CommandKCredentialsProvider;
import dev.commandk.javasdk.kvstore.GlobalKVStoreFactory;
import dev.commandk.javasdk.kvstore.KVStore;
import dev.commandk.javasdk.kvstore.KVStoreFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Main {
   public static void main(String[] args) {
      KVStore<Object, Object> kvStore = new CustomKVStore<>();
      CommandKCredentialsProvider credentialsProvider = new CustomCredentialProvider();
      KVStoreFactory<Object, Object> kvStoreFactory = new CustomKVStoreFactory<Object, Object>(kvStore);
      CommandKClient commandKClient = new CommandKClient(kvStoreFactory, credentialsProvider);

      Object renderedCredentials = commandKClient.getRenderedAppSecrets(
              "<API_ID>",
              "<ENVIRONMENT_ID>"
      );
   }
}

class CustomCredentialProvider implements CommandKCredentialsProvider {
   @Override
   public CommandKCredentials resolveCredentials(){
      return new CommandKCredentials("https://commandk.dev/", "<API_TOKEN>");
   }
}

class CustomKVStoreFactory<K, V> implements KVStoreFactory<K, V> {
   KVStore<K, V> kvStore;
   public CustomKVStoreFactory(KVStore<K, V> kvStore) {
      this.kvStore = kvStore;
   }

   @Nonnull
   @Override
   public KVStore<K, V> getStore() {
      return this.kvStore;
   }
}

class CustomKVStore<K, V> implements KVStore<K, V> {
   Map<K, V> map = new HashMap<K, V>();

   @Nonnull
   @Override
   public Optional<V> get(@Nonnull K k) {
      return this.map.containsKey(k) ? Optional.of(this.map.get(k)) : Optional.empty();
   }

   @Override
   public void set(@Nonnull K k, @Nonnull V v) {
      this.map.put(k, v);
   }
}
```

*Note: The default KVStoreFactory, `GlobalKvStoreFactory`, used by the CommandKClient uses a thread safe static object as the underlying store. So all instances of it will use the same store. Hence, all default instantiations of the CommandKClient will also share the same KVStore.*

*Note: If your application/pipeline is multithreaded then please make sure that any custom implementations of `KvStore` and `KvStoreFactory` are thread safe as well.*