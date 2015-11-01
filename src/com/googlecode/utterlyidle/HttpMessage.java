package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.functions.Functions;
import com.googlecode.totallylazy.functions.Unary;

import static com.googlecode.utterlyidle.HeaderParameters.headerParameters;
import static com.googlecode.utterlyidle.Parameters.Builder.replace;

public interface HttpMessage<T extends HttpMessage<T>> {
    HeaderParameters headers();

    default T headers(HeaderParameters value) {
        return create(value, entity());
    }

    Entity entity();

    default T entity(Entity value) {
        return create(headers(), value);
    }

    T create(HeaderParameters headers, Entity entity);

    interface Builder {
        static <T extends HttpMessage<T>> Unary<T> header(String name, Object value) {
            return header(replace(name, value));
        }

        @SafeVarargs
        static <T extends HttpMessage<T>> Unary<T> header(Unary<Parameters<?>>... builders) {
            return request -> Functions.modify(request, header(Functions.modify(request.headers(), builders)));
        }

        static <T extends HttpMessage<T>> Unary<T> header(Iterable<? extends Pair<String, String>> parameters) {
            return message -> message.headers(headerParameters(parameters));
        }

        static <T extends HttpMessage<T>> Unary<T> entity(Object value) {
            return message -> message.entity(Entity.entity(value));
        }
    }
}