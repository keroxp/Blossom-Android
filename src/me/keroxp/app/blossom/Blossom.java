/**
 * Blossom
 * A Japanese text input system for Android
 * Developed by Yusuke Sakurai as Keio Univ SFC Masui Lab.
 */
package me.keroxp.app.blossom;

import java.util.List;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.util.Log;

import android.widget.AbsoluteLayout; 

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.ClipboardManager;

public final class Blossom extends InputMethodService implements KeyboardView.OnKeyboardActionListener  {
    
	private InputMethodManager InputMethodManager;
	private String WordSeparators;

	// Keyboardオブジェクト
	private BLKeyboard mainKeyboard;
	// 現在のKeyboardオブジェクト
	private BLKeyboard currentKeyboard;
	// KeyboardView
	private BLKeyboardView keyboardView;
	// Keyboardのイベントハンドラ
	private BLKeyboardController keyboardController;
	// 候補ビューとかパイビューもこいつが管理する必要がある？	
    
	private int mLastDisplayWidth;
    
	private PopupWindow popupWindow;
    private BLPieView pieView;
    
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
		// イベント処理は全部コイツに投げる
		keyboardController = new BLKeyboardController();	
		
		// Pie View
		pieView = (BLPieView)getLayoutInflater().inflate(R.layout.pie, null);		
		// Popup
		popupWindow = new PopupWindow(pieView);
//		popupWindow.setContentView(pieView);
		popupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		popupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		popupWindow.setHeight(LayoutParams.WRAP_CONTENT);
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
		// KeyboardViewのイベントハンドラを設定。ここではkeyboardControllerにメソッドを実装。
		keyboardController = new BLKeyboardController();		
        //keyboardView.setOnKeyboardActionListener(keyboardController);
		keyboardView.setOnKeyboardActionListener(this);
        // KeyboardViewにKeyboardをアサイン。
        keyboardView.setKeyboard(mainKeyboard);
        // Previewをオフに
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

    @Override public void onFinishInput() {
    	super.onFinishInput();
    	if (keyboardView != null) {
            keyboardView.closing();
        }
    }
    
    @Override public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
    	Log.d("Blossom.onKeyDown","keyCode : " + keyCode + " KeyEvent : " + keyEvent);    
    	List<Keyboard.Key> keys = mainKeyboard.getKeys();    	
    	// show Flower    	
    	popupWindow.showAtLocation(keyboardView, Gravity.CENTER, 0, 0);
    	return super.onKeyDown(keyCode, keyEvent);
    }
    
    @Override public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    	Log.d("Blossom.onKeyUp","keyCod : " + keyCode + " KeyEvent : " + keyEvent);    	
    	popupWindow.dismiss();
    	return super.onKeyUp(keyCode, keyEvent);
    }
       
 // Keyが押されたら必ず呼ばれる
 	public void onKey(int primaryCode, int[] keyCodes) {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.onKey", "onkey");
    	popupWindow.showAtLocation(keyboardView, Gravity.NO_GRAVITY, 0, 0);
 	}
 	// Keyが押されたとき、最初に一度だけ呼ばれる。Keyが連続された場合は呼ばれない。
 	// 順番的には onKeyの前に呼ばれる。
 	public void onPress(int primaryCode) {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.onPress","key did press : " + primaryCode); 		
 	}
 	
 	// Keyが離されたら呼ばれる。
 	// 順番的には onKeyの後に呼ばれる
 	public void onRelease(int primaryCode) {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.onRelease","key did release : " + primaryCode);
    	popupWindow.dismiss();
 	}

 	public void onText(CharSequence text) {
 		// TODO Auto-generated method stub
 		
 	}

 	public void swipeDown() {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.swipeDown", "swipe down");
 	}

 	public void swipeLeft() {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.swipeLeft","swipe left");
 	}

 	public void swipeRight() {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.swipeRight", "swipe right");
 	}

 	public void swipeUp() {
 		// TODO Auto-generated method stub
 		Log.d("BLKeyboardController.swipeUp","swipe up");
 	}
}
