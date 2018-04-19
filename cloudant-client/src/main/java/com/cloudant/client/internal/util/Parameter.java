package com.cloudant.client.internal.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {

    String USE_FIELD_NAME = "BodyParameter.USE_FIELD_NAME";

    String value() default USE_FIELD_NAME;

    Type type();

    enum Type {
        QUERY_PARAMETER,
        BODY_PARAMETER
    }

}
