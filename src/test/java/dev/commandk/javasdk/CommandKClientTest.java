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
import dev.commandk.javasdk.models.RenderedAppSecret;
import dev.commandk.javasdk.models.RenderedAppSecretValueType;
import dev.commandk.javasdk.models.RenderedAppSecretsResponse;
import dev.commandk.javasdk.models.RenderingMode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.*;

import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Mockito.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandKClient.class, RestTemplate.class})
public class CommandKClientTest {
    @Captor
    ArgumentCaptor<CommandKResponse> commandKResponseArgumentCaptor;

    @Test
    public void CommandKClientTest_nullCredentialProviderAndNullKVStoreFactory_throwException() {
        Assert.assertThrows(IllegalArgumentException.class, () -> { new CommandKClient(null, null); });
    }

    @Test
    public void CommandKClientTest_nullCredentialProvider_throwException() {
        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        KVStore kvStoreMock = mock(KVStore.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        Assert.assertThrows(IllegalArgumentException.class, () -> { new CommandKClient(kvStoreFactoryMock, null); });
    }

    @Test
    public void CommandKClientTest_nullKVStoreFactory_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("null", null));

        Assert.assertThrows(IllegalArgumentException.class, () -> { new CommandKClient(null, commandKCredentialsProviderMock); });
    }

    @Test
    public void CommandKClientTest_nullCredentials_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(null);

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        KVStore kvStoreMock = mock(KVStore.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        Assert.assertThrows(ConfigException.class, () -> { new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock); });
    }

    @Test
    public void CommandKClientTest_emptyCredential_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials(null, null));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        KVStore kvStoreMock = mock(KVStore.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        Assert.assertThrows(ConfigException.class, () -> { new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock); });
    }

    @Test
    public void CommandKClientTest_partialCredential1_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("null", null));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        KVStore kvStoreMock = mock(KVStore.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        Assert.assertThrows(ConfigException.class, () -> { new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock); });
    }

    @Test
    public void CommandKClientTest_partialCredential2_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials(null, "null"));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        KVStore kvStoreMock = mock(KVStore.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        Assert.assertThrows(ConfigException.class, () -> { new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock); });
    }

    @Test
    public void CommandKClientTest_nullKVStore_throwException() {
        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

        KVStoreFactory kvStoreFactoryMock = mock(KVStoreFactory.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(null);

        Assert.assertThrows(ConfigException.class, () -> { new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock); });
    }

    @Test
    public void getRenderedAppSecrets_whenEtagNotPresentAndApiReturnsEtagAndAppSecrets_returnAppSecretsAndStoreResponse() throws Exception {

        // Setup
        String catalogAppId = "catalogAppId";
        String environment = "environment";
        String returnedEtag = "etag";
        RenderedAppSecret returnedRenderedAppSecret = new RenderedAppSecret().key("key")
                .serializedValue("serializedValue").secretId("secretId")
                .valueType(RenderedAppSecretValueType.STRING);
        List<RenderedAppSecret> returnedRenderedAppSecrets = new ArrayList<RenderedAppSecret>() {{ add(returnedRenderedAppSecret); }};
        GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environment);

        KVStore<Object, CommandKResponse> kvStoreMock = mock(KVStore.class);
        when(kvStoreMock.get(any(Object.class))).thenReturn(Optional.empty());
        KVStoreFactory<Object, CommandKResponse> kvStoreFactoryMock = mock(KVStoreFactory.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

        SdkApi sdkApiMock = mock(SdkApi.class);
        when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString()))
                .thenReturn(new ResponseEntity<>(new RenderedAppSecretsResponse().secrets(returnedRenderedAppSecrets), new HttpHeaders(){{add(Headers.E_TAG, returnedEtag);}}, HttpStatus.OK));
        PowerMockito.whenNew(SdkApi.class).withAnyArguments().thenReturn(sdkApiMock);

        PowerMockito.whenNew(RestTemplate.class).withAnyArguments().thenReturn(mock(RestTemplate.class));

        // Test
        CommandKClient commandKClient = new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environment);

        Assert.assertEquals(renderedAppSecrets.size(), returnedRenderedAppSecrets.size());
        Assert.assertEquals(renderedAppSecrets.get(0), returnedRenderedAppSecret);

        verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environment, RenderingMode.FULL, "");
        verify(kvStoreMock).get(getRenderedAppSecretsRequest);

        verify(kvStoreMock).set(eq(getRenderedAppSecretsRequest), commandKResponseArgumentCaptor.capture());
        CommandKResponse commandKResponse = commandKResponseArgumentCaptor.getValue();
        List<RenderedAppSecret> cachedRenderedAppSecrets = (List<RenderedAppSecret>) commandKResponse.getResponse();
        Assert.assertEquals(cachedRenderedAppSecrets.size(), returnedRenderedAppSecrets.size());
        Assert.assertEquals(cachedRenderedAppSecrets.get(0), returnedRenderedAppSecret);
    }

    @Test
    public void getRenderedAppSecrets_whenEtagPresentAndApiReturns304_returnCachedResponse() throws Exception{

        // Setup
        String catalogAppId = "catalogAppId";
        String environment = "environment";
        String cachedEtag = "etag";
        RenderedAppSecret cachedRenderedAppSecret = new RenderedAppSecret().key("key")
                .serializedValue("serializedValue").secretId("secretId")
                .valueType(RenderedAppSecretValueType.STRING);
        List<RenderedAppSecret> cachedRenderedAppSecrets = new ArrayList<RenderedAppSecret>() {{ add(cachedRenderedAppSecret); }};
        GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environment);

        KVStore<Object, CommandKResponse> kvStoreMock = mock(KVStore.class);
        when(kvStoreMock.get(any(Object.class))).thenReturn(Optional.of(new CommandKResponse(cachedEtag, cachedRenderedAppSecrets)));
        KVStoreFactory<Object, CommandKResponse> kvStoreFactoryMock = mock(KVStoreFactory.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

        SdkApi sdkApiMock = mock(SdkApi.class);
        when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString()))
                .thenThrow(new ResponseNotModifiedException());
        PowerMockito.whenNew(SdkApi.class).withAnyArguments().thenReturn(sdkApiMock);
        PowerMockito.whenNew(RestTemplate.class).withAnyArguments().thenReturn(mock(RestTemplate.class));

        // Test
        CommandKClient commandKClient = new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        List<RenderedAppSecret> renderedAppSecrets = commandKClient.getRenderedAppSecrets(catalogAppId, environment);

        Assert.assertEquals(renderedAppSecrets.size(), cachedRenderedAppSecrets.size());
        Assert.assertEquals(renderedAppSecrets.get(0), cachedRenderedAppSecret);

        verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environment, RenderingMode.FULL, cachedEtag);
        verify(kvStoreMock).get(getRenderedAppSecretsRequest);
    }

    @Test
    public void getRenderedAppSecrets_whenApiThrowsApiException_clientThrowsClientException() throws Exception {

        // Setup
        String catalogAppId = "catalogAppId";
        String environment = "environment";
        String exceptionMessage = "This is a client exception";
        GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = new GetRenderedAppSecretsRequest(catalogAppId, environment);

        KVStore<Object, CommandKResponse> kvStoreMock = mock(KVStore.class);
        when(kvStoreMock.get(any(Object.class))).thenReturn(Optional.empty());
        KVStoreFactory<Object, CommandKResponse> kvStoreFactoryMock = mock(KVStoreFactory.class);
        when(kvStoreFactoryMock.getStore()).thenReturn(kvStoreMock);

        CommandKCredentialsProvider commandKCredentialsProviderMock = mock(CommandKCredentialsProvider.class);
        when(commandKCredentialsProviderMock.resolveCredentials()).thenReturn(new CommandKCredentials("", ""));

        SdkApi sdkApiMock = mock(SdkApi.class);
        when(sdkApiMock.getRenderedAppSecretsWithHttpInfo(anyString(), anyString(), any(), anyString()))
                .thenThrow(new ClientException(exceptionMessage));
        PowerMockito.whenNew(SdkApi.class).withAnyArguments().thenReturn(sdkApiMock);
        PowerMockito.whenNew(RestTemplate.class).withAnyArguments().thenReturn(mock(RestTemplate.class));

        // Test
        CommandKClient commandKClient = new CommandKClient(kvStoreFactoryMock, commandKCredentialsProviderMock);
        ClientException clientException = Assert.assertThrows(ClientException.class, () -> {
            commandKClient.getRenderedAppSecrets(catalogAppId, environment);
        });

        Assert.assertEquals(clientException.getMessage(), exceptionMessage);
        verify(sdkApiMock).getRenderedAppSecretsWithHttpInfo(catalogAppId, environment, RenderingMode.FULL, "");
        verify(kvStoreMock).get(getRenderedAppSecretsRequest);
    }
}