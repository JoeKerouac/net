package com.joe.http.request;

public class IHttpPost extends IHttpRequestBase {
    IHttpPost(String url) {
        super(url);
    }

    /**
     * 构建POST构建器
     *
     * @param url 请求url
     * @return POST构建器
     */
    public static Builder builder(String url) {
        return new Builder(url);
    }

    public static final class Builder extends IHttpRequestBase.Builder<IHttpPost> {
        private Builder(String url) {
            super(url);
        }

        @Override
        public IHttpPost build() {
            IHttpPost post = new IHttpPost(super.url);
            super.configure(post);
            return post;
        }
    }
}
