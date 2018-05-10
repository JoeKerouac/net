package com.joe.http.response;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP响应，如果不使用必须关闭，如果调用过getresult方法可以不关闭
 *
 * @author joe
 */
public class IHttpResponse implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(IHttpResponse.class);
    // 请求状态
    private int status;
    // 请求响应
    private CloseableHttpResponse closeableHttpResponse;
    // 是否关闭
    private boolean closed = false;
    // 响应数据
    private String data;
    private List<Header> headers;

    public IHttpResponse(CloseableHttpResponse closeableHttpResponse) throws IOException {
        this.closeableHttpResponse = closeableHttpResponse;
        this.status = closeableHttpResponse.getStatusLine().getStatusCode();
        this.headers = Arrays.asList(closeableHttpResponse.getAllHeaders());
    }

    /**
     * 将结果转换为字符串（调用此方法后input流将会关闭）
     *
     * @return 请求结果的字符串
     * @throws IOException IO异常
     */
    public String getResult() throws IOException {
        return getResult(null);
    }

    /**
     * 获取请求头
     *
     * @param name 请求头的名字
     * @return 对应的值
     */
    public List<IHeader> getHeader(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return headers.stream().filter(header -> name.equals(header.getName())).map(IHeader::new).collect(Collectors.toList());
    }

    /**
     * 将结果转换为指定字符集字符串（调用此方法后input流将会关闭）
     *
     * @param charset 结果字符集
     * @return 请求结果的字符串
     * @throws IOException IO异常
     */
    public String getResult(String charset) throws IOException {
        if (this.closed) {
            return this.data;
        }
        if (charset == null) {
            charset = Charset.defaultCharset().name();
        }
        HttpEntity entity = this.closeableHttpResponse.getEntity();
        this.data = EntityUtils.toString(entity, charset);
        close();
        return this.data;
    }

    public InputStream getResultAsStream() throws IOException {
        byte[] data = EntityUtils.toByteArray(this.closeableHttpResponse.getEntity());
        return new ByteArrayInputStream(data);
    }

    /**
     * 获取响应HTTP状态
     *
     * @return http状态码
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * 关闭响应流
     *
     * @throws IOException IO异常
     */
    public void close() throws IOException {
        if (!this.closed) {
            logger.debug("关闭连接");
            this.closeableHttpResponse.close();
            this.closed = true;
        }
    }
}
