package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.Either;
import com.googlecode.totallylazy.Left;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Right;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.io.Uri;
import com.googlecode.utterlyidle.annotations.GET;
import com.googlecode.utterlyidle.annotations.Path;
import com.googlecode.utterlyidle.annotations.QueryParam;
import com.googlecode.utterlyidle.dsl.DslTest;
import org.junit.Test;

import static com.googlecode.totallylazy.io.Uri.uri;
import static com.googlecode.totallylazy.proxy.Call.method;
import static com.googlecode.totallylazy.proxy.Call.on;
import static com.googlecode.utterlyidle.BaseUri.baseUri;
import static com.googlecode.utterlyidle.HttpHeaders.LOCATION;
import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.annotatedClass;
import static com.googlecode.utterlyidle.dsl.BindingBuilder.get;
import static com.googlecode.utterlyidle.dsl.BindingBuilder.pathParam;
import static com.googlecode.utterlyidle.dsl.BindingBuilder.queryParam;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BaseUriRedirectorTest {
    @Test
    public void canRedirectToRelativeUri() throws Exception {
        Response response = redirector().seeOther(uri("relative"));
        assertLocation(response, "http://server/base/relative");
    }

    @Test
    public void canConvertRelativeToFullyQualifiedUri() throws Exception {
        assertThat(redirector().absoluteUriOf(uri("relative")), is(uri("/base/relative")));
    }

    @Test
    public void extractsUrlFromABinding() throws Exception {
        Redirector redirector = redirector(get("/redirect").resource(method(on(DslTest.Redirect.class).redirect())).build());
        Response response = redirector.seeOther(method(on(DslTest.Redirect.class).redirect()));
        assertLocation(response, "http://server/base/redirect");
    }

    @Test
    public void supportsPathParameters() throws Exception {
        Redirector redirector = redirector(get("/redirect/{foo}").resource(method(on(DslTest.Redirect.class).redirect(pathParam(String.class, "foo")))).build());
        Response response = redirector.seeOther(method(on(DslTest.Redirect.class).redirect("bar")));
        assertLocation(response, "http://server/base/redirect/bar");
    }

    @Test
    public void supportsQueryParameters() throws Exception {
        Redirector redirector = redirector(get("/redirect").resource(method(on(DslTest.Redirect.class).redirect(queryParam(String.class, "foo")))).build());
        Response response = redirector.seeOther(method(on(DslTest.Redirect.class).redirect("bar")));
        assertLocation(response, "http://server/base/redirect?foo=bar");
    }
    
    @Test
    public void supportsIterableQueryParameters() throws Exception {
        Uri uri = redirector(annotatedClass(IterableParameterResource.class)).absoluteUriOf(method(on(IterableParameterResource.class).dosomething(Sequences.sequence("1234", "5678"))));
        assertThat(uri, is(uri("/base/path?id=1234&id=5678")));
    }

    public static class IterableParameterResource {
        @GET
        @Path("/path")
        public String dosomething(@QueryParam("id") Iterable<String> values) {
            return "foo";
        }
    }

    @Test
    public void supportsDefaultValue() throws Exception {
        Redirector redirector = redirector(get("/redirect").resource(method(on(DslTest.Redirect.class).redirect(queryParam(String.class, "foo", "Dan")))).build());
        Response response = redirector.seeOther(method(on(DslTest.Redirect.class).redirect(null)));
        assertLocation(response, "http://server/base/redirect?foo=Dan");
    }

    @Test @SuppressWarnings("unchecked")
    public void supportsOption() throws Exception {
        Redirector redirector = redirector(get("/redirect").resource(method(on(RedirectWithFunctionalTypes.class).optional(queryParam(Option.class, "optional")))).build());
        assertLocation(redirector.seeOther(method(on(RedirectWithFunctionalTypes.class).optional(Option.<String>none()))), "http://server/base/redirect");
        assertLocation(redirector.seeOther(method(on(RedirectWithFunctionalTypes.class).optional(Option.<String>some("baz")))), "http://server/base/redirect?optional=baz");
    }

    @Test @SuppressWarnings("unchecked")
    public void supportsEither() throws Exception {
        Redirector redirector = redirector(get("/redirect").resource(method(on(RedirectWithFunctionalTypes.class).either(queryParam(Either.class, "either")))).build());
        assertLocation(redirector.seeOther(method(on(RedirectWithFunctionalTypes.class).either(Left.<String, Integer>left("left")))), "http://server/base/redirect?either=left");
        assertLocation(redirector.seeOther(method(on(RedirectWithFunctionalTypes.class).either(Right.<String, Integer>right(100)))), "http://server/base/redirect?either=100");
    }

    @Test
    public void canExtractPathWithStreamingWriter() {
        Redirector redirector = redirector(annotatedClass(SomeResource.class));
        Response response = redirector.seeOther(method(on(SomeResource.class).getStreamingWriter("foo")));
        assertLocation(response, "http://server/base/path/foo");
    }

    @Test
    public void canHandleMultipleParametersWithTheSameName() {
        QueryParameters queryParameters = QueryParameters.queryParameters().
                add("name", "name1").
                add("name", "name2");
        Redirector redirector = redirector(annotatedClass(SomeResource.class));
        Response response = redirector.seeOther(method(on(SomeResource.class).getWithQueryParameters(queryParameters)));
        assertLocation(response, "http://server/base/path/?name=name1&name=name2");
    }


    private void assertLocation(Response response, String location) {
        assertThat(response.status(), is(Status.SEE_OTHER));
        assertThat(response.header(LOCATION).get(), is(location));
    }

    public static BaseUriRedirector redirector(Binding... values) {
        return new BaseUriRedirector(baseUri("http://server/base/"), bindings(values));
    }

    public static Bindings bindings(Binding... values) {
        RegisteredResources bindings = new RegisteredResources();
        bindings.add(values);
        return bindings;
    }

    public static class RedirectWithFunctionalTypes {
        public String optional(Option<String> bar){
            return "optional";
        }

        public String either(Either<String, Integer> bar){
            return "either";
        }
    }


}
