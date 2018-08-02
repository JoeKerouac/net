package com.joe.http.request;

import com.joe.http.client.IHttpClient;

public class IHttpPost extends IHttpRequestBase {
    public IHttpPost(String url) {
        super(url);
    }

    public IHttpPost(String url, IHttpClient client) {
        super(url, client);
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
        private HttpGetBuilder() {
        }

        @Override
        public IHttpPost build() {
            checkUrl();
            IHttpPost post = new IHttpPost(super.url);
            super.configure(post);
            return post;
        }
    }
}
