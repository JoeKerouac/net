package com.joe.http.request;

public class IHttpGet extends IHttpRequestBase {
    IHttpGet(String url) {
        super(url);
    }

    /**
     * 构建GET构建器
     * 
     * @param url
     *            url
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
            IHttpGet get = new IHttpGet(super.url);
            super.configure(get);
            return get;
        }
    }
}
