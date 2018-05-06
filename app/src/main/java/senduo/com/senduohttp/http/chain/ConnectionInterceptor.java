package senduo.com.senduohttp.http.chain;

import android.util.Log;

import java.io.IOException;

import senduo.com.senduohttp.http.HttpClient;
import senduo.com.senduohttp.http.HttpConnection;
import senduo.com.senduohttp.http.HttpUrl;
import senduo.com.senduohttp.http.Request;
import senduo.com.senduohttp.http.Response;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：获得有效连接的socket的拦截器
 * * 修改历史：2018/5/5 22:10*************************************
 **/

public class ConnectionInterceptor implements Interceptor {
    @Override
    public Response intercept(InterceptorChain interceptorChain) throws IOException {
        Log.e("interceptor", "获取连接拦截器");
        Request request = interceptorChain.call.getRequest();
        HttpClient httpClient = interceptorChain.call.getHttpClient();
        HttpUrl httpUrl = request.getHttpUrl();

        HttpConnection httpConnection = httpClient.getConnectionPool().getHttpConnection(httpUrl.getHost(),httpUrl.getPort());
        if(null == httpConnection){
            httpConnection = new HttpConnection();
        }else{
            Log.e("interceptor", "从连接池中获得连接");
        }
        httpConnection.setRequest(request);

        try {
            Response response = interceptorChain.proceed(httpConnection);
            if (response.isKeepAlive()){
                httpClient.getConnectionPool().putHttpConnection(httpConnection);
            }else{
                httpConnection.close();
            }
            return response;
        }catch (IOException e){
            httpConnection.close();
            throw e;
        }
    }
}
