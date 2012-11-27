/**
 * 
 */
package me.keroxp.app.blossom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author keroxp
 * 
 */
@SuppressLint("DrawAllocation")
public class BLKeyboardView extends KeyboardView {

	// Flower表示のためのPopupWindow
	private PopupWindow popupWindow;

	public PopupWindow getPopupWindow() {
		return popupWindow;
	}

	// FlowerのViewGroup
	private RelativeLayout flowerLayout;

	public RelativeLayout getFlowerLayout() {
		return flowerLayout;
	}

	// 親のBlossomオブジェクト
	private Blossom mBlossom;

	public BLKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mBlossom = (Blossom) context;
		this.makePopupWindow(context);
	}

	public BLKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.popupWindow = new PopupWindow(context);
		this.mBlossom = (Blossom) context;
	}

	@Override
	public void onDraw(android.graphics.Canvas canvas) {
		super.onDraw(canvas);
		// PopupWindowのサイズをキーボードの40%に変更
		Log.d("BLKeyboardView.makePopupWindow",
				String.valueOf(this.getMeasuredWidth()));
		this.popupWindow.setWidth(this.getMeasuredWidth() * 4 / 10);
		this.popupWindow.setHeight(this.getMeasuredWidth() * 4 / 10);
	}

	public void makePopupWindow(Context context) {
		// PopupWindowを構築
		this.popupWindow = new PopupWindow(context);
		// FlowerLayoutをLayoutファイルから作成
		this.flowerLayout = (RelativeLayout) this.mBlossom.getLayoutInflater()
				.inflate(R.layout.pie, null);
		// PopupWindowにセット
		popupWindow.setContentView(this.flowerLayout);
		popupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
	}

	public void showPopupWindow(int x, int y) {
		if (popupWindow.isShowing()) {
			// 表示中だったら場所を更新
			View v = popupWindow.getContentView();
			int w = v.getMeasuredWidth();
			int h = v.getMeasuredHeight();
			popupWindow.update(x, y, w, h);
		} else {
			// 非表示中だったらば相対位置に表示
			popupWindow.showAtLocation(this, Gravity.CENTER, x, y);
		}
	}

	public void dismissPopupWindow() {
		// 消す
		if (popupWindow.isShowing()) {
			popupWindow.dismiss();
		}
	}
}
