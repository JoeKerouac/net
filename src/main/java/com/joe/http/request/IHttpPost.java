package com.joe.http.request;

public class IHttpPost extends IHttpRequestBase {
    public IHttpPost(String url) {
        super(url);
    }

    /**
     * 构建POST构建器
     *
     * @return POST构建器
     */
    public static HttpGetBuilder builder() {
        return new HttpGetBuilder();
    }

    public static final class HttpGetBuilder extends Builder<IHttpPost> {
        private HttpGetBuilder() {}

        @Override
        public IHttpPost build() {
            checkUrl();
            IHttpPost post = new IHttpPost(super.url);
            super.configure(post);
            return post;
        }
    }
}
