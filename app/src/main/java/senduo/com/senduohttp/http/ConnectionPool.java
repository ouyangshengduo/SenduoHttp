package senduo.com.senduohttp.http;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述： 与服务器之间的socket连接池
 * * 修改历史：2018/5/5 22:24*************************************
 **/

public class ConnectionPool {

    private long keepAliveTime;

    private Deque<HttpConnection> httpConnections = new ArrayDeque<>();

    private boolean cleanupRunning;

    private static final Executor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable,"This is Connection Pool");
            thread.setDaemon(true);//设为守护线程
            return thread;
        }
    });


    public ConnectionPool(){
        this(60L,TimeUnit.SECONDS);
    }

    public ConnectionPool(long keepAliveTime,TimeUnit timeUnit){
        this.keepAliveTime = timeUnit.toMillis(keepAliveTime);
    }


    //TODO 生成一个清理线程,这个线程会定期去检查，并且清理那些无用的连接，这里的无用是指没使用的间期超过了保留时间
    private Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {

            while(true){
                long now = System.currentTimeMillis();
                long waitDuration = cleanup(now);//获取到下次检测时间
                if(waitDuration == -1){
                    return;//连接池为空，清理线程执行结束
                }

                if(waitDuration > 0){
                    synchronized (ConnectionPool.this){
                        try {
                            ConnectionPool.this.wait(waitDuration);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    /**
     * 根据当前时间，清理无用的连接
     * @param now
     */
    private long cleanup(long now) {

        long longestIdleDuration = -1;//最长闲置的时间
        synchronized (this){
            Iterator<HttpConnection> connectionIterator = httpConnections.iterator();
            while(connectionIterator.hasNext()){
                HttpConnection httpConnection = connectionIterator.next();//获得连接
                //计算闲置时间
                long idleDuration = now - httpConnection.lastUseTime;

                //根据闲置时间来判断是否需要被清理
                if(idleDuration > keepAliveTime){
                    connectionIterator.remove();
                    httpConnection.close();
                    Log.e("ConnectionPool", "超过闲置时间,移出连接池");
                    continue;
                }

                //然后就整个连接池中最大的闲置时间
                if(idleDuration > longestIdleDuration){
                    longestIdleDuration = idleDuration;
                }
            }

            if(longestIdleDuration >= 0){
                return keepAliveTime - longestIdleDuration;//这里返回的值，可以让清理线程知道，下一次清理要多久以后
            }

            //如果运行到这里的话，代表longestIdleDuration = -1，连接池中为空
            cleanupRunning = false;
            return longestIdleDuration;
        }
    }

    public void putHttpConnection(HttpConnection httpConnection){
        //首先判断线程池有没有在执行
        if(!cleanupRunning){
            cleanupRunning = true;
            executor.execute(cleanupRunnable);
        }
        httpConnections.add(httpConnection);

    }

    /**
     * 根据服务器地址与端口，来获取可复用的连接
     * @param host
     * @param port
     * @return
     */
    public synchronized HttpConnection getHttpConnection(String host,int port){

        Iterator<HttpConnection> httpConnectionIterator = httpConnections.iterator();
        while(httpConnectionIterator.hasNext()){
            HttpConnection httpConnection = httpConnectionIterator.next();
            if(httpConnection.isSameAddress(host,port)){
                httpConnectionIterator.remove();
                return httpConnection;
            }
        }
        return null;
    }


}
