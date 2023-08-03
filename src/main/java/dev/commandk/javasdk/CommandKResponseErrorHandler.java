package dev.commandk.javasdk;

import dev.commandk.javasdk.exception.ClientException;
import dev.commandk.javasdk.exception.ResponseNotModifiedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

class CommandKResponseErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode() != HttpStatus.OK;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if(response.getStatusCode() == HttpStatus.NOT_MODIFIED) throw new ResponseNotModifiedException();
        throw new ClientException(String.format("Response from host: %d", response.getRawStatusCode()));
    }
}
