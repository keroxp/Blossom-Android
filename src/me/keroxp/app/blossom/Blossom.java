/**
 * Blossom
 * A Japanese text input system for Android
 * Developed by Yusuke Sakurai as Keio Univ SFC Masui Lab.
 */

package me.keroxp.app.blossom;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import me.keroxp.app.blossom.BLKeyboard.BLKey;

//import net.sf.json.*;
import org.json.*;

import android.R.bool;
import android.R.string;
import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.StaticLayout;
import android.text.method.MetaKeyKeyListener;

import android.os.Bundle;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SimpleAdapter.ViewBinder;
import android.util.Log;

public final class Blossom extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
    View.OnTouchListener, BLKeyboard.BLKey.OnKeyActionListener, OnGestureListener {

  // おまじないオブジェクト
  private InputMethodManager InputMethodManager;
  private String WordSeparators;

  // ジェスチャディテクタ
  private GestureDetector gestureDetector;
  // バイブレータ
  private Vibrator vibrator;

  // Keyboardオブジェクト
  private BLKeyboard mainKeyboard; // メイン
  private BLKeyboard currentKeyboard; // 現在のKeyboard

  // KeyboardViewオブジェクト
  private BLKeyboardView keyboardView;
  // CandidateViewオブジェクト
  private BLCandidateLayout candidateLayout;
  private ScrollView candidateScrollView;

  // なんだろこれ
  private int mLastDisplayWidth;

  // バッファ
  private StringBuilder originalBuffer = new StringBuilder(); // 生バッファ(あiueo）
  private StringBuilder composedBuffer = new StringBuilder(); // 変換後バッファ(あいうえお）
  private StringBuilder romeBuffer = new StringBuilder(); // (i.e i）

  // 辞書
  private JSONObject piecesDictionary; // ピース対応表
  private JSONObject romeDictionary; // ローマ字変換
  private JSONObject labelDictionary; // ラベル
  private JSONObject fullHalfDictionary; // 半角全角変換
  private JSONObject smallDictionary; // 大文字小文字変換

  final int InputModeEnglish = 0;
  final int InputModeRomeKana = 1;

  // 現在扱っているイベントとそれに関係するオブジェクト
  MotionEvent currentMotionEvent; // モーションイベント
  int currentKeyCode = -1;
  int currentFlickDirection = -1; // フリック方向
  int currentInputMode = 0; // 入力モード
  JSONArray currentPiecesArray; // ピース

  // BLKey currentKey; // キー

  /**
   * Main initialization of the input method component. Be sure to call to super
   * class.
   */

  @Override
  public void onCreate() {
    super.onCreate();
    // こいつが何なのかは分からない
    InputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    // こいつもよく分からない
    WordSeparators = getResources().getString(R.string.word_separators);
    // ジェスチャディテクタを初期化
    gestureDetector = new GestureDetector(this, this);
    // gestureDetector.setOnDoubleTapListener(this);
    // バイブレータを取得
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    // 辞書を初期化
    try {
      piecesDictionary = getDictionary(R.raw.pieces);
      romeDictionary = getDictionary(R.raw.romakana);
      labelDictionary = getDictionary(R.raw.keylabel);
      fullHalfDictionary = getDictionary(R.raw.fullhalf);
      smallDictionary = getDictionary(R.raw.small);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /*
   * res/rawのjsonファイルからJSONObectを返すメソッド
   */
  private JSONObject getDictionary(int res) throws IOException, JSONException {
    BufferedReader bufferedReader = null;
    try {
      InputStream inStream = getResources().openRawResource(res);
      BufferedInputStream bufferedStream = new BufferedInputStream(inStream);
      InputStreamReader reader = new InputStreamReader(bufferedStream);
      bufferedReader = new BufferedReader(reader);
      StringBuilder builder = new StringBuilder();
      String line = bufferedReader.readLine();
      while (line != null) {
        builder.append(line);
        line = bufferedReader.readLine();
      }
      return new JSONObject(builder.toString());
    } finally {
      if (bufferedReader != null) {
        bufferedReader.close();
      }
    }
  }

  /**
   * Helper to determine if a given character code is alphabetic.
   */
  private boolean isAlphabet(int code) {
    if (Character.isLetter(code)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Helper to send a key down / key up pair to the current editor.
   */
  private void keyDownUp(int keyEventCode) {
    getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
    getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
  }

  /**
   * Helper to send a character to the editor as raw key events.
   */
  private void sendKey(int keyCode) {
    switch (keyCode) {
    case BLKeyboard.enterKey:
      keyDownUp(KeyEvent.KEYCODE_ENTER);
      break;
    default:
      if (keyCode >= '0' && keyCode <= '9') {
        keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
      } else {
        getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
      }
      break;
    }
  }

  /**
   * This is the point where you can do all of your UI initialization. It is
   * called after creation and any configuration change.
   */
  @Override
  public void onInitializeInterface() {
    if (mainKeyboard != null) {
      // Configuration changes can happen after the keyboard gets
      // recreated,
      // so we need to be able to re-build the keyboards if the available
      // space has changed.
      int displayWidth = getMaxWidth();
      if (displayWidth == mLastDisplayWidth)
        return;
      mLastDisplayWidth = displayWidth;
    }

    // メインキーボードを作成
    mainKeyboard = new BLKeyboard(this, R.xml.qwerty);
    // デリゲートを設定
    for (Key key : mainKeyboard.getKeys()) {
      BLKey bKey = (BLKey) key;
      bKey.setOnKeyActionListener(this);
    }

  }

  /**
   * Called by the framework when your view for showing candidates needs to be
   * generated, like {@link #onCreateInputView}.
   */
  @Override
  public View onCreateCandidatesView() {
    // 候補ビューを作成
    return null;
  }

  /**
   * This is the main point where we do our initialization of the input method
   * to begin operating on an application. At this point we have been bound to
   * the client, and are now receiving all of the detailed information about the
   * target of our edits.
   */
  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    // バッファを初期化
    originalBuffer.setLength(0);
    composedBuffer.setLength(0);
    romeBuffer.setLength(0);
  }

  /**
   * Called by the framework when your view for creating input needs to be
   * generated. This will be called the first time your input method is
   * displayed, and every time it needs to be re-created such as due to a
   * configuration change.
   */
  @Override
  public View onCreateInputView() {
    // コンテナ
    LinearLayout linearLayout = new LinearLayout(this);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    linearLayout.setLayoutParams(new LayoutParams(-2,-2));
    // keyboard実体
    keyboardView = (BLKeyboardView)getLayoutInflater().inflate(R.layout.input, null);     
    // 候補ビュー    
    candidateLayout = (BLCandidateLayout)getLayoutInflater().inflate(R.layout.candidate, null);
    candidateLayout.setService(this);
    candidateLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 70));    
    //scrollView.addView(candidateLayout);
    // KeyboardViewのイベントリスナをこのクラスに
    keyboardView.setOnKeyboardActionListener(this);
    keyboardView.setOnTouchListener(this);
    // KeyboardViewにKeyboardをアサイン。
    keyboardView.setKeyboard(mainKeyboard);
    // Keyboardのプレビューをオフに
    keyboardView.setPreviewEnabled(false);
    // サブビューに追加
    linearLayout.addView(candidateLayout,0);
    linearLayout.addView(keyboardView,1);
    return linearLayout;
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    // Apply the selected keyboard to the input view.
    // keyboardView.setKeyboard(currentKeyboard);
    keyboardView.closing();
    // コメントアウトしておかないと落ちる 2.x系
    // final InputMethodSubtype subtype =
    // mInputMethodManager.getCurrentInputMethodSubtype();
    // keyboardView.setSubtypeOnSpaceKey(subtype);
  }

  // IMEによる入力が終わったときに呼ばれる。
  @Override
  public void onFinishInput() {
    super.onFinishInput();

    // バッファをクリア
    originalBuffer.setLength(0);
    romeBuffer.setLength(0);
    composedBuffer.setLength(0);

    // We only hide the candidates window when finishing input on
    // a particular editor, to avoid _ping the underlying application
    // up and down if the user is entering text into the bottom of
    // its window.
    setCandidatesViewShown(false);

    if (keyboardView != null) {
      keyboardView.closing();
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
    Log.d("Blossom.onKeyDown", "keyCode : " + keyCode + " KeyEvent : " + keyEvent);
    return super.onKeyDown(keyCode, keyEvent);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    Log.d("Blossom.onKeyUp", "keyCod : " + keyCode + " KeyEvent : " + keyEvent);
    return super.onKeyUp(keyCode, keyEvent);
  }

  // Keyが押されたら必ず呼ばれる。KeyがRepeatableの場合連続して呼ばれる
  public void onKey(int primaryCode, int[] keyCodes) {
    // TODO Auto-generated method stub
    Log.d("BLossom.onKey", "onkey");
    switch (primaryCode) {
    case BLKeyboard.deleteKey:
      handleBackspace();
      break;
    case BLKeyboard.spaceKey:
      handleSpace();
      break;
    case BLKeyboard.enterKey:
      handleEnter();
      break;
    default:
      break;
    }
  }

  // Keyが押されたとき、最初に一度だけ呼ばれる。Keyが連続された場合は呼ばれない。順番的には onKeyの前に呼ばれる。
  public void onPress(int primaryCode) {
    // 使わない
    Log.v("onPress", "onPress");
    currentKeyCode = primaryCode;
    // バイブを鳴らす
    vibrator.vibrate(50);
    // キーを保存
    // 対応するピースを取得
    JSONArray piecesArray;
    try {
      piecesArray = piecesDictionary.getJSONArray(String.valueOf(primaryCode));
      currentPiecesArray = piecesArray;
      keyboardView.setPiecesArray(piecesArray);
      Log.d("Blossom.onPress", piecesArray.toString());
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // キャラクタなら
    if (primaryCode < 300) {
      if (currentInputMode == InputModeEnglish) { // 英字入力中
        // 未確定状態の英文字を入力
        String s;
        try {
          s = labelDictionary.getString(String.valueOf(primaryCode));
          if (s != null) {
            if (s.length() > 0) {
              getCurrentInputConnection().setComposingText(s, 1);
            }
          }
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }
  private int candidateIndex = 0;
  // Keyが離されたら呼ばれる。Keyが連続された場合は呼ばれない。順番的には onKeyの後に呼ばれる。
  public void onRelease(int primaryCode) {
    // 使わない
    Log.v("onRelease", "onRelease");
    // フリック状態なら仮名文字を
    if (currentPiecesArray != null && currentFlickDirection != -1) {
      // ローマ字入力モードへ
      currentInputMode = InputModeRomeKana;
      String s;
      try {
        s = currentPiecesArray.getString(currentFlickDirection);
        composedBuffer.append(s);
        getCurrentInputConnection().setComposingText(composedBuffer, 1);
        updateCandidate(candidateIndex);
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    } else {
      if (primaryCode < 300) {
        if (currentInputMode == InputModeEnglish) {
          // 英字を確定
          getCurrentInputConnection().finishComposingText();
        } else if (currentInputMode == InputModeRomeKana) {
          // ローマ字変換
          if (currentInputMode == InputModeRomeKana) { // ローマ字
            String converted = null;
            String label = null;
            try {
              label = labelDictionary.getString(String.valueOf(primaryCode));
              converted = romeDictionary.getString(romeBuffer.toString() + label);
            } catch (JSONException e) {
              // TODO Auto-generated catch block
              // e.printStackTrace();
            }
            if (converted != null) {
              // ローマバッファに対応するかながあれば変換
              int cl = composedBuffer.length();
              int rl = romeBuffer.length();
              // ローマバッファ分を削除
              composedBuffer.delete((cl - 1) - (rl - 1), cl);
              // 変換文字を追加
              composedBuffer.append(converted);
              // ローマバッファを空に
              romeBuffer.setLength(0);
            } else {
              // なければローマ字バッファに追加して入力を継続
              romeBuffer.append(label);
              composedBuffer.append(label);
              if (romeBuffer.length() > 3) {
                // ホントはここでローマ字変換の可能性がないことをトップダウンで確認しなくてはいけない
                romeBuffer.setLength(0);
              }
            }
            getCurrentInputConnection().setComposingText(composedBuffer, 1);
            updateCandidate(candidateIndex);
            candidateIndex++;
          }
        }
      }
    }
    finishKeyInput();
  }

  public void keyDidPress(BLKey key) {
    // Log.v("keyDidPress", "key did press : " + key.label);
  }

  public void keyDidRelease(BLKey key, Boolean inside) {
    // Log.d("BLKeyboard.OnKeyActionListener", "key did Release : " +
    // String.valueOf(key.label));
  }

  // KeybaordView上のタッチイベントをすべてここでバインドする
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      currentMotionEvent = event;
    }
    // あとの処理はGestureDetectorに任せる
    return gestureDetector.onTouchEvent(event);
  }

  // 共通の完了処理はここに書く
  private void finishKeyInput() {
    currentPiecesArray = null;
    currentFlickDirection = -1;
    currentKeyCode = -1;
    currentMotionEvent = null;
    keyboardView.dismissPopupWindow();
  }
  
  private void finishComposing(){
    composedBuffer.setLength(0);
    romeBuffer.setLength(0);
    currentInputMode = InputModeEnglish;
  }

  public boolean onDown(MotionEvent e) {
    // Log.v("onDown", "on down");
    // バインドしてはいけない
    return false;
  }

  // 一定の距離をドラッグして離した場合の処理
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    // Log.v("onFling", "onFling");
    return false;
  }

  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    Log.v("onScroll", "scroll");
    int THRESHOLD = 2;
    if (Math.abs(e2.getX() - e1.getX()) > THRESHOLD) {
      if (keyboardView != null && keyboardView.getPopupWindow() != null && keyboardView.getFlowerLayout() != null) {
        // PopupWindowを表示
        if (!keyboardView.getPopupWindow().isShowing()) {
          keyboardView.showPopupWindow(0, -keyboardView.getMeasuredHeight());
        }
        // 方向を検知
        int dir = getDirection(e2.getRawX(), e2.getRawY(), e1.getRawX(), e1.getRawY());
        // 方向が代わったら指定方向をハイライト
        if (dir != currentFlickDirection) {
          currentFlickDirection = dir;
          keyboardView.getFlowerLayout().hilightPiece(dir);
          vibrator.vibrate(50);
          String s;
          // ハイライトに合わせてcomposedを変化
          try {
            // cpaがNullになっている場合があるの
            if (currentPiecesArray != null) {
              s = currentPiecesArray.getString(dir);
              getCurrentInputConnection().setComposingText(composedBuffer + s, 1);
            }
          } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        return true;
      }      
    }
    return false;
  }

  // MotionEventから方向を取得するメソッド

  final int BLDirectionUp = 0;
  final int BLDirectionUpRight = 1;
  final int BLDirectionDownRight = 2;
  final int BLDirectionDownLeft = 3;
  final int BLDirectionUpLeft = 4;

  private int getDirection(float f, float g, float h, float i) {
    float dx = f - h;
    float dy = g - i;
    double angle = -Math.atan2(dy, dx);
    double PI = Math.PI;

    if (angle < 0)
      angle += PI * 2;

    if ((0 <= angle && angle < PI * 3 / 10) || (PI * 19 / 10 <= angle && angle <= PI * 2)) {
      return BLDirectionUpRight;      // 右上
    } else if (PI * 3 / 10 <= angle && angle < PI * 7 / 10) {    
      return BLDirectionUp;  // 上
    } else if (PI * 7 / 10 <= angle && angle < PI * 11 / 10) {
      return BLDirectionUpLeft;  // 左上
    } else if (PI * 11 / 10 <= angle && angle < PI * 15 / 10) {     
      return BLDirectionDownLeft; // 左下
    } else if (PI * 15 / 10 <= angle && angle < PI * 19 / 10) {      
      return BLDirectionDownRight; // 右下
    } else {
      Log.d("getDirction", "invalid angel " + String.valueOf(angle));
    }
    return -1;
  }

  public void onLongPress(MotionEvent e) {
    Log.v("onLongPress", "longpress");
    if (currentInputMode == InputModeEnglish && currentKeyCode < 300) {
      // Composing中のアルファベットを大文字に
      String s;
      try {
        s = labelDictionary.getString(String.valueOf(currentKeyCode));
        getCurrentInputConnection().setComposingText(s.toUpperCase(), 1);
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
  }
  
  /*
   * 候補ビューの更新
   */    
  static final String [] cand1 = {"ほげ","ふが","ばー","もぎゅ","ねが","ぱふ","ふむ","しにょん","うにょん","もにゅん"};
  static final String [] cand2 = {"つぼみ","えりか","いつき","ゆり","らぶ","みき","いのり","せつな","みゆき","あかね","なお","やよい","なお","れいか"};
  static final String [] EMPTY_STRINGS = {};
    
  private void updateCandidate(int i){    
    if (composedBuffer.length() > 0) {
      candidateLayout.setCandidateStrings((i%2 == 0) ? cand1 : cand2);
    }else{
      candidateLayout.setCandidateStrings(EMPTY_STRINGS);      
    }
    candidateIndex++;    
  }
  
  public void candidateSelected(String s){
    Log.v("candidateSelected", s);
    getCurrentInputConnection().commitText(s, 1);
    candidateLayout.setCandidateStrings(EMPTY_STRINGS);
    finishComposing();
    finishKeyInput();
  }

  /*
   * バックスペースキー
   */
  private void handleBackspace() {
    if (currentInputMode == InputModeRomeKana) {
      // ローマバッファをトリミング
      final int romeLength = romeBuffer.length();
      if (romeLength > 1) {
        romeBuffer.delete(romeLength - 1, romeLength);
      } else if (romeLength > 0) {
        romeBuffer.setLength(0);
      }
      // composedBufferをトリミング
      final int length = composedBuffer.length();
      if (length > 1) {
        composedBuffer.delete(length - 1, length);
        getCurrentInputConnection().setComposingText(composedBuffer, 1);
        updateCandidate(candidateIndex);        
      } else if (length > 0) {
        composedBuffer.setLength(0);
        currentInputMode = InputModeEnglish;
        getCurrentInputConnection().commitText("", 0);        
        updateCandidate(candidateIndex);
      }
    } else {
      keyDownUp(KeyEvent.KEYCODE_DEL);
    }
    finishKeyInput();
    // updateShiftKeyState(getCurrentInputEditorInfo());
  }

  /*
   * エンターキー
   */
  private void handleEnter() {
    if (composedBuffer.length() > 0) {
      // 確定
      getCurrentInputConnection().finishComposingText();
      finishComposing();
      finishKeyInput();
    } else {
      // 改行
      getCurrentInputConnection().commitText("\n", 1);
    }
  }

  /*
   * スペースキー
   */
  private void handleSpace() {
    InputConnection ic = getCurrentInputConnection();
    if (composedBuffer.length() > 0) {
      // 変換
    } else {
      // スペース
      ic.commitText(" ", 0);
    }
  }

  /*
   * 使わないメソッド
   */

  public void onText(CharSequence text) {
    // 多分使わない
  }

  public void swipeDown() {
    // 使わない
  }

  public void swipeLeft() {
    // 使わない
  }

  public void swipeRight() {
    // 使わない
  }

  public void swipeUp() {
    // 使わない
  }

  public void onShowPress(MotionEvent e) {
    // TODO Auto-generated method stub
    Log.v("onShowPress", "showpress");
    if (!keyboardView.getPopupWindow().isShowing() && currentKeyCode < 300) {
      keyboardView.showPopupWindow(0, -keyboardView.getMeasuredHeight());
    }
  }

  public boolean onSingleTapUp(MotionEvent e) {
    // TODO Auto-generated method stub
    Log.v("onSingleTapUp", "singletapup");
    return false;
  }
}
