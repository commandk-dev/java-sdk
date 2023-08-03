package dev.commandk.javasdk;

class GetRenderedAppSecretsRequest {
    String cataglogAppId, environmentId;

    GetRenderedAppSecretsRequest(String catalogAppId, String environmentId){
        this.cataglogAppId = catalogAppId;
        this.environmentId = environmentId;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GetRenderedAppSecretsRequest) {
            GetRenderedAppSecretsRequest getRenderedAppSecretsRequest = (GetRenderedAppSecretsRequest) obj;
            return this.cataglogAppId.equals(getRenderedAppSecretsRequest.cataglogAppId)
                    && this.environmentId.equals(getRenderedAppSecretsRequest.environmentId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.cataglogAppId.hashCode() & this.environmentId.hashCode();
    }
}