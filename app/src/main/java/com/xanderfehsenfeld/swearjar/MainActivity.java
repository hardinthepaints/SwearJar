package com.xanderfehsenfeld.swearjar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.DelayQueue;


/** Created By Xander
 * Much of this code is taken from http://stackoverflow.com/questions/18039429/android-speech-recognition-continuous-service
 *
 *   This activity starts a service and receives results from it to put in a TextView
 */
public class MainActivity extends AppCompatActivity {

    public static final String SWEAR_COUNT = "SWEAR_COUNT";
    int swearCount = 5;

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

    /* intent for the result of clicking the notification (used later in makeNotification )*/
    Intent resultIntent = new Intent(this, MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* setup result reciever */
        resultReceiver = new MyResultReceiver(null);
        textView = (TextView)findViewById(R.id.results_field);
        header = (TextView)findViewById(R.id.header);


//        /* start and bind to service */
//        startSpeechService();
//        bindToService();

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

        /* click the button */
        findViewById(R.id.button).callOnClick();



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
                //runOnUiThread(new UpdateUI(resultData.getString("start")));
            }
            else if(resultCode == 200){
                //runOnUiThread(new UpdateUI(resultData.getString("end")));
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

        /* bind to the service (assumes service is already started) */
        bindToService();
    }


    @Override
    protected void onStop()
    {
        super.onStop();

        Log.d(TAG, "onStop");

        /* unbind from the service */
        unbindService();

        /* send out the notification when user navigates away from activity */
        makeNotification();

    }

    /* Bind the the speech recog service */
    private void bindToService(){
        Intent i = new Intent(this, SpeechToText.class);
        i.putExtra("receiver", resultReceiver);
        bindService(i, mServiceConnection, mBindFlag);
    }
    /* unBind the the speech recog service */
    private void unbindService(){
        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
    }

    /* stop and unbind with the service */
    private void stopSpeechService(){

        Intent service = new Intent(MainActivity.this, SpeechToText.class);
        MainActivity.this.stopService(service);
    }

    /* start the continuous speech service and bind to it */
    private void startSpeechService(){

        /* start the service */
        Intent service = new Intent(MainActivity.this, SpeechToText.class);
        /* send the reciever to the service */
        service.putExtra("receiver", resultReceiver);
        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;
        MainActivity.this.startService(service);

        /* bind to the service */
        //bindToService();

    }


    /* Notification */
    private void makeNotification(){

        /* create the notification builder */
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("SwearJar")
                        .setContentText( "Swears: " + swearCount );


        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        mBuilder.setContentIntent( resultPendingIntent );

        /* ISSUE THE NOTIFICATION */

        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }



    // Menu handling
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pay_up:
                Intent i = new Intent(this, CharityActivity.class);
                i.putExtra(SWEAR_COUNT, swearCount);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void buttonClick(View view){
//        Toast t = Toast.makeText(MainActivity.this, "ButtonClicked!", Toast.LENGTH_SHORT);
//        t.show();

        /* cycle everything */
        unbindService();
        stopSpeechService();
        startSpeechService();
        bindToService();
    }




}
