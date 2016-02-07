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

    @Override
    public void onCreate()
    {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

        Log.d(TAG, "onCreate"); //$NON-NLS-1$


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        /* get the result reciever which was sent when this services was started */
        resultReceiver = intent.getParcelableExtra("receiver");


        Log.d(TAG, "onStartCommand"); //$NON-NLS-1$

        return START_STICKY;


    }



    /* IncomingHandler */
    protected static class IncomingHandler extends Handler
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
                        // turn off beep sound
//                        if ( !mIsStreamSolo )
//                        {
//                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
//                            mIsStreamSolo = true;
//                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        //Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
//                    if (mIsStreamSolo)
//                    {
//                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
//                        mIsStreamSolo = false;
//                    }
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
        bundle.putString("end", "Timer Stopped....");
        resultReceiver.send(200, bundle);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");  //$NON-NLS-1$

        resultReceiver = intent.getParcelableExtra("receiver");

        return mServerMessenger.getBinder();
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
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

        }

        @Override
        public void onError(int error)
        {
            restartSpeechCycle();
            //Log.d(TAG, "error = " + error); //$NON-NLS-1$
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
        }

        @Override
        public void onResults(Bundle results)
        {
            Log.d(TAG, "onResults"); //$NON-NLS-1$
            ArrayList<String> whatWasSaid = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, whatWasSaid.get(0));
            Toast t = Toast.makeText(SpeechToText.this, whatWasSaid.get(0), Toast.LENGTH_LONG);
            t.show();

            Bundle bundle = new Bundle();
            bundle.putInt("result", SpeechAnalyzer.analyzeSpeech( whatWasSaid.get(0) ) );
            resultReceiver.send(2, bundle);



            //restartSpeechCycle();


        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }
}