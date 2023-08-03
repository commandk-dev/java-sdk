package dev.commandk.javasdk;

import dev.commandk.javasdk.api.SdkApi;
import dev.commandk.javasdk.common.Headers;
import dev.commandk.javasdk.credential.CommandKCredentials;
import dev.commandk.javasdk.credential.CommandKCredentialsProvider;
import dev.commandk.javasdk.credential.DefaultCredentialsProviderChain;
import dev.commandk.javasdk.exception.ClientException;
import dev.commandk.javasdk.exception.ConfigException;
import dev.commandk.javasdk.exception.ResponseNotModifiedException;
import dev.commandk.javasdk.kvstore.GlobalKVStoreFactory;
import dev.commandk.javasdk.kvstore.KVStore;
import dev.commandk.javasdk.kvstore.KVStoreFactory;
import dev.commandk.javasdk.models.RenderedAppSecret;
import dev.commandk.javasdk.models.RenderedAppSecretsResponse;
import dev.commandk.javasdk.models.RenderingMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.util.*;

class CommandKResponse {

    @Nonnull
    private String eTag;
    @Nonnull
    private Object response;

    public CommandKResponse(@Nonnull String eTag, @Nonnull Object response){
        this.eTag = eTag;
        this.response = response;
    }

    @Nonnull
    String getETag() { return this.eTag; }

    @Nonnull
    Object getResponse() { return this.response; }
}

/**
 * CommandKClient provides an interface to communicate with the CommandK host
 */
public class CommandKClient {

    @Nonnull
    private KVStore<Object, CommandKResponse> eTagsStore;

    @Nonnull
    private SdkApi sdkApi;

    public CommandKClient(
            @Nonnull KVStoreFactory<? extends Object, ? extends Object> kvStoreFactory,
            @Nonnull CommandKCredentialsProvider commandKCredentialsProvider
    ) {
        if(commandKCredentialsProvider==null){
            throw new IllegalArgumentException("commandKCredentialsProvider is null");
        }
        if(kvStoreFactory==null){
            throw new IllegalArgumentException("kvStoreFactory return null");
        }

        CommandKCredentials credentials = commandKCredentialsProvider.resolveCredentials();
        if(credentials==null){
            throw new ConfigException("Provided CommandKCredentials object is null");
        }
        if(credentials.host==null || credentials.apiToken==null){
            throw new ConfigException("Provided CommandKCredentials object is missing host or apiToken");
        }

        KVStore<Object, CommandKResponse> kvStore = (KVStore<Object, CommandKResponse>) kvStoreFactory.getStore();
        if(kvStore==null){
            throw new ConfigException("KVStoreFactory return null");
        }
        this.eTagsStore = kvStore;
        this.sdkApi = CommandKClient.createSdkApiWithCredentials(credentials);
    }

    public static @Nonnull CommandKClient withDefaults() {
        return new CommandKClient(
                new GlobalKVStoreFactory<Object, CommandKResponse>(),
                DefaultCredentialsProviderChain.defaultCredentialsProviderChain()
        );
    }


    public @Nonnull List<RenderedAppSecret> getRenderedAppSecrets(
        @Nonnull String catalogAppId,
        @Nonnull String environmentId
    ) {
        GetRenderedAppSecretsRequest getRenderedAppSecretsRequest =
                new GetRenderedAppSecretsRequest(catalogAppId, environmentId);

        Optional<CommandKResponse> commandKResponseOptional = eTagsStore.get(getRenderedAppSecretsRequest);
        String ifNoneMatch = commandKResponseOptional.map(CommandKResponse::getETag).orElse("");

        try{
            ResponseEntity<RenderedAppSecretsResponse> renderedAppSecretsWithHttpInfo =
                    sdkApi.getRenderedAppSecretsWithHttpInfo(catalogAppId, environmentId, RenderingMode.FULL, ifNoneMatch);

            List<RenderedAppSecret> renderedAppSecrets = renderedAppSecretsWithHttpInfo.getBody().getSecrets();

            Map<String, List<String>> headers = renderedAppSecretsWithHttpInfo.getHeaders();
            List<String> eTagValue = headers.get(Headers.E_TAG);
            if(eTagValue!=null && !eTagValue.isEmpty()){
                String eTag = eTagValue.get(0);
                if(eTag!=null)
                    eTagsStore.set(getRenderedAppSecretsRequest, new CommandKResponse(eTag, renderedAppSecrets));
            }

            return renderedAppSecrets;
        } catch (Exception e) {
            if(e instanceof ResponseNotModifiedException){
                if(commandKResponseOptional.isPresent())
                    return (List<RenderedAppSecret>) commandKResponseOptional.get().getResponse();
                else throw new ClientException("Cannot find cached response for getRenderedAppSecrets()");
            } else if(e instanceof ClientException)
                throw e;
            else
                throw new ClientException(String.format("%s: %s", e.getClass(), e.getMessage()));
        }
    }

    static SdkApi createSdkApiWithCredentials(@Nonnull CommandKCredentials commandKCredentials) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new CommandKResponseErrorHandler());

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(commandKCredentials.host);
        apiClient.addDefaultHeader("Authorization", String.format("Bearer %s", commandKCredentials.apiToken));
        return new SdkApi(apiClient);
    }
}
