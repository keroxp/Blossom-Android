package me.keroxp.app.blossom;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;

import android.util.Log;
import android.widget.PopupWindow;

public class BLKeyboardController implements KeyboardView.OnKeyboardActionListener {

	// Keyが押されたら必ず呼ばれる
	public void onKey(int primaryCode, int[] keyCodes) {
		// TODO Auto-generated method stub
		Log.d("BLKeyboardController.onKey", "");
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
