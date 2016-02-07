package com.xanderfehsenfeld.swearjar;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.DelayQueue;


/** Created By Xander
 * Much of this code is taken from http://stackoverflow.com/questions/18039429/android-speech-recognition-continuous-service
 *
 *   This activity starts a service and recieves results from it to put in a TextView
 */
public class MainActivity extends Activity {

    int swearCount = 0;

    private static final String TAG = "MainActivity";

    /* for binding with service */
    private int mBindFlag;
    private Messenger mServiceMessenger;


    /* A service connection to connect this Activity with the speech recognition service */
    final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public static final boolean DEBUG = true;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            if (DEBUG) {Log.d(TAG, "onServiceConnected");} //$NON-NLS-1$

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = SpeechToText.MSG_RECOGNIZER_START_LISTENING;

            try
            {
                //mServerMessenger.send(msg);
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            if (DEBUG) {Log.d(TAG, "onServiceDisconnected");} //$NON-NLS-1$
            mServiceMessenger = null;
        }

    }; // mServiceConnection


    /* Make a way to get results from the activity */
    MyResultReceiver resultReceiver;
    TextView textView;
    TextView header;

    /* animate the swearword falling */
    Animation animationFalling;
    TextView badword;
    Queue<TextView> fallingWords;

    /* match text views to their animations */
    HashMap<Animation, TextView> animationMap;

    Queue<String> badWordQeue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* start the service */
        Intent service = new Intent(MainActivity.this, SpeechToText.class);
        /* send the reciever to the service */
        service.putExtra("receiver", resultReceiver );
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;
        MainActivity.this.startService(service);


        /* setup result reciever */
        resultReceiver = new MyResultReceiver(null);
        textView = (TextView)findViewById(R.id.results_field);
        header = (TextView)findViewById(R.id.header);


                //badword = getNewSwearView();

        fallingWords = new LinkedList<>();
        animationMap = new HashMap<>();
        TextView tv;
        for (int i = 0; i < 5; i ++){
            tv = getNewSwearView();
            animationFalling = AnimationUtils.loadAnimation(this, R.anim.falling);
            animationFalling.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    TextView animatedView = animationMap.get(animation);
                    //animatedView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    TextView animatedView = animationMap.get(animation);
                    fallingWords.add(animatedView);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            fallingWords.add(tv);
            animationMap.put(animationFalling, tv);

        }

        badWordQeue = new LinkedList<>();

    }

    /* inflate a new text view to hold the swearword, add it to the outer framlayout, and rtn */
    private TextView getNewSwearView(){
        FrameLayout fl = (FrameLayout) findViewById(R.id.innerLayout);
        TextView output = (TextView)((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.inflateable_views, null, false);
        fl.addView(output);
        output.setGravity(Gravity.CENTER_HORIZONTAL);
        return output;

    }

    /* a runnable to update ui */
    class UpdateUI implements Runnable
    {
        String updateString;

        public UpdateUI(String updateString) {
            this.updateString = updateString;
        }
        public void run() {
            textView.setText(updateString);

            /* make the bad words rain */
            while( !badWordQeue.isEmpty() ) {
                TextView toAnimate = fallingWords.poll();
                toAnimate.setText(badWordQeue.poll());
                toAnimate.startAnimation(animationFalling);


            }

        }
    }

    /* update ui with results
     */
    class MyResultReceiver extends ResultReceiver
    {
        public MyResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if(resultCode == 100){
                runOnUiThread(new UpdateUI(resultData.getString("start")));
            }
            else if(resultCode == 200){
                runOnUiThread(new UpdateUI(resultData.getString("end")));
            }
            else{
                swearCount += resultData.getInt("score");
                badWordQeue.addAll( resultData.getStringArrayList("badwords") );
                runOnUiThread(new UpdateUI(swearCount + ""));
            }
        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        Intent i = new Intent(this, SpeechToText.class);
        i.putExtra("receiver", resultReceiver);
        bindService(i, mServiceConnection, mBindFlag);
    }


    @Override
    protected void onStop()
    {
        super.onStop();

        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
    }





}
