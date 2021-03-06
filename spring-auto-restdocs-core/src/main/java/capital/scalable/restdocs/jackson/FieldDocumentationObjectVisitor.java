/*
 * Copyright 2016 the original author or authors.
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

package capital.scalable.restdocs.jackson;

import java.util.Set;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public class FieldDocumentationObjectVisitor extends JsonObjectFormatVisitor.Base {

    private final FieldDocumentationVisitorContext context;
    private final String path;
    private final Set<JavaType> visited;

    public FieldDocumentationObjectVisitor(SerializerProvider provider,
            FieldDocumentationVisitorContext context, String path, Set<JavaType> visited) {
        super(provider);
        this.context = context;
        this.path = path;
        this.visited = visited;
    }

    @Override
    public void optionalProperty(BeanProperty prop) throws JsonMappingException {
        String jsonName = prop.getName();
        String fieldName = prop.getMember().getName();

        JavaType type = prop.getType();
        if (type == null) {
            throw new IllegalStateException("Missing type for property '" + jsonName + "', " +
                    "field '" + fieldName + "'");
        }

        JsonSerializer<?> ser = getSer(prop);
        if (ser == null) {
            return;
        }

        String fieldPath = path + (path.isEmpty() ? "" : ".") + jsonName;
        Class<?> javaBaseClass = prop.getMember().getDeclaringClass();
        boolean shouldExpand = shouldExpand(prop);

        InternalFieldInfo fieldInfo = new InternalFieldInfo(javaBaseClass, fieldName, fieldPath,
                shouldExpand);

        JsonFormatVisitorWrapper visitor = new FieldDocumentationVisitorWrapper(getProvider(),
                context, fieldPath, fieldInfo, visited);

        ser.acceptJsonFormatVisitor(visitor, type);
    }

    protected JsonSerializer<?> getSer(BeanProperty prop) throws JsonMappingException {
        JsonSerializer<Object> ser = null;
        if (prop instanceof BeanPropertyWriter) {
            ser = ((BeanPropertyWriter) prop).getSerializer();
        }
        if (ser == null) {
            ser = getProvider().findValueSerializer(prop.getType(), prop);
        }
        return ser;
    }

    private boolean shouldExpand(BeanProperty prop) {
        return prop.getMember().getAnnotation(RestdocsNotExpanded.class) == null;
    }
}
