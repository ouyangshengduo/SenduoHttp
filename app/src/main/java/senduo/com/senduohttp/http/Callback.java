package senduo.com.senduohttp.http;

/**
 * *****************************************************************
 * * 文件作者：ouyangshengduo
 * * 创建时间：2018/5/5
 * * 文件描述：一个给使用者回调网络请求最终的访问结果的一个类
 * * 修改历史：2018/5/5 11:27*************************************
 **/

public interface Callback {

    void onFailure(Call call,Throwable throwable);

    void onResponse(Call call,Response response);
}
