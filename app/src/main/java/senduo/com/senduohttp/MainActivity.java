package senduo.com.senduohttp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import senduo.com.senduohttp.http.Call;
import senduo.com.senduohttp.http.Callback;
import senduo.com.senduohttp.http.HttpClient;
import senduo.com.senduohttp.http.Request;
import senduo.com.senduohttp.http.RequestBody;
import senduo.com.senduohttp.http.Response;

public class MainActivity extends AppCompatActivity {

    HttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new HttpClient.Builder()
                .setRetryTimes(3)
                .build();
    }

    public void get(View view) {
        Request request = new Request.Builder()
                .setHttpUrl("http://www.kuaidi100.com/query?type=yuantong&postid=222222222")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e("响应体", response.getBody());
            }
        });
    }

    public void post(View view) {
        RequestBody body = new RequestBody()
                .add("key", "064a7778b8389441e30f91b8a60c9b23")
                .add("city", "深圳");


        Request request = new Request.Builder()
                .setHttpUrl("http://restapi.amap.com/v3/weather/weatherInfo")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e("响应体", response.getBody());
            }
        });
    }
}
