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
import java.util.List;

import me.keroxp.app.blossom.BLKeyboard.BLKey;

//import net.sf.json.*;
import org.json.*;

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
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SimpleAdapter.ViewBinder;
import android.util.Log;

public final class Blossom extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
		View.OnTouchListener, BLKeyboard.BLKey.OnKeyActionListener, OnGestureListener, OnDoubleTapListener {

	// おまじないオブジェクト
	private InputMethodManager InputMethodManager;
	private String WordSeparators;

	// ジェスチャディテクタ
	private GestureDetector gestureDetector;

	// Keyboardオブジェクト
	private BLKeyboard mainKeyboard; // メイン
	private BLKeyboard currentKeyboard; // 現在のKeyboard

	// KeyboardViewオブジェクト
	private BLKeyboardView keyboardView;

	// なんだろこれ
	private int mLastDisplayWidth;

	// バッファ
	private StringBuilder originalBuffer = new StringBuilder(); // 生バッファ(あiueo）
	private StringBuilder composedBuffer = new StringBuilder(); // 変換後バッファ(あいうえお）
	private StringBuilder romeBuffer = new StringBuilder(); // (i.e i）

	// 辞書
	private JSONObject piecesDictionary; // ピース対応表
	private JSONObject romeDictionary; // ローマ字変換
	private JSONObject fullHalfDictionary; // 半角全角変換
	private JSONObject smallDictionary; // 大文字小文字変換

	// 遷移処理
	Boolean touching;
	
	// 現在のフリック方向
	int currentFlickDirection = -1;
	JSONArray currentPiecesArray;

	/**
	 * Main initialization of the input method component. Be sure to call to
	 * super class.
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
		gestureDetector.setOnDoubleTapListener(this);

		// 辞書を初期化
		try {
			this.piecesDictionary = this.getDictionary(R.raw.pieces);
			this.romeDictionary = this.getDictionary(R.raw.romakana);
			this.fullHalfDictionary = this.getDictionary(R.raw.fullhalf);
			this.smallDictionary = this.getDictionary(R.raw.small);
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
		case '\n':
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
		// 候補ビューはとりあえずいまは使わないから放置
		return null;
	}

	/**
	 * This is the main point where we do our initialization of the input method
	 * to begin operating on an application. At this point we have been bound to
	 * the client, and are now receiving all of the detailed information about
	 * the target of our edits.
	 */
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
		// バッファを初期化
		this.originalBuffer.setLength(0);
		this.composedBuffer.setLength(0);
		this.romeBuffer.setLength(0);
	}

	/**
	 * Called by the framework when your view for creating input needs to be
	 * generated. This will be called the first time your input method is
	 * displayed, and every time it needs to be re-created such as due to a
	 * configuration change.
	 */
	@Override
	public View onCreateInputView() {
		// レイアウトファイルからViewを作成。ファイルは res/layout/input.xml
		keyboardView = (BLKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
		// KeyboardViewのイベントリスナをこのクラスに
		keyboardView.setOnKeyboardActionListener(this);
		keyboardView.setOnTouchListener(this);
		// KeyboardViewにKeyboardをアサイン。
		keyboardView.setKeyboard(mainKeyboard);
		// Keyboardのプレビューをオフに
		keyboardView.setPreviewEnabled(false);
		return keyboardView;
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
		this.originalBuffer.setLength(0);
		this.romeBuffer.setLength(0);
		this.composedBuffer.setLength(0);

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
			this.handleBackspace();
			break;
		default:
			break;
		}
	}

	public void keyDidPress(BLKey key) {
		// Log.d("BLKeyboard.OnKeyActionListener", "key did press : " +
		// String.valueOf(key.codes[0]));
		JSONArray piecesArray = null;
		try {
			piecesArray = this.piecesDictionary.getJSONArray(String.valueOf(key.codes[0]));
			Log.d("Blossom.onPress", piecesArray.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// PopupWindowを表示（テスト）
		if (!this.keyboardView.getPopupWindow().isShowing()) {
			// Piecesをセット
			this.keyboardView.setPiecesArray(piecesArray);
			// 表示
			this.keyboardView.showPopupWindow(0, -this.keyboardView.getMeasuredHeight());
		}
		currentPiecesArray = piecesArray;
		// キャラクタなら入力
		if (key.codes[0] < 300) {
			//getCurrentInputConnection().commitText(key.label, 0);
		}
	}

	public void keyDidRelease(BLKey key, Boolean inside) {
		// TODO Auto-generated method stub
		Log.d("BLKeyboard.OnKeyActionListener", "key did Release : " + String.valueOf(key.codes[0]));
		if (currentFlickDirection > -1) {
			if (currentPiecesArray != null) {
				String s;
				try {
					s = currentPiecesArray.getString(currentFlickDirection);
					getCurrentInputConnection().commitText(s, 0);					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				currentPiecesArray = null;
				currentFlickDirection = -1;
			}
		}else{
			// キャラクタなら入力
			if (key.codes[0] < 300) {
				getCurrentInputConnection().commitText(key.label, 0);
			}
		}
		this.keyboardView.dismissPopupWindow();
	}

	// Keyが押されたとき、最初に一度だけ呼ばれる。Keyが連続された場合は呼ばれない。順番的には onKeyの前に呼ばれる。
	public void onPress(int primaryCode) {
		// 使わない
	}

	// Keyが離されたら呼ばれる。Keyが連続された場合は呼ばれない。順番的には onKeyの後に呼ばれる。
	public void onRelease(int primaryCode) {
		// 使わない
	}

	public void onText(CharSequence text) {
		Log.d("onText", String.valueOf(text));
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

	/*
	 * View.OnTouchListener keyboardView上でのタッチイベントはすべてここでバインドする
	 * Action一覧とかはここにある。ありすぎる。
	 * http://developer.android.com/reference/android/view/MotionEvent.html
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 * android.view.MotionEvent)
	 */

	private MotionEvent startEvent;
	private MotionEvent currentEvent;

	public boolean onTouch(View v, MotionEvent event) {
		// あとの処理はGestureDetectorに任せる
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			this.touching = false;
		}
		return this.gestureDetector.onTouchEvent(event);
	}

	/*
	 * GestureDetector.OnGestureListener
	 * KeyboardView.OnTouchListenerからMotionEventを受け取って抽象的なモーションを判別してくれる
	 * バインドしたらtrueを返す。でもその場合はそれ以外のMotionEventのリスナとかkeyDidReleaseとかが呼ばれないので注意。
	 * 
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.
	 * MotionEvent)
	 */

	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.v("onDown", "on down");
		this.touching = true;
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		// TODO Auto-generated method stub
		int THRESHOLD = 10;
		if (Math.abs(e2.getX() - e1.getX()) > THRESHOLD) {
			Log.v("onFling", "fling");
			return false;
		}
		return false;
	}

	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.v("onLongPress", "longpress");
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		Log.v("onScroll", "scroll");
		int dir = getDirection(e2.getRawX(), e2.getRawY(), e1.getRawX(), e1.getRawY());
		keyboardView.getFlowerLayout().hilightPiece(dir);
		currentFlickDirection = dir;
		return true;
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
			// 右上
			return BLDirectionUpRight;
		} else if (PI * 3 / 10 <= angle && angle < PI * 7 / 10) {
			// 上
			return BLDirectionUp;
		} else if (PI * 7 / 10 <= angle && angle < PI * 11 / 10) {
			// 左上
			return BLDirectionUpLeft;
		} else if (PI * 11 / 10 <= angle && angle < PI * 15 / 10) {
			// 左下
			return BLDirectionDownLeft;
		} else if (PI * 15 / 10 <= angle && angle < PI * 19 / 10) {
			// 右下
			return BLDirectionDownRight;
		} else {
			Log.d("getDirction", "invalid angel " + String.valueOf(angle));
		}
		return -1;
	}

	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.v("onShowPress", "showpress");
	}

	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		Log.v("onSingleTapUp", "singletapup");
		return false;
	}

	public boolean onDoubleTap(MotionEvent arg0) {
		// TODO Auto-generated method stub
		Log.v("onDoubleTap", "doubletap");
		return false;
	}

	public boolean onDoubleTapEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		Log.v("onDoubleTapEvent", "doubletapevent");
		return false;
	}

	public boolean onSingleTapConfirmed(MotionEvent arg0) {
		// TODO Auto-generated method stub
		Log.v("onSingleTapConfirmed", "singletapConirmed");
		return false;
	}

	private void handleBackspace() {
		final int length = this.composedBuffer.length();
		if (length > 1) {
			this.composedBuffer.delete(length - 1, length);
			getCurrentInputConnection().setComposingText(this.composedBuffer, 1);
			// updateCandidates();
		} else if (length > 0) {
			this.composedBuffer.setLength(0);
			getCurrentInputConnection().commitText("", 0);
			// updateCandidates();
		} else {
			keyDownUp(KeyEvent.KEYCODE_DEL);
		}
		// updateShiftKeyState(getCurrentInputEditorInfo());
	}
}
