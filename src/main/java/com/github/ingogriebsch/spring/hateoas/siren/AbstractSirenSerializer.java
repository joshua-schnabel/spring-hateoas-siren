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

import java.util.List;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.MessageResolver;

abstract class AbstractSirenSerializer<T extends RepresentationModel<?>> extends ContainerSerializer<T>
    implements ContextualSerializer {

    private static final long serialVersionUID = -8665900081601124431L;
    protected static final String ATTR_KEY_PARENT = "__SIREN_ENTITY_PARENT_OBJECT__";

    protected final SirenEntityClassProvider sirenEntityClassProvider;
    protected final SirenEntityRelProvider sirenEntityRelProvider;
    protected final SirenConfiguration sirenConfiguration;
    protected final SirenLinkConverter sirenLinkConverter;
    protected final MessageResolver messageResolver;
    protected final BeanProperty property;

    protected AbstractSirenSerializer(Class<?> type, SirenConfiguration sirenConfiguration, SirenLinkConverter sirenLinkConverter,
        SirenEntityClassProvider sirenEntityClassProvider, SirenEntityRelProvider sirenEntityRelProvider,
        MessageResolver messageResolver, BeanProperty property) {
        super(type, false);
        this.sirenConfiguration = sirenConfiguration;
        this.sirenLinkConverter = sirenLinkConverter;
        this.sirenEntityClassProvider = sirenEntityClassProvider;
        this.sirenEntityRelProvider = sirenEntityRelProvider;
        this.messageResolver = messageResolver;
        this.property = property;
    }

    @Override
    public JavaType getContentType() {
        return null;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return null;
    }

    @Override
    public boolean hasSingleElement(T value) {
        return false;
    }

    @Override
    protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return null;
    }

    protected List<String> classes(RepresentationModel<?> model) {
        return sirenEntityClassProvider.get(model);
    }

    protected List<LinkRelation> rels(RepresentationModel<?> model, SerializerProvider provider) {
        return sirenEntityRelProvider.get(model, getAttribute(ATTR_KEY_PARENT, provider));
    }

    protected String title(Class<?> type) {
        return messageResolver.resolve(SirenEntity.TitleResolvable.of(type));
    }

    protected Object setAttribute(String key, Object value, SerializerProvider provider) {
        Object current = provider.getAttribute(key);
        provider.setAttribute(key, value);
        return current;
    }

    @SuppressWarnings("unchecked")
    protected T getAttribute(String key, SerializerProvider provider) {
        return (T) provider.getAttribute(key);
    }
}
