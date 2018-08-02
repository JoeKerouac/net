package com.joe.http.request;

import com.joe.http.client.IHttpClient;

public class IHttpGet extends IHttpRequestBase {
    public IHttpGet(String url) {
        super(url);
    }

    public IHttpGet(String url, IHttpClient client) {
        super(url, client);
    }

    /**
     * 构建GET构建器
     *
     * @return GET构建器
     */
    public static HttpGetBuilder builder() {
        return new HttpGetBuilder();
    }

    public static final class HttpGetBuilder extends Builder<IHttpGet> {
        private HttpGetBuilder() {
        }

        @Override
        public IHttpGet build() {
            checkUrl();
            IHttpGet get = new IHttpGet(super.url);
            super.configure(get);
            return get;
        }
    }
}
