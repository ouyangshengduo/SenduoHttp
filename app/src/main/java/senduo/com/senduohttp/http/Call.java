package senduo.com.senduohttp.http;

import java.io.IOException;
import java.util.ArrayList;

import senduo.com.senduohttp.http.chain.CallServiceInterceptor;
import senduo.com.senduohttp.http.chain.ConnectionInterceptor;
import senduo.com.senduohttp.http.chain.HeadersInterceptor;
import senduo.com.senduohttp.http.chain.Interceptor;
import senduo.com.senduohttp.http.chain.InterceptorChain;
import senduo.com.senduohttp.http.chain.RetryInterceptor;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：使用者通过使用这个类对象来进行网络的访问
 * * 修改历史：2018/5/5 11:28*************************************
 **/

public class Call {

    private HttpClient httpClient;
    private Request request;

    public Request getRequest() {
        return request;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    //TODO 是否被执行过
    boolean executed;

    //TODO 是否被取消了
    boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public Call(HttpClient httpClient, Request request){
        this.httpClient = httpClient;
        this.request = request;
    }




    /**
     * 获取返回
     * @return
     * @throws IOException
     */
    Response getResponse() throws IOException{
        ArrayList<Interceptor> interceptors = new ArrayList<>();
        interceptors.addAll(httpClient.getInterceptors());
        interceptors.add(new RetryInterceptor());
        interceptors.add(new HeadersInterceptor());
        interceptors.add(new ConnectionInterceptor());
        interceptors.add(new CallServiceInterceptor());
        InterceptorChain interceptorChain = new InterceptorChain(interceptors,0,this,null);
        Response response = interceptorChain.proceed();
        return response;
    }


    /**
     * 将Call对象放到调度器里面去执行，如果已经加过了，就不能加了
     * @param callback
     * @return
     */
    public Call enqueue(Callback callback){

        synchronized (this){
            if(executed){
                throw new IllegalStateException("This Call Already Executed!");
            }
            executed = true;
        }
        httpClient.getDispather().enqueue(new AsyncCall(callback));
        return this;
    }


    final class AsyncCall implements Runnable{

        private Callback callback;

        public AsyncCall(Callback callback){
            this.callback = callback;
        }

        @Override
        public void run() {

            boolean signalledCallback = false;
            try {
                Response response = getResponse();
                if(canceled){
                    signalledCallback = true;
                    callback.onFailure(Call.this,new IOException("this task had canceled"));
                }else{
                    signalledCallback = true;
                    callback.onResponse(Call.this,response);
                }
            } catch (IOException e) {
                if(!signalledCallback){
                    callback.onFailure(Call.this,e);
                }
            } finally {
                //将这个任务从调度器移除
                httpClient.getDispather().finished(this);
            }
        }

        public String getHost(){
            return request.getHttpUrl().getHost();
        }
    }
}
