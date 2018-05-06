# **开篇废话** #

趁着周末两天的时间，跟着大神的脚步，把我们经常使用的网络框架OkHttp的源码好好跟了一下，初次观看，确实非常容易钻进去，搞得云里雾里，在大神的指导下，才勉强把整个逻辑走通。写这篇文章，也是希望自己脑袋里面，能对这个网络框架有一个整体的认识，了解它整体设计思想。

大概了解后，还是需要自己亲自动手，来手写当中的一些细节，加深自己理解，所以，接下来，我会给出OkHttp中设计的主线，以及模拟OkHttp，手写一个属于我们自己的网络框架。

废话就到此为止了，开始这次的学习之旅吧。。。


----------


# **技术详情** #

## **1. OkHttp 的主线调用** ##

关于OkHttp的使用，我这里就不说了，不清楚的可以，在简书里面搜索一下，应该有非常多文章来说明，我这里就大概说一下调用的主线流程。

    第一步：使用者拿到OkHttpClient对象，一般我们都会声明成全局的一个对象
    第二步：使用OkHttpClient对象来拿到一个RealCall对象
    第三步：用这个对象，把我们的Request请求（请求包含一些服务器信息）加入到它的一个调度器
           这个调度器是通过控制执行/等待队列的线程池来调度我们传进去的网络请求
    第四步：执行RealCall中网络请求，将请求结果返回给我们使用者，这一步中，OkHttp框架会帮我
           做很多优化处理，其中的责任链式的拦截器，就在这个步骤中实现

查看如下的图片，可能更清晰点，
![主线.png](https://upload-images.jianshu.io/upload_images/6522799-23032da284141ce9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

OkHttp中拦截器的整体调用逻辑，可以查看下面这张图，可以说非常清晰了，细节地方，自己可以去查看源码了：

![OkHttp拦截器.png](https://upload-images.jianshu.io/upload_images/6522799-1da833a69ad9f780.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

对于当中具体调用细节，我这里就不再讲述了，可以根据手写的简易OkHttp框架来对它们有一个整体的认识。

## **2. 模拟OkHttp,手写简易Http框架** ##

对OkHttp的设计思路有一个整体的认识后，自己手写一个简易版的Http框架，加深一下对OkHttp源码的认识。手写Http框架，当中能够学到以下知识点：

      1.对于http协议能够有熟悉的认识
      2.线程池的项目实践
      3.建造者模式的项目实践
      4.socket层的字节流处理
      5.责任链模式的项目实践，熟悉OkHttp中的责任链模式的拦截器


首先，根据使用者习惯，完成HttpClient的功能（建造者模式）:

    public class HttpClient {

	    //设置调度器
	    private Dispather dispather;
	
		//声明拦截器
	    private List<Interceptor> interceptors;
	
		//尝试次数
	    private int retryTimes;
	
		//连接池
	    private ConnectionPool connectionPool;
	
	    public int getRetryTimes() {
	        return retryTimes;
	    }
	
	    public Dispather getDispather() {
	        return dispather;
	    }
	
	    public List<Interceptor> getInterceptors() {
	        return interceptors;
	    }
	
	    public ConnectionPool getConnectionPool() {
	        return connectionPool;
	    }
	
	    /**
	     * 构造方法
	     */
	    public HttpClient(Builder builder){
	        this.dispather = builder.dispather;
	        this.interceptors = builder.interceptors;
	        this.retryTimes = builder.retryTimes;
	        this.connectionPool = builder.connectionPool;
	    }
	
	    /**
	     * 生成一个网络请求Call对象实例
	     * @param request
	     * @return
	     */
	    public Call newCall(Request request){
	        return new Call(this,request);
	    }
	
	    //TODO 建造对象
	    public static final class Builder{
	
	        Dispather dispather;
	        List<Interceptor> interceptors = new ArrayList<>();
	        int retryTimes;
	        ConnectionPool connectionPool;
	
	        public Builder addInterceptors(Interceptor interceptor){
	            interceptors.add(interceptor);
	            return this;
	        }
	
	        public Builder setDispather(Dispather dispather){
	            this.dispather = dispather;
	            return this;
	        }
	
	        public Builder setRetryTimes(int retryTimes){
	            this.retryTimes = retryTimes;
	            return this;
	        }
	
	        public Builder setConnectionPool(ConnectionPool connectionPool){
	            this.connectionPool = connectionPool;
	            return this;
	        }
	
	
	        public HttpClient build(){
	
	            if(null == dispather){
	                dispather = new Dispather();
	            }
	
	            if(null == connectionPool){
	                connectionPool = new ConnectionPool();
	            }
	            return new HttpClient(this);
	        }
	    }
	}
        

使用者能够通过设置失败重试次数，自定义拦截器构造一个httpClient对象

得到HttpClient对象后，就能拿到Call对象了，以下是Call里面的实现：

    
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


Call类中的AsyncCall线程，是请求真正开始的地方，使用者调用enqueue的时候，只是把这个AsyncCall线程添加到调度器Dispather的线程池，其中，Dispacher中的实现如下：

    
	public class Dispather {

	    //TODO 最多同时请求的数量
	    private int maxRequests;
	
	    //TODO 同一个host最多允许请求的数量
	    private int maxRequestPreHost;
	
	    public Dispather(){
	        this(64,5);
	    }
	
	    public Dispather(int maxRequests,int maxRequestPreHost){
	        this.maxRequestPreHost = maxRequestPreHost;
	        this.maxRequests = maxRequests;
	    }
	
	    private ExecutorService executorService;//声明一个线程池
	
	    //TODO 等待双端队列，双端比较适合增加与删除
	    private final Deque<Call.AsyncCall> readyAsyncCalls = new ArrayDeque<>();
	
	    //TODO 运行中的双端队列
	    private final Deque<Call.AsyncCall> runningAsyncCalls = new ArrayDeque<>();
	
	
	    /**
	     * 线程池的初始化
	     * @return
	     */
	    public synchronized ExecutorService initExecutorService(){
	
	        if(null == executorService){
	            //这里只是给这个线程起一个名字
	            ThreadFactory threadFactory = new ThreadFactory() {
	                @Override
	                public Thread newThread(@NonNull Runnable runnable) {
	                    Thread thread = new Thread(runnable,"Http Client Thread");
	                    return thread;
	                }
	            };
	            //这里按照OkHttp的线程池样式来创建，单个线程在闲置的时候保留60秒
	            executorService = new ThreadPoolExecutor(0,Integer.MAX_VALUE,60L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),threadFactory);
	        }
	        return executorService;
	    }
	
	    /**
	     * 将线程加入到线程池队列
	     * @param asyncCall
	     */
	    public void enqueue(Call.AsyncCall asyncCall){
	        Log.e("Dispatcher", "同时有:" + runningAsyncCalls.size());
	        Log.e("Dispatcher", "host同时有:" + getRunningPreHostCount(asyncCall));
	        //TODO 首先判断正在运行的队列是否已经满了，而且同一个host请求的是否已经超过规定的数量
	        if(runningAsyncCalls.size() < maxRequests && getRunningPreHostCount(asyncCall) < maxRequestPreHost){
	            Log.e("Dispatcher", "提交执行");
	            runningAsyncCalls.add(asyncCall);
	            initExecutorService().execute(asyncCall);
	        }else{
	            //不满足条件，就加到等待队列
	            Log.e("Dispatcher", "等待执行");
	            readyAsyncCalls.add(asyncCall);
	        }
	    }
	
	    /**
	     * 获取同一host在正在运行队列中的数量
	     * @param asyncCall
	     * @return
	     */
	    private int getRunningPreHostCount(Call.AsyncCall asyncCall) {
	        int count = 0;
	        for(Call.AsyncCall runningAsyncCall : runningAsyncCalls){
	            if(runningAsyncCall.getHost().equals(asyncCall.getHost())){
	                count ++;
	            }
	        }
	        return count;
	    }
	
	    public void finished(Call.AsyncCall asyncCall){
	
	        synchronized (this){
	            runningAsyncCalls.remove(asyncCall);
	
	            checkReadyCalls();
	        }
	
	    }
	
	    /**
	     * 检查是否可以运行等待中的请求
	     */
	    private void checkReadyCalls() {
	        //达到了同时请求最大数
	        if(runningAsyncCalls.size() >= maxRequests){
	            return;
	        }
	        //没有等待执行的任务
	        if(readyAsyncCalls.isEmpty()){
	            return;
	        }
	
	        Iterator<Call.AsyncCall> asyncCallIterator = readyAsyncCalls.iterator();
	        while(asyncCallIterator.hasNext()){
	            Call.AsyncCall asyncCall = asyncCallIterator.next();
	            //如果获得的等待执行的任务 执行后 小于host相同最大允许数 就可以去执行
	            if(getRunningPreHostCount(asyncCall) < maxRequestPreHost){
	                asyncCallIterator.remove();
	                runningAsyncCalls.add(asyncCall);
	                executorService.execute(asyncCall);
	            }
	
	            if(runningAsyncCalls.size() >= maxRequests){
	                return;
	            }
	        }
	
	    }
	}


调度器中只是负责把添加进来的请求进行按序执行管理，真正执行，还是在AsyncCall线程中run方法，其中的责任链式的拦截器，也在这里面进行添加，执行。
我这里自己手写的拦截器，没有像OkHttp那么全而细，知道它的设计思想后，自己只是手动实现一个简单的责任链拦截器，其中包括
    
    1.重试拦截器
	2.Http头拦截器
	3.选择有效socket连接的拦截器
	4.socket通信拦截器

这个顺序是不能随意调换的，就跟工厂里面的流水线一样，一步一步往下走的。
首先，实现的是重试拦截器

    
	public class RetryInterceptor implements Interceptor {
	    @Override
	    public Response intercept(InterceptorChain interceptorChain) throws IOException {
	        Log.e("interceprot", "重试拦截器....");
	        Call call = interceptorChain.call;
	        IOException ioException = null;
	
	        for(int i = 0 ; i < call.getHttpClient().getRetryTimes(); i ++){
	
	            if(call.isCanceled()){
	                throw new IOException("this task had canceled");
	            }
	
	            try {
	                Response response = interceptorChain.proceed();
	                return response;
	            }catch (IOException e){
	                ioException = e;
	            }
	        }
	        throw ioException;
	    }
	}

然后是Http头处理的拦截器

    
	public class HeadersInterceptor implements Interceptor {
	    @Override
	    public Response intercept(InterceptorChain interceptorChain) throws IOException {
	
	        Log.e("interceprot","Http头拦截器....");
	
	        Request request = interceptorChain.call.getRequest();
	        Map<String,String> headers = request.getHeaders();
	        if(!headers.containsKey(HttpCodec.HEAD_HOST)){
	            headers.put(HttpCodec.HEAD_HOST,request.getHttpUrl().getHost());
	        }
	        if(!headers.containsKey(HttpCodec.HEAD_CONNECTION)) {
	            headers.put(HttpCodec.HEAD_CONNECTION, HttpCodec.HEAD_VALUE_KEEP_ALIVE);
	        }
	
	        if(null != request.getRequestBody()){
	            String contentType = request.getRequestBody().getContentType();
	            if(null != contentType){
	                headers.put(HttpCodec.HEAD_CONTENT_TYPE,contentType);
	            }
	
	            long contentLength = request.getRequestBody().getContentLength();
	
	            if(-1 != contentLength){
	                headers.put(HttpCodec.HEAD_CONTENT_LENGTH,Long.toString(contentLength));
	            }
	        }
	        return interceptorChain.proceed();
	    }
	}

接着是选择可用socket连接的拦截器

	
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


把请求的配置信息都配置好后，最后，交给socket去通信，去解析，就是socket通信拦截器了：

    public class CallServiceInterceptor implements Interceptor {
	    @Override
	    public Response intercept(InterceptorChain interceptorChain) throws IOException {
	
	        Log.e("interceptor", "通信拦截器");
	
	        HttpConnection httpConnection = interceptorChain.httpConnection;
	        HttpCodec httpCodec = new HttpCodec();
	        InputStream inputStream = httpConnection.call(httpCodec);
	
	        //获取服务器返回的响应行 HTTP/1.1 200 OK\r\n
	        String statusLine = httpCodec.readLine(inputStream);
	
	        //获取服务器返回的响应头
	        Map<String,String> headers = httpCodec.readHeaders(inputStream);
	
	        //根据Content-Length或者Transfer-Encoding(分块)计算响应体的长度
	        int contentLength = -1;
	        if(headers.containsKey(HttpCodec.HEAD_CONTENT_LENGTH)){
	            contentLength = Integer.valueOf(headers.get(HttpCodec.HEAD_CONTENT_LENGTH));
	        }
	        //是否为分块编码
	        boolean isChunked = false;
	        if(headers.containsKey(HttpCodec.HEAD_TRANSFER_ENCODING)){
	            isChunked = headers.get(HttpCodec.HEAD_TRANSFER_ENCODING).equalsIgnoreCase(HttpCodec.HEAD_VALUE_CHUNKED);
	        }
	
	        //获取服务器响应体
	
	        String body = null;
	        if(contentLength > 0){
	            byte[] bodyBytes = httpCodec.readBytes(inputStream,contentLength);
	            body = new String(bodyBytes,HttpCodec.ENCODE);
	        }else if(isChunked){
	            body = httpCodec.readChunked(inputStream,contentLength);
	        }
	
	        // HTTP/1.1 200 OK\r\n status[0] = "HTTP/1.1",status[1] = "200",status[2] = "OK\r\n"
	        String[] status = statusLine.split(" ");
	
	        //根据响应头中的Connection的值，来判断是否能够复用连接
	        boolean isKeepAlive = false;
	        if(headers.containsKey(HttpCodec.HEAD_CONNECTION)){
	            isKeepAlive = headers.get(HttpCodec.HEAD_CONNECTION).equalsIgnoreCase(HttpCodec.HEAD_VALUE_KEEP_ALIVE);
	        }
	
	        //更新此请求的最新使用时间，作用于线程池的清理工作
	        httpConnection.updateLastUseTime();
	
	        return new Response(Integer.valueOf(status[1]),contentLength,headers,body,isKeepAlive);
	    }
	}


经过以上四个拦截器后，使用者就能够正常使用http或者https的请求了。这里面还有一些额外处理的类，我这里就没有给出来了，具体的可以去我的github上查看项目源码，注释写了蛮多，应该很好理解。

关于此项目的源代码，在文章最后，会提供github地址，欢迎star


# **干货总结** #

阅读框架源码，说真的，确实是一件非常蛋疼的事，但是为了提升自己，却不得不去啃这个硬骨头，学习他人优秀的设计思想为己所用。只有积累的足够多方法之后，我们才能真正得去创造。

对于我们项目中框架的使用，会引入很多不一样的框架，比如retrofit（okhttp）,glide,greenDao数据库框架,arouter路由框架，rxjava,butterknife,以及一些其他的第三方自定义控件等，就会不知不觉是我们的项目越发的庞大，而且比较难以维护，所以，建议我们自己项目，能够尽量自己手写相关的框架，而不要直接引用别人的（我个人的愿景，不喜勿喷）。

希望通过此文章以及源码，能够学习到以下知识点，也不枉我码了那么多字：

      1.对于http协议能够有熟悉的认识
      2.线程池的项目实践
      3.建造者模式的项目实践
      4.socket层的字节流处理
      5.责任链模式的项目实践，熟悉OkHttp中的责任链模式的拦截器


接下来的日子，希望能够对数据库框架，路由框架，图片加载框架，换肤框架，热更新框架等框架，进行熟悉，然后给出自己手写的简易版本，提升自身对于这些框架的认识。


以下是此项目的源码：

[项目的简书地址](https://www.jianshu.com/p/36347c7dfa65)


