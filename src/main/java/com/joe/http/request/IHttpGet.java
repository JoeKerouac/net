package com.joe.http.request;

import com.joe.http.client.IHttpClient;

public class IHttpGet extends IHttpRequestBase {
    IHttpGet(String url, IHttpClient client) {
        super(url, client);
    }

    /**
     * 构建GET构建器
     * 
     * @param url url
     * @return GET构建器
     */
    public static Builder builder(String url) {
        return new Builder(url);
    }

    public static final class Builder extends IHttpRequestBase.Builder<IHttpGet> {
        protected Builder(String url) {
            super(url);
        }

        @Override
        public IHttpGet build() {
            IHttpGet get = new IHttpGet(super.url, super.client);
            super.configure(get);
            return get;
        }
    }
}
