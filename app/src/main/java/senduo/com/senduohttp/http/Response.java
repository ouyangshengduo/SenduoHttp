package senduo.com.senduohttp.http;

import java.util.HashMap;
import java.util.Map;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：存储网络请求返回的数据
 * * 修改历史：2018/5/5 11:27*************************************
 **/

public class Response {

    int code;//状态码
    int contentLength = -1;//返回包的长度
    Map<String,String> headers = new HashMap<>();//返回包的头信息
    String body;//包的内容
    boolean isKeepAlive;//是否保持连接

    public Response(){
    }

    public Response(int code, int contentLength, Map<String, String> headers, String body, boolean isKeepAlive) {
        this.code = code;
        this.contentLength = contentLength;
        this.headers = headers;
        this.body = body;
        this.isKeepAlive = isKeepAlive;
    }

    public int getCode() {
        return code;
    }

    public int getContentLength() {
        return contentLength;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }


}
