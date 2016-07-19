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

import java.lang.ref.WeakReference;

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

        weakHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "消息到达");
            }
        }, 60 * 60 * 1000);

    }

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

}
