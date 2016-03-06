package com.example.voicetotext;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextToSpeech.OnInitListener {
    private String tmpRecode = "";
    // リクエストを識別するための変数宣言。適当な数字でよい for VTT
    private static final int REQUEST_CODE = 0;
    // for TTS
    private TextToSpeech tts;
    private static final String TAG = "Test the TTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // TTS オブジェクト生成
        tts = new TextToSpeech(this, this);

        Button ttsButton = (Button)findViewById(R.id.ttsButton);
        ttsButton.setOnClickListener(this);
    }

    @Override
    public void onInit(int status) {
        // TTS初期化
        if (TextToSpeech.SUCCESS == status) {
            Log.d(TAG, "initialized");
        } else {
            Log.e(TAG, "failed to initialize");
        }
    }

    @Override
    public void onClick(View v) {
        speechText();
    }

    private void shutDown(){
        if (null != tts) {
            // to release the resource of TextToSpeech
            tts.shutdown();
        }
    }

    private void speechText() {
        EditText editor = (EditText)findViewById(R.id.editText);
        editor.selectAll();

        String string = "";
        if (tmpRecode.equals("")){
            // EditTextからテキストを取得
            string = editor.getText().toString();
        }else {
            String tmpString = "";

            // VTTで得た文字列を1文字ずつ切り分けてセミコロンまで判定
            for (int i = 0; i < tmpRecode.length(); i++) {
                tmpString = String.valueOf(tmpRecode.charAt(i));

                if (tmpString.equals(";")) {
                    Log.d(TAG, string);
                    break;
                } else {
                    string += tmpString;
                }
            }
//            string = tmpRecode;
        }


        if (0 < string.length()) {
            if (tts.isSpeaking()) {
                // 読み上げ中なら止める
                tts.stop();
                return;
            }
            setSpeechRate(1.0f);
            setSpeechPitch(1.0f);

            // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null) に
            // KEY_PARAM_UTTERANCE_ID を HasMap で設定
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");

            // 読み上げ開始
            tts.speak(string, TextToSpeech.QUEUE_FLUSH, map);
            setTtsListener();

        }
    }

    // 読み上げのスピード
    private void setSpeechRate(float rate){
        if (null != tts) {
            tts.setSpeechRate(rate);
        }
    }

    // 読み上げのピッチ
    private void setSpeechPitch(float pitch){
        if (null != tts) {
            tts.setPitch(pitch);
        }
    }

    // 読み上げの始まりと終わりを取得
    private void setTtsListener(){
        // android version more than 15th
        if (Build.VERSION.SDK_INT >= 15)
        {
            int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener()
            {
                @Override
                public void onDone(String utteranceId)
                {
                    Log.d(TAG,"progress on Done " + utteranceId);
                }

                @Override
                public void onError(String utteranceId)
                {
                    Log.d(TAG,"progress on Error " + utteranceId);
                }

                @Override
                public void onStart(String utteranceId)
                {
                    Log.d(TAG,"progress on Start " + utteranceId);
                }

            });
            if (listenerResult != TextToSpeech.SUCCESS)
            {
                Log.e(TAG, "failed to add utterance progress listener");
            }
        }
        else
        {
            // less than 15th
            int listenerResult = tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener()
            {
                @Override
                public void onUtteranceCompleted(String utteranceId)
                {
                    Log.d(TAG,"progress on Completed " + utteranceId);
                }
            });

            if (listenerResult != TextToSpeech.SUCCESS)
            {
                Log.e(TAG, "failed to add utterance completed listener");
            }
        }

    }

    protected void onDestroy() {
        super.onDestroy();
        shutDown();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 画面から指が離れるイベントの場合のみ実行
        if (event.getAction() == MotionEvent.ACTION_UP) {
            try {
                // 音声認識プロンプトを立ち上げるインテント作成
                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                // 言語モデルをfree-form speech recognitionに設定
                // web search terms用のLANGUAGE_MODEL_WEB_SEARCHにすると検索画面になる
                intent.putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                // プロンプトに表示する文字を設定
                intent.putExtra(
                        RecognizerIntent.EXTRA_PROMPT,
                        "話してください");
                // インテント発行
                startActivityForResult(intent, REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                // エラー表示
                Toast.makeText(MainActivity.this,
                        "ActivityNotFoundException", Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    // startActivityForResultで起動したアクティビティが終了した時に呼び出される関数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 音声認識結果のとき
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // 結果文字列リストを取得
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            // 取得した文字列を結合
            String resultsString = "";
            for (int i = 0; i < results.size(); i++) {
                resultsString += results.get(i)+";";
            }
            // トーストを使って結果表示
            Toast.makeText(this, resultsString, Toast.LENGTH_LONG).show();

            // VTTとTTSの連携
            Log.d(TAG, "連携はいりまっっっっっっっ");
            Log.d(TAG, resultsString);
            tmpRecode = resultsString;
            speechText();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
