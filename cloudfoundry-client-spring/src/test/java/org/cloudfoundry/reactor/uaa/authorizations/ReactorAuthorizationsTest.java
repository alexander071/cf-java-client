/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.reactor.uaa.authorizations;

import org.cloudfoundry.reactor.InteractionContext;
import org.cloudfoundry.reactor.TestRequest;
import org.cloudfoundry.reactor.TestResponse;
import org.cloudfoundry.reactor.uaa.AbstractUaaApiTest;
import org.cloudfoundry.uaa.authorizations.AuthorizeByAuthorizationCodeGrantApiRequest;
import org.cloudfoundry.uaa.authorizations.ResponseType;
import reactor.core.publisher.Mono;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;

public final class ReactorAuthorizationsTest {

    public static final class AuthorizeByAuthorizationCodeGrantApi extends AbstractUaaApiTest<AuthorizeByAuthorizationCodeGrantApiRequest, String> {

        private final ReactorAuthorizations authorizations = new ReactorAuthorizations(AUTHORIZATION_PROVIDER, HTTP_CLIENT, OBJECT_MAPPER, this.root);

        @Override
        protected InteractionContext getInteractionContext() {
            return InteractionContext.builder()
                .request(TestRequest.builder()
                    .method(GET).path("/oauth/authorize?client_id=login&redirect_uri=https://uaa.cloudfoundry.com/redirect/cf&response_type=code&state=v4LpFF")
                    .build())
                .response(TestResponse.builder()
                    .status(FOUND)
                    .header("Location", "https://uaa.cloudfoundry.com/redirect/cf?code=O6A5eT&state=v4LpFF")
                    .build())
                .build();
        }

        @Override
        protected String getResponse() {
            return "O6A5eT";
        }

        @Override
        protected AuthorizeByAuthorizationCodeGrantApiRequest getValidRequest() throws Exception {
            return AuthorizeByAuthorizationCodeGrantApiRequest.builder()
                .responseType(ResponseType.CODE)
                .clientId("login")
                .redirectUri("https://uaa.cloudfoundry.com/redirect/cf")
                .state("v4LpFF")
                .build();
        }

        @Override
        protected Mono<String> invoke(AuthorizeByAuthorizationCodeGrantApiRequest request) {
            return this.authorizations.authorizeByAuthorizationCodeGrantApi(request);
        }
    }

}