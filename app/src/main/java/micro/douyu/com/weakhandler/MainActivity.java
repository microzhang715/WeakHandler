package micro.douyu.com.weakhandler;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);

                MainActivity.this.finish();
            }
        });

        weakHandler.sendEmptyMessage(1);
        weakHandler.postDelayed(sRunnable, 60 * 60 * 1000);
    }

    private Runnable sRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG, "收到消息");
        }
    };

    private WeakHandler weakHandler = new WeakHandler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d(TAG, "收到消息1");
                    break;
                case 2:
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //对于长时间才可能执行的代码，最好是在最后destroy的时候进行清理。
        //weakHandler由于是弱引用，不清理也是可以释放掉的
        weakHandler.removeCallbacks(sRunnable);
    }
}
