package com.xanderfehsenfeld.swearjar;


/**
 * created by Xander
 * much of this code is from http://stackoverflow.com/questions/14940657/android-speech-recognition-as-a-service-on-android-4-1-4-2/14950616#14950616
 * It is a service which is a "continuous" speech recognizer - not quite continuous because it pauses to analyze speech
 *
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SpeechToText extends Service
{
    private static final String TAG = "SpeechToText";
    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;


    /* send data back to MainActivity */
    ResultReceiver resultReceiver;

    /* keep speech recognizer going */
    CountDownTimer mTimer;

    /* keep track of number of swearwords*/
    int currentScore = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

        Log.d(TAG, "onCreate"); //$NON-NLS-1$



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$

        currentScore = intent.getIntExtra("score", 0);

        return START_STICKY;


    }



    /* IncomingHandler */
    protected class IncomingHandler extends Handler
    {
        private WeakReference<SpeechToText> mtarget;

        IncomingHandler(SpeechToText target)
        {
            mtarget = new WeakReference<SpeechToText>(target);
            Log.d(TAG, "IncomingHandler");
        }


        @Override
        public void handleMessage(Message msg)
        {
            final SpeechToText target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                         //turn off beep sound
                        if ( !mIsStreamSolo )
                        {
//                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
//                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
//                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
//                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub
            //Log.d(TAG, "Timer: " + millisUntilFinished );


        }

        @Override
        public void onFinish()
        {
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {
                Log.d(TAG, "error starting on timer finish: " + e.getMessage() );

            }
            //Log.d(TAG, "countdown finished"); //$NON-NLS-1$

        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }

        /* send end code to parent activity */
        Bundle bundle = new Bundle();
        //bundle.putString("end", "Timer Stopped....");
        resultReceiver.send(200, bundle);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");  //$NON-NLS-1$

        resultReceiver = intent.getParcelableExtra("receiver");

        /* send an initial amount */


        return mServerMessenger.getBinder();
    }

    /* send results out */
    private void sendResults( String result ){
        /* send info back to parent activity */
        Bundle bundle = new Bundle();
        bundle.putInt("score", currentScore);
        bundle.putStringArrayList("badwords", SpeechAnalyzer.getBadWords(result));
        bundle.putString("whatWasSaid", result);
        resultReceiver.send(2, bundle);
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {

        @Override
        public void onBeginningOfSpeech()
        {
            // speech input will be processed, so there is no need for count down anymore
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$


            /* added to make speech recognition "continuous"
                Not perfect because the speech recognizer is busy translating for a bit
             */
            //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

        }

        @Override
        public void onError(int error)
        {
            restartSpeechCycle();
            Log.d(TAG, "onError error: " + error); //$NON-NLS-1$

        }

        private void restartSpeechCycle(){
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "SpeechRecognizer.onEvent");
        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "SpeechRecognizer.onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();

            }
            Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$

            /* cancel the timer so it doesn't interrupt speech process */
//            Log.d("Timer", "onReadyForSpeech: Cancel Timer");
//            if(mTimer != null) {
//                mTimer.cancel();
//            }
        }

        @Override
        public void onResults(Bundle results)
        {



            Log.d(TAG, "onResults"); //$NON-NLS-1$

            //If the timer is available, cancel it so it doesn't interrupt our result processing
            if(mTimer != null){
                mTimer.cancel();
            }

            ArrayList<String> whatWasSaid = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, whatWasSaid.get(0));
            Toast t = Toast.makeText(SpeechToText.this, whatWasSaid.get(0), Toast.LENGTH_LONG);
            t.show();

            // update score and send results
            currentScore += SpeechAnalyzer.analyzeSpeech(whatWasSaid.get(0));
            sendResults(whatWasSaid.get(0));

            /* start listening again */
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }


}