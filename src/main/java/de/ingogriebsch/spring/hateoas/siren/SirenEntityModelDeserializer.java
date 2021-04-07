/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ingogriebsch.spring.hateoas.siren;

import static java.lang.String.format;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static de.ingogriebsch.spring.hateoas.siren.RepresentationModelUtils.isRepresentationModel;
import static de.ingogriebsch.spring.hateoas.siren.RepresentationModelUtils.isRepresentationModelSubclass;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.hateoas.EntityModel;
import org.springframework.lang.Nullable;

/**
 * {@link JsonDeserializer} implementation which is able to deserialize a Siren entity into a {@link EntityModel}.
 * 
 * @author Ingo Griebsch
 * @since 1.0.0
 * @see EntityModel
 */
class SirenEntityModelDeserializer extends AbstractSirenDeserializer<EntityModel<?>> {

    private static final long serialVersionUID = -3683235541542548855L;
    private static final JavaType TYPE = defaultInstance().constructType(EntityModel.class);

    SirenEntityModelDeserializer(SirenConfiguration configuration, SirenDeserializerFacilities deserializerFacilities) {
        this(configuration, deserializerFacilities, TYPE);
    }

    SirenEntityModelDeserializer(SirenConfiguration configuration, SirenDeserializerFacilities deserializerFacilities,
        JavaType contentType) {
        super(configuration, deserializerFacilities, contentType);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contentType = property == null ? ctxt.getContextualType() : property.getType().getContentType();
        return new SirenEntityModelDeserializer(configuration, deserializerFacilities, contentType);
    }

    @Override
    protected EntityModel<?> deserializeModel(JsonParser jp, DeserializationContext ctxt) throws IOException {
        SirenEntityModelBuilder builder =
            SirenEntityModelBuilder.builder(contentType, getRepresentationModelFactories().forEntityModel(), getLinkConverter());

        while (!END_OBJECT.equals(jp.nextToken())) {
            if (FIELD_NAME.equals(jp.currentToken())) {
                String text = jp.getText();
                if ("properties".equals(text)) {
                    deserializeProperties(jp, ctxt, builder);
                }

                if ("entities".equals(text)) {
                    builder.content(deserializeEntities(jp, ctxt));
                }

                if ("links".equals(text)) {
                    builder.links(deserializeLinks(jp, ctxt));
                }

                if ("actions".equals(text)) {
                    builder.actions(deserializeActions(jp, ctxt));
                }
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void deserializeProperties(JsonParser jp, DeserializationContext ctxt, SirenEntityModelBuilder builder)
        throws IOException {
        JavaType containedType = obtainContainedType();
        if (!isRepresentationModel(containedType.getRawClass())) {
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(containedType);
            if (deserializer == null) {
                throw new JsonParseException(jp, format("No deserializer available for type '%s'!", containedType));
            }

            jp.nextToken();
            builder.content(deserializer.deserialize(jp, ctxt));
        } else if (isRepresentationModelSubclass(contentType.getRawClass())) {
            JavaType type = defaultInstance().constructMapType(Map.class, String.class, Object.class);
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(type);
            if (deserializer == null) {
                throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
            }

            jp.nextToken();
            builder.properties((Map<String, Object>) deserializer.deserialize(jp, ctxt));
        } else {
            throw new IllegalStateException(
                format("Cannot decide how to deserialize the given properties based on type '%s'!", contentType));
        }
    }

    private Object deserializeEntities(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> deserializer = obtainDeserializer(obtainContainedType(), jp, ctxt);

        List<Object> content = newArrayList();
        if (START_ARRAY.equals(jp.nextToken())) {
            while (!END_ARRAY.equals(jp.nextToken())) {
                content.add(deserializer.deserialize(jp, ctxt));
            }
        }

        return content.stream().collect(toOptional()).orElse(null);
    }

    private static List<SirenLink> deserializeLinks(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> deserializer = obtainDeserializer(defaultInstance().constructType(SirenLink.class), jp, ctxt);

        List<SirenLink> links = newArrayList();
        if (START_ARRAY.equals(jp.nextToken())) {
            while (!END_ARRAY.equals(jp.nextToken())) {
                links.add((SirenLink) deserializer.deserialize(jp, ctxt));
            }
        }

        return links;
    }

    private static List<SirenAction> deserializeActions(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonDeserializer<Object> deserializer = obtainDeserializer(defaultInstance().constructType(SirenAction.class), jp, ctxt);

        List<SirenAction> actions = newArrayList();
        if (START_ARRAY.equals(jp.nextToken())) {
            while (!END_ARRAY.equals(jp.nextToken())) {
                actions.add((SirenAction) deserializer.deserialize(jp, ctxt));
            }
        }

        return actions;
    }

    private static JsonDeserializer<Object> obtainDeserializer(JavaType type, JsonParser jp, DeserializationContext ctxt)
        throws JsonMappingException, JsonParseException {
        JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(type);
        if (deserializer == null) {
            throw new JsonParseException(jp, format("No deserializer available for type '%s'!", type));
        }
        return deserializer;
    }

}
