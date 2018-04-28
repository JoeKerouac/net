package com.joe.http.request;

public class IHttpGet extends IHttpRequestBase {
    public IHttpGet(String url) {
        super(url);
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
        private HttpGetBuilder(){}

        @Override
        public IHttpGet build() {
            checkUrl();
            IHttpGet get = new IHttpGet(super.url);
            super.configure(get);
            return get;
        }
    }
}
