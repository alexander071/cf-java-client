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

package org.cloudfoundry.doppler;

import java.util.Optional;
import java.util.UUID;

final class UuidUtils {

    private UuidUtils() {
    }

    static Optional<UUID> from(org.cloudfoundry.dropsonde.events.UUID dropsonde) {
        return Optional.ofNullable(dropsonde)
            .map(d -> new UUID(d.high, d.low));
    }

}