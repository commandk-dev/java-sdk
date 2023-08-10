package dev.commandk.javasdk;

import dev.commandk.javasdk.api.SdkApi;
import dev.commandk.javasdk.common.Headers;
import dev.commandk.javasdk.credential.CommandKCredentials;
import dev.commandk.javasdk.credential.CommandKCredentialsProvider;
import dev.commandk.javasdk.exception.ClientException;
import dev.commandk.javasdk.exception.ConfigException;
import dev.commandk.javasdk.exception.ResponseNotModifiedException;
import dev.commandk.javasdk.kvstore.KVStore;
import dev.commandk.javasdk.kvstore.KVStoreFactory;
import dev.commandk.javasdk.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommandKClientTest {

    @Test
    public void CommandKClientTest_nullCredentialProviderAndNullKVStoreFactory_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CommandKClient(null, null);
        });
    }

    @Test
    public void CommandKClientTest_nullCredentialProvider_throwException() {
        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);

        assertThrows(IllegalArgumentException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, null);
        });
    }

    @Test
    public void CommandKClientTest_nullKVStoreFactory_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);

        assertThrows(IllegalArgumentException.class, () -> {
            new CommandKClient(null, commandKCredentialsProviderMock);
        });
    }

    @Test
    public void CommandKClientTest_nullCredentials_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(null);

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);

        assertThrows(ConfigException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        });
    }

    @Test
    public void CommandKClientTest_emptyCredential_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials(null, null));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);

        assertThrows(ConfigException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        });
    }

    @Test
    public void CommandKClientTest_partialCredential1_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("null", null));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);

        assertThrows(ConfigException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        });
    }

    @Test
    public void CommandKClientTest_partialCredential2_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials(null, "null"));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);

        assertThrows(ConfigException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        });
    }

    @Test
    public void CommandKClientTest_nullKVStore_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(null);

        assertThrows(ConfigException.class, () -> {
            new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        });
    }

    @Nested
    public class RenderedAppSecrets {
        @Captor
        ArgumentCaptor<CommandKResponse> commandKResponseArgumentCaptor;
        String catalogAppId = "catalogAppId";
        String environmentName = "environment-name";
        String environmentId = "environment-id";
        List<EnvironmentDescriptor> environmentDescriptors = Collections.singletonList(new EnvironmentDescriptor().id(environmentId).name(environmentName).slug(environmentName).label("any-label"));

        SdkApi sdkApiMock = mock(SdkApi.class);
        KVStore<Object, CommandKResponse> kvStoreMock = mock(KVStore.class);
        KVStoreFactory<Object, CommandKResponse> kvStoreFactoryMock = mock(KVStoreFactory.class);

        CommandKClient commandKClient;

        @BeforeEach
        public void setup() throws IllegalAccessException {
            when(sdkApiMock.getEnvironmentsWithHttpInfo(null, null, null)).thenReturn(new ResponseEntity<>(new GetAllEnvironmentsResponse().environments(environmentDescriptors), HttpStatus.OK));

            when(kvStoreMock.get(any(Object.class))).thenReturn(Optional.empty());
            when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

            CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
            when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

            commandKClient = new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
            // Setting the private field sdkApi in commandKClient to the mock
            Field field = ReflectionUtils.findFields(CommandKClient.class, f -> f.getName().equals("sdkApi"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
            field.setAccessible(true);
            field.set(commandKClient, sdkApiMock);
        }

        @Test
        public void getRenderedAppSecrets_whenEtagNotPresentAndApiReturnsEtagAndAppSecrets_returnAppSecretsAndStoreResponse() throws Exception {

            // Setup
            String returnedEtag = "etag";
            RenderedAppSecret returnedRenderedAppSecret = new RenderedAppSecret().key("key").serializedValue("serializedValue").secretId("secretId").valueType(RenderedAppSecretValueType.STRING);
            List<RenderedAppSecret> returnedRenderedAppSecrets = new ArrayList<RenderedAppSecret>() {{
                add(returnedRenderedAppSecret);
            }};
            GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environmentId);

            when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString(), anyList())).thenReturn(new ResponseEntity<>(new RenderedAppSecretsResponse().secrets(returnedRenderedAppSecrets), new HttpHeaders() {{
                add(Headers.E_TAG, returnedEtag);
            }}, HttpStatus.OK));

            // Test
            List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environmentName, null);

            assertEquals(renderedAppSecrets.size(), returnedRenderedAppSecrets.size());
            assertEquals(renderedAppSecrets.get(0), returnedRenderedAppSecret);

            verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, "", emptyList());
            verify(kvStoreMock).get(getRenderedAppSecretsRequest);

            verify(kvStoreMock).set(eq(getRenderedAppSecretsRequest), commandKResponseArgumentCaptor.capture());
            CommandKResponse commandKResponse = commandKResponseArgumentCaptor.getValue();
            List<RenderedAppSecret> cachedRenderedAppSecrets = (List<RenderedAppSecret>) commandKResponse.getResponse();
            assertEquals(cachedRenderedAppSecrets.size(), returnedRenderedAppSecrets.size());
            assertEquals(cachedRenderedAppSecrets.get(0), returnedRenderedAppSecret);
        }

        @Test
        public void getRenderedAppSecrets_whenEtagPresentAndApiReturns304_returnCachedResponse() throws Exception {

            // Setup
            String cachedEtag = "etag";
            RenderedAppSecret cachedRenderedAppSecret = new RenderedAppSecret().key("key").serializedValue("serializedValue").secretId("secretId").valueType(RenderedAppSecretValueType.STRING);
            List<RenderedAppSecret> cachedRenderedAppSecrets = new ArrayList<RenderedAppSecret>() {{
                add(cachedRenderedAppSecret);
            }};
            GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environmentId);

            when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString(), anyList())).thenThrow(new ResponseNotModifiedException());
            when(kvStoreMock.get(any(Object.class))).thenReturn(Optional.of(new CommandKResponse(cachedEtag, cachedRenderedAppSecrets)));

            // Test
            List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environmentName, null);

            assertEquals(renderedAppSecrets.size(), cachedRenderedAppSecrets.size());
            assertEquals(renderedAppSecrets.get(0), cachedRenderedAppSecret);

            verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, cachedEtag, emptyList());
            verify(kvStoreMock).get(getRenderedAppSecretsRequest);
        }

        @Test
        public void getRenderedAppSecrets_whenApiThrowsApiException_clientThrowsClientException() throws Exception {

            // Setup
            String exceptionMessage = "This is a client exception";
            GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environmentId);

            when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString(), anyList())).thenThrow(new ClientException(exceptionMessage));

            // Test
            ClientException clientException = assertThrows(ClientException.class, () -> {
                commandKClient.getRenderedAppSecrets(catalogAppId, environmentName, null);
            });

            assertEquals(clientException.getMessage(), exceptionMessage);
            verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, "", emptyList());
            verify(kvStoreMock).get(getRenderedAppSecretsRequest);
        }


        @Test
        public void getRenderedAppSecrets_whenQueryingForSpecificSecrets_returnValuesForOnlyQueriedSecrets() throws Exception {

            // Setup
            String returnedEtag = "etag";
            RenderedAppSecret returnedRenderedAppSecret1 = new RenderedAppSecret().key("secret1").serializedValue("serializedValue1").secretId("secret1Id").valueType(RenderedAppSecretValueType.STRING);

            RenderedAppSecret returnedRenderedAppSecret2 = new RenderedAppSecret().key("secret2").serializedValue("serializedValue2").secretId("secret2Id").valueType(RenderedAppSecretValueType.STRING);
            List<RenderedAppSecret> allRenderedAppSecrets = Arrays.asList(returnedRenderedAppSecret1, returnedRenderedAppSecret2);
            List<RenderedAppSecret> filteredRenderedAppSecrets = Collections.singletonList(returnedRenderedAppSecret1);

            List<String> secretNameFilter = Collections.singletonList("secret1");

            when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(eq(catalogAppId), eq(environmentId), any(), anyString(), eq(emptyList()))).thenReturn(new ResponseEntity<>(new RenderedAppSecretsResponse().secrets(allRenderedAppSecrets), new HttpHeaders() {{
                add(Headers.E_TAG, returnedEtag);
            }}, HttpStatus.OK));
            when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(eq(catalogAppId), eq(environmentId), any(), anyString(), eq(secretNameFilter))).thenReturn(new ResponseEntity<>(new RenderedAppSecretsResponse().secrets(filteredRenderedAppSecrets), new HttpHeaders() {{
                add(Headers.E_TAG, returnedEtag);
            }}, HttpStatus.OK));

            // Test

            // When no secrets are queried, all secrets are returned
            List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environmentName, null);

            assertEquals(renderedAppSecrets.size(), allRenderedAppSecrets.size());

            Set<RenderedAppSecret> expectedSet = new HashSet<>(allRenderedAppSecrets);
            Set<RenderedAppSecret> actualSet = new HashSet<>(renderedAppSecrets);
            assertEquals(expectedSet, actualSet);

            verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, "", emptyList());

            // When secrets are queried, only those secrets are returned
            renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environmentName, secretNameFilter);
            assertEquals(renderedAppSecrets.size(), filteredRenderedAppSecrets.size());
            expectedSet = new HashSet<>(filteredRenderedAppSecrets);
            actualSet = new HashSet<>(renderedAppSecrets);
            assertEquals(expectedSet, actualSet);

            verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, "", secretNameFilter);
        }

    }
}
