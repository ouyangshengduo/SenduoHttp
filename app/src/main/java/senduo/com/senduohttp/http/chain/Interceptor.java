package senduo.com.senduohttp.http.chain;

import java.io.IOException;

import senduo.com.senduohttp.http.Response;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：拦截器接口
 * * 修改历史：2018/5/5 17:04*************************************
 **/

public interface Interceptor {

    Response intercept(InterceptorChain interceptorChain) throws IOException;
}
