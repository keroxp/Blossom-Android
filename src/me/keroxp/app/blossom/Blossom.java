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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import me.keroxp.app.blossom.BLKeyboard.BLKey;

//import net.sf.json.*;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

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
import android.util.Xml;

public final class Blossom extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
    View.OnTouchListener, OnGestureListener {

  // おまじないオブジェクト
  private InputMethodManager InputMethodManager;
  private String WordSeparators;

  // ジェスチャディテクタ
  private GestureDetector gestureDetector;
  // バイブレータ
  private Vibrator vibrator;
  // XMLパーサ
  private XmlPullParser xmlPullParser;

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
    // バイブレータを取得
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    // XMLパーサを初期化
    xmlPullParser = Xml.newPullParser();

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
        commitText(String.valueOf((char) keyCode), 1);
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
    currentInputMode = InputModeEnglish;
    currentFlickDirection = -1;
    currentKeyCode = -1;
    currentPiecesArray = null;
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
    linearLayout.setLayoutParams(new LayoutParams(-2, -2));
    // keyboard実体
    keyboardView = (BLKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
    // 候補ビュー
    candidateLayout = (BLCandidateLayout) getLayoutInflater().inflate(R.layout.candidate, null);
    candidateLayout.setService(this);
    candidateLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 70));
    // KeyboardViewのイベントリスナをこのクラスに
    keyboardView.setOnKeyboardActionListener(this);
    keyboardView.setOnTouchListener(this);
    // KeyboardViewにKeyboardをアサイン。
    keyboardView.setKeyboard(mainKeyboard);
    // Keyboardのプレビューをオフに
    keyboardView.setPreviewEnabled(false);
    // コンテナのサブビューに追加
    linearLayout.addView(candidateLayout, 0);
    linearLayout.addView(keyboardView, 1);
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
    case BLKeyboard.closeKey:
      handleClose();
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
              setComposingText(s, 1);
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
        appendComposedBuffer(s);
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
              deleteComposedBuffer((cl - 1) - (rl - 1), cl);
              // 変換文字を追加
              appendComposedBuffer(converted);
              // ローマバッファを空に
              romeBuffer.setLength(0);
            } else {
              // なければローマ字バッファに追加して入力を継続
              romeBuffer.append(label);
              appendComposedBuffer(label);
              if (romeBuffer.length() > 3) {
                // ホントはここでローマ字変換の可能性がないことをトップダウンで確認しなくてはいけない
                romeBuffer.setLength(0);
              }
            }
          }
        }
      }
    }
    finishKeyInput();
  }

  // KeybaordView上のタッチイベントをすべてここでバインドする
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      currentMotionEvent = event;
    }
    // あとの処理はGestureDetectorに任せる
    return gestureDetector.onTouchEvent(event);
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
              setComposingText(composedBuffer + s, 1);
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

  static final int BLDirectionUp = 0;
  static final int BLDirectionUpRight = 1;
  static final int BLDirectionDownRight = 2;
  static final int BLDirectionDownLeft = 3;
  static final int BLDirectionUpLeft = 4;

  public void onLongPress(MotionEvent e) {
    Log.v("onLongPress", "longpress");
    if (currentInputMode == InputModeEnglish && currentKeyCode < 300) {
      // Composing中のアルファベットを大文字に
      String s;
      try {
        s = labelDictionary.getString(String.valueOf(currentKeyCode));
        setComposingText(s.toUpperCase(), 1);
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
  }

  /*
   * compsedBufferの更新ラッパー
   */

  private void appendComposedBuffer(String string) {
    composedBuffer.append(string);
    setComposingText(composedBuffer, composedBuffer.length());
    updateCandidate();
  }

  private void replaceComposedBuffer(int start, int end, String string) {
    composedBuffer.replace(start, end, string);
  }

  private void deleteComposedBuffer(int start, int end) {
    int length = composedBuffer.length();
    if (length > 1) {
      composedBuffer.delete(start, end);
      setComposingText(composedBuffer, 1);
    } else if (length > 0) {
      composedBuffer.setLength(0);
      currentInputMode = InputModeEnglish;
      commitText("", 0);
    }
    updateCandidate();
  }

  private void setComposingText(CharSequence text, int newCursorPosition) {
    getCurrentInputConnection().setComposingText(text, newCursorPosition);
    if (currentInputMode == InputModeRomeKana) {
      int cp = getCurrentInputEditorInfo().initialSelStart;
      int cpe = getCurrentInputEditorInfo().initialSelEnd;
      Log.d("setComposingText", String.valueOf(cp) + ":" + String.valueOf(cpe));
      // getCurrentInputConnection().setSelection(cp, composedBuffer.length());
    }
  }

  private void commitText(CharSequence text, int newCursorPosition) {
    getCurrentInputConnection().commitText(text, newCursorPosition);
    updateCandidate();
  }

  /*
   * 候補ビューの更新
   */

  static final String[] cand1 = { "ほげ", "ふが", "ばー", "もぎゅ", "ねが", "ぱふ", "ふむ", "しにょん", "うにょん", "もにゅん" };
  static final String[] cand2 = { "つぼみ", "えりか", "いつき", "ゆり", "らぶ", "みき", "いのり", "せつな", "みゆき", "あかね", "なお", "やよい", "なお",
      "れいか" };
  static final String[] EMPTY_STRINGS = {};  

  private void updateCandidate() {
    if (composedBuffer.length() > 0) {
      // グーグルサジェストから持ってくる
      AsyncHttpClient client = new AsyncHttpClient();
      client.get("http://www.google.com/complete/search?output=toolbar&hl=ja&q=" + composedBuffer,
          new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
              // ここに通信が成功したときの処理をかく
              //System.out.println(response);
              try {
                xmlPullParser.setInput(new StringReader(response));
              } catch (XmlPullParserException e) {
                Log.d("XmlPullParserSample", "Error");
              }
              try {
                int eventType;
                ArrayList<String> suggests = new ArrayList<String>();
                while ((eventType = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {
                  if (eventType == XmlPullParser.START_TAG && "suggestion".equals(xmlPullParser.getName())) {
                    String s = xmlPullParser.getAttributeValue(null, "data");
                    suggests.add(s);
                  }
                }
                Log.v("updateCandidates", suggests.toString());
                candidateLayout.setCandidates(suggests);
              } catch (Exception e) {
                Log.d("XmlPullParserSample", "Error");
              }
            }
          });
    } else {
      candidateLayout.setCandidates(null);
    }
    candidateIndex++;
  }

  public void candidateSelected(String string) {
    Log.v("candidateSelected", string);
    // setComposingText(s, s.length());
    commitText(string, string.length());
    finishComposing();
    finishKeyInput();
  }

  // キーの共通の完了処理はここに書く
  private void finishKeyInput() {
    currentPiecesArray = null;
    currentFlickDirection = -1;
    currentKeyCode = -1;
    currentMotionEvent = null;
    keyboardView.dismissPopupWindow();
  }

  // コンポージングの終了処理はここに書く
  private void finishComposing() {
    // getCurrentInputConnection().setSelection(composedBuffer.length(),
    // composedBuffer.length());
    getCurrentInputConnection().finishComposingText();
    composedBuffer.setLength(0);
    romeBuffer.setLength(0);
    currentInputMode = InputModeEnglish;
    updateCandidate();
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
      deleteComposedBuffer(length - 1, length);
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
      finishComposing();
      finishKeyInput();
    } else {
      // 改行とか
      getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
      getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
    }
  }

  /*
   * スペースキー
   */
  private void handleSpace() {
    InputConnection ic = getCurrentInputConnection();
    if (composedBuffer.length() > 0) {
      finishComposing();
    } else {
      // スペース
      commitText(" ", 1);
    }
  }

  private void handleClose() {
    commitText(composedBuffer, composedBuffer.length());
    requestHideSelf(0);
    keyboardView.closing();
  }

  /*
   * ヘルパーメソッド
   */

  // res/rawのjsonファイルからJSONObectを返すメソッド
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

  // 位置から方向を取得するメソッド
  private int getDirection(float f, float g, float h, float i) {
    float dx = f - h;
    float dy = g - i;
    double angle = -Math.atan2(dy, dx);
    double PI = Math.PI;

    if (angle < 0)
      angle += PI * 2;

    if ((0 <= angle && angle < PI * 3 / 10) || (PI * 19 / 10 <= angle && angle <= PI * 2)) {
      return BLDirectionUpRight; // 右上
    } else if (PI * 3 / 10 <= angle && angle < PI * 7 / 10) {
      return BLDirectionUp; // 上
    } else if (PI * 7 / 10 <= angle && angle < PI * 11 / 10) {
      return BLDirectionUpLeft; // 左上
    } else if (PI * 11 / 10 <= angle && angle < PI * 15 / 10) {
      return BLDirectionDownLeft; // 左下
    } else if (PI * 15 / 10 <= angle && angle < PI * 19 / 10) {
      return BLDirectionDownRight; // 右下
    } else {
      Log.d("getDirction", "invalid angel " + String.valueOf(angle));
    }
    return -1;
  }

  /*
   * 使わないメソッド
   */

  public boolean onDown(MotionEvent e) {
    // Log.v("onDown", "on down");
    // バインドしてはいけない
    return false;
  }

  // 一定の距離をドラッグして離した場合の処理
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    return false;
  }

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
