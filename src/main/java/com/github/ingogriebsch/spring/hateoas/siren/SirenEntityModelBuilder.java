/*-
 * #%L
 * Spring HATEOAS Siren
 * %%
 * Copyright (C) 2018 - 2020 Ingo Griebsch
 * %%
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
 * #L%
 */
package com.github.ingogriebsch.spring.hateoas.siren;

import static com.github.ingogriebsch.spring.hateoas.siren.SirenNavigables.navigables;
import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = PRIVATE)
class SirenEntityModelBuilder {

    @NonNull
    private final JavaType type;
    @NonNull
    private final EntityModelFactory modelFactory;
    @NonNull
    private final SirenLinkConverter linkConverter;

    private List<SirenAction> actions = newArrayList();
    private List<SirenLink> links = newArrayList();
    private Object content;

    static SirenEntityModelBuilder builder(@NonNull JavaType type, @NonNull EntityModelFactory modelFactory,
        @NonNull SirenLinkConverter linkConverter) {
        return new SirenEntityModelBuilder(type, modelFactory, linkConverter);
    }

    SirenEntityModelBuilder content(@NonNull Object content) {
        this.content = content;
        return this;
    }

    SirenEntityModelBuilder links(List<SirenLink> links) {
        this.links = links != null ? links : newArrayList();
        return this;
    }

    SirenEntityModelBuilder actions(List<SirenAction> actions) {
        this.actions = actions != null ? actions : newArrayList();
        return this;
    }

    private Iterable<Link> links() {
        return linkConverter.from(navigables(links, actions));
    }

    EntityModel<?> build() {
        return modelFactory.create(type, links(), content);
    }
}
