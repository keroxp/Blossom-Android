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

//import net.sf.json.*;
import org.json.*;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.StaticLayout;
import android.text.method.MetaKeyKeyListener;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter.ViewBinder;
import android.util.Log;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

@SuppressWarnings("deprecation")
public final class Blossom extends InputMethodService implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener  {
    
	private InputMethodManager InputMethodManager;
	private String WordSeparators;

	// Keyboardオブジェクト
	private BLKeyboard mainKeyboard;
	// 現在のKeyboardオブジェクト
	private BLKeyboard currentKeyboard;
	// KeyboardView
	private BLKeyboardView keyboardView;

	private int mLastDisplayWidth;
	
	// バッファ
	private StringBuilder originalBuffer;
	private StringBuilder composedBuffer;
	private StringBuilder romeBuffer;
	
	// 辞書
	//private JSONObject keyDictionary; // Key
	private JSONObject piecesDictionary;
	private JSONObject romeDictionary; // ローマ字変換
	private JSONObject fullHalfDictionary; // 半角全角変換
	private JSONObject smallDictionary;  // 大文字小文字変換
    
	/**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    
	@Override public void onCreate(){
		super.onCreate();	
		// こいつが何なのかは分からない
        InputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        // こいつもよく分からない
        WordSeparators = getResources().getString(R.string.word_separators);
        
		// 辞書を初期化
        try {
//			this.keyDictionary = this.getDictionary(R.raw.keydictionary);
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
	
	/**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
	@Override public void onInitializeInterface () {
		
		if(mainKeyboard != null){
			// Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
		}
		
		// メインキーボードを作成
		mainKeyboard = new BLKeyboard(this, R.xml.qwerty);

	}
	
	/**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override 
    public View onCreateCandidatesView() {
    	// 候補ビューはとりあえずいまは使わないから放置
        return null;
    }
	
	/**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
	@Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
    }
	
	/**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
	@Override public View onCreateInputView() {
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

	@Override public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
        // Apply the selected keyboard to the input view.
        //keyboardView.setKeyboard(currentKeyboard);
        keyboardView.closing();
        // コメントアウトしておかないと落ちる 2.x系
        //final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        //keyboardView.setSubtypeOnSpaceKey(subtype);
    }

	// IMEによる入力が終わったときに呼ばれる。
    @Override public void onFinishInput() {
    	super.onFinishInput();
        
        // バッファをクリア
    	//this.originalBuffer.setLength(0);
    	//this.romeBuffer.setLength(0);
    	//this.composedBuffer.setLength(0);
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid _ping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
                
    	if (keyboardView != null) {
            keyboardView.closing();
        }
    }
    
    @Override public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
    	Log.d("Blossom.onKeyDown","keyCode : " + keyCode + " KeyEvent : " + keyEvent);    
    	// show Flower    	
    	//popupWindow.showAtLocation(keyboardView, Gravity.CENTER, 0, 0);
    	return super.onKeyDown(keyCode, keyEvent);
    }
    
    @Override public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    	Log.d("Blossom.onKeyUp","keyCod : " + keyCode + " KeyEvent : " + keyEvent);    	
    	//popupWindow.dismiss();
    	return super.onKeyUp(keyCode, keyEvent);
    }
       
    // Keyが押されたら必ず呼ばれる
 	public void onKey(int primaryCode, int[] keyCodes) {
 		// TODO Auto-generated method stub
 		Log.d("BLossom.onKey", "onkey");
    	//popupWindow.showAtLocation(keyboardView, Gravity.NO_GRAVITY, 0, 0);
 	}
 	// Keyが押されたとき、最初に一度だけ呼ばれる。Keyが連続された場合は呼ばれない。
 	// 順番的には onKeyの前に呼ばれる。
 	public void onPress(int primaryCode) {
 		// TODO Auto-generated method stub
 		Log.d("BLossom.onPress","key did press : " + primaryCode);	
 		try {
			JSONArray pieces = this.piecesDictionary.getJSONArray(String.valueOf(primaryCode));
			Log.d("Blossom.onPress", pieces.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();		
		} 		
 	}
 	
 	// Keyが離されたら呼ばれる。
 	// 順番的には onKeyの後に呼ばれる
 	public void onRelease(int primaryCode) {
 		// TODO Auto-generated method stub
 		Log.d("BLossom.onRelease","key did release : " + primaryCode);
    	//popupWindow.dismiss();
 	}

 	public void onText(CharSequence text) {
 		// TODO Auto-generated method stub
 		
 	}

 	public void swipeDown() {
 		Log.d("BLossom.swipeDown", "swipe down");
 	}

 	public void swipeLeft() {
 		Log.d("BLossom.swipeLeft","swipe left");
 	}

 	public void swipeRight() {
 		Log.d("BLossom.swipeRight", "swipe right");
 	}

 	public void swipeUp() {
 		Log.d("BLossom.swipeUp","swipe up");
 	} 	
 	
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

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
}
