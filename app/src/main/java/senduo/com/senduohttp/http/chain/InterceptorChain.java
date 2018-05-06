package senduo.com.senduohttp.http.chain;

import java.io.IOException;
import java.util.List;

import senduo.com.senduohttp.http.Call;
import senduo.com.senduohttp.http.HttpConnection;
import senduo.com.senduohttp.http.Response;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：存储拦截器列表，并启动拦截器
 * * 修改历史：2018/5/5 17:05*************************************
 **/

public class InterceptorChain {


    final List<Interceptor> interceptors;
    final int index;
    final Call call;
    HttpConnection httpConnection;


    public InterceptorChain(List<Interceptor> interceptors, int index, Call call,HttpConnection httpConnection) {
        this.interceptors = interceptors;
        this.index = index;
        this.call = call;
        this.httpConnection = httpConnection;
    }

    public Response proceed(HttpConnection httpConnection) throws IOException{
        this.httpConnection = httpConnection;
        return proceed();
    }

    public Response proceed() throws IOException{
        if(index > interceptors.size()){
            throw new IOException("Interceptor Chain Error");
        }
        Interceptor interceptor = interceptors.get(index);
        InterceptorChain next = new InterceptorChain(interceptors,index + 1,call, httpConnection);
        Response response = interceptor.intercept(next);
        return response;
    }


}
