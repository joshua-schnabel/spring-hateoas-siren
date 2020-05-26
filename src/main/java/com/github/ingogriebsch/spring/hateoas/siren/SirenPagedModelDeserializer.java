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

import static java.lang.String.format;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;

import lombok.NonNull;

class SirenPagedModelDeserializer extends AbstractSirenDeserializer<PagedModel<?>> {

    private static final long serialVersionUID = 4364222303241126575L;
    private static final JavaType TYPE = defaultInstance().constructType(PagedModel.class);

    public SirenPagedModelDeserializer(@NonNull SirenConfiguration sirenConfiguration,
        @NonNull SirenLinkConverter linkConverter) {
        this(sirenConfiguration, linkConverter, TYPE);
    }

    public SirenPagedModelDeserializer(@NonNull SirenConfiguration sirenConfiguration, @NonNull SirenLinkConverter linkConverter,
        JavaType contentType) {
        super(sirenConfiguration, linkConverter, contentType);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        return new SirenPagedModelDeserializer(sirenConfiguration, linkConverter,
            property == null ? ctxt.getContextualType() : property.getType().getContentType());
    }

    @Override
    protected PagedModel<?> deserializeModel(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        SirenPagedModelBuilder builder = SirenPagedModelBuilder.builder(contentType.getRawClass(), linkConverter);
        while (jp.nextToken() != null) {
            if (FIELD_NAME.equals(jp.currentToken())) {
                if ("properties".equals(jp.getText())) {
                    builder.metadata(deserializeMetadata(jp, ctxt));
                }

                if ("entities".equals(jp.getText())) {
                    builder.content(deserializeEntities(jp, ctxt));
                }

                if ("links".equals(jp.getText())) {
                    builder.links(deserializeLinks(jp, ctxt));
                }

                if ("actions".equals(jp.getText())) {
                    builder.actions(deserializeActions(jp, ctxt));
                }
            }
        }
        return builder.build();
    }

    private List<Object> deserializeEntities(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JavaType type = obtainContainedType();
        JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(type);
        if (deserializer == null) {
            throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
        }

        List<Object> content = newArrayList();
        if (START_ARRAY.equals(jp.nextToken())) {
            while (!END_ARRAY.equals(jp.nextToken())) {
                content.add(deserializer.deserialize(jp, ctxt));
            }
        }
        return content;
    }

    private PageMetadata deserializeMetadata(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JavaType type = defaultInstance().constructType(PageMetadata.class);
        JsonDeserializer<Object> deserializer = ctxt.findNonContextualValueDeserializer(type);
        if (deserializer == null) {
            throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
        }

        JsonToken nextToken = jp.nextToken();
        if (!START_OBJECT.equals(nextToken)) {
            throw new JsonParseException(jp, String.format("Token does not represent '%s' [but '%']!", START_OBJECT, nextToken));
        }

        return (PageMetadata) deserializer.deserialize(jp, ctxt);
    }

    private List<SirenLink> deserializeLinks(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JavaType type = defaultInstance().constructType(SirenLink.class);
        JsonDeserializer<Object> deserializer = ctxt.findContextualValueDeserializer(type, null);
        if (deserializer == null) {
            throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
        }

        List<SirenLink> links = newArrayList();
        if (START_ARRAY.equals(jp.nextToken())) {
            while (!END_ARRAY.equals(jp.nextToken())) {
                links.add((SirenLink) deserializer.deserialize(jp, ctxt));
            }
        }

        return links;
    }

    private List<SirenAction> deserializeActions(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JavaType type = defaultInstance().constructType(SirenAction.class);
        JsonDeserializer<Object> deserializer = ctxt.findContextualValueDeserializer(type, null);
        if (deserializer == null) {
            throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
        }

        List<SirenAction> actions = newArrayList();
        if (JsonToken.START_ARRAY.equals(jp.nextToken())) {
            while (!JsonToken.END_ARRAY.equals(jp.nextToken())) {
                actions.add((SirenAction) deserializer.deserialize(jp, ctxt));
            }
        }

        return actions;
    }
}
