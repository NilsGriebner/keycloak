package org.keycloak.utils;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MsAzureClient {

    private final String clientId;

    private final String clientSecret;

    private final String issuer;

    private String tenantId;

    public MsAzureClient(final String clientId, final String clientSecret, final String issuer) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.issuer = issuer;

        extractTenantId();
    }

    public List<String> getUserGroups(String userId) {
        GraphServiceClient graphClient = createCredentialAuthProvider();
        DirectoryObjectCollectionWithReferencesPage groupsPage =
                graphClient.users(userId).transitiveMemberOf()
                        .buildRequest()
                        .get();

        List<String> groupIds = new ArrayList<>();
        while (groupsPage != null) {
            List<DirectoryObject> groups = groupsPage.getCurrentPage();
            for (DirectoryObject directoryObject: groups) {
                if (directoryObject instanceof Group) {
                    Group group = (Group) directoryObject;
                    groupIds.add(group.id);
                }
            }
            final DirectoryObjectCollectionWithReferencesRequestBuilder nextPage = groupsPage.getNextPage();
            if(nextPage == null) {
                break;
            } else {
                groupsPage = nextPage.buildRequest().get();
            }
        }
        return groupIds;
    }

    private GraphServiceClient createCredentialAuthProvider() {
        final List<String> scopes = Collections.singletonList("https://graph.microsoft.com/.default");

        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build();

        final TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                scopes, credential);

        return GraphServiceClient.builder().authenticationProvider( authProvider ).buildClient();

    }

    private void extractTenantId() {
        String[] parts = issuer.split("/");
        tenantId = parts[parts.length-1];
    }
}
