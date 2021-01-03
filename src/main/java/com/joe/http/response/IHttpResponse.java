package com.joe.http.response;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.joe.http.config.HeaderEnum;
import com.joe.http.exception.NetException;
import com.joe.http.exception.ServerException;
import com.joe.utils.common.string.StringUtils;
import com.joe.utils.serialize.json.JsonParser;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP响应，如果不使用必须关闭，如果调用过getresult方法可以不关闭
 *
 * PS:非线程安全，并发调用有可能会导致未知错误
 *
 * @author joe
 */
@Slf4j
public class IHttpResponse implements Closeable {

    /**
     * JSON解析器
     */
    private static final JsonParser JSON_PARSER = JsonParser.getInstance();

    /**
     * 请求状态
     */
    private int status;

    /**
     * 请求响应
     */
    private CloseableHttpResponse closeableHttpResponse;

    /**
     * 当前是否关闭
     */
    private boolean closed = false;

    /**
     * 响应数据
     */
    private String data;

    /**
     * 请求中的异常，如果没有异常那么该值为空
     */
    private NetException exception;

    /**
     * 响应header
     */
    private List<Header> headers;

    /**
     * 响应编码
     */
    private String charset;

    public IHttpResponse(CloseableHttpResponse closeableHttpResponse) {
        this.closeableHttpResponse = closeableHttpResponse;
        this.status = closeableHttpResponse.getStatusLine().getStatusCode();
        this.headers = Arrays.asList(closeableHttpResponse.getAllHeaders());
        // 从请求头中解析字符集
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(HeaderEnum.CONTENT_TYPE.getHeader())) {
                HeaderElement element = header.getElements()[0];
                String[] values = element.getValue().trim().split(";");
                if (values.length >= 2) {
                    try {
                        this.charset = Charset.forName(values[1]).name();
                    } catch (UnsupportedCharsetException e) {
                        log.warn("服务器响应content_type为：[{}]，解析的编码字符集为：[{}]，该字符集不存在", element.getValue().trim(),
                            values[1]);
                    }
                }
                break;
            }
        }
    }

    /**
     * 获取请求头
     *
     * @param name
     *            请求头的名字
     * @return 对应的值
     */
    public List<IHeader> getHeader(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return headers.stream().filter(header -> name.equals(header.getName())).map(IHeader::new)
            .collect(Collectors.toList());
    }

    /**
     * 将结果转换为字符串（调用此方法后input流将会关闭）
     *
     * @return 请求结果的字符串
     * @throws IOException
     *             IO异常
     */
    public String getResult() throws IOException {
        return getResult(null);
    }

    /**
     * 将结果转换为指定字符集字符串（调用此方法后input流将会关闭）
     *
     * @param charset
     *            结果字符集
     * @return 请求结果的字符串
     * @throws IOException
     *             IO异常
     */
    public String getResult(String charset) throws IOException {
        return getResult(charset, true);
    }

    /**
     * 将结果转换为指定字符集字符串，调用此方法后input流将会关闭，无论是否发生异常（有可能因为字符集异常）
     *
     * @param defaultCharset
     *            结果字符集
     * @param force
     *            是否强制使用指定字符集而忽略服务器响应字符集，true表示强制使用指定字符集，忽略服务器响应字符集
     * @return 请求结果的字符串
     * @throws IOException
     *             IO异常
     */
    public String getResult(String defaultCharset, boolean force) throws IOException {
        if (this.closed) {
            if (exception != null) {
                throw exception;
            }
            return this.data;
        }

        try {
            String charset = null;

            if (!force) {
                charset = getCharset();
            }

            if (StringUtils.isEmpty(charset)) {
                if (StringUtils.isEmpty(defaultCharset)) {
                    charset = Charset.defaultCharset().name();
                } else {
                    charset = defaultCharset;
                }
            }

            HttpEntity entity = this.closeableHttpResponse.getEntity();
            this.data = EntityUtils.toString(entity, charset);

            if (status >= 400) {
                ErrorResp resp = JSON_PARSER.read(this.data, ErrorResp.class);
                if (resp == null) {
                    resp = new ErrorResp();
                    resp.setMessage(data);
                    resp.setError(data);
                    resp.setStatus(status);
                    resp.setPath("unknown");
                    resp.setException("unknown");
                }
                exception = new ServerException(resp.getPath(), resp.getException(), resp.getMessage(), resp.getError(),
                    status);
                throw exception;
            }

            return this.data;
        } finally {
            close();
        }
    }

    /**
     * 以流的形式获取响应
     * 
     * @return 响应流
     * @throws IOException
     *             IOException
     */
    public InputStream getResultAsStream() throws IOException {
        byte[] data = EntityUtils.toByteArray(this.closeableHttpResponse.getEntity());
        return new ByteArrayInputStream(data);
    }

    /**
     * 获取响应编码字符集
     * 
     * @return 服务器响应编码字符集
     */
    public String getCharset() {
        return charset;
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
     * @throws IOException
     *             IO异常
     */
    public void close() throws IOException {
        if (!this.closed) {
            log.debug("关闭连接");
            this.closeableHttpResponse.close();
            this.closed = true;
        }
    }

    @Data
    private static final class ErrorResp {
        private String timestamp;
        private int status;
        private String error;
        private String exception;
        private String message;
        private String path;
    }
}
