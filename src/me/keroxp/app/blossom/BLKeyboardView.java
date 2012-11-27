/**
 * 
 */
package me.keroxp.app.blossom;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

/**
 * @author keroxp
 *
 */
public class BLKeyboardView extends KeyboardView{

	// Flower表示のためのPopupWindow
	private PopupWindow popupWindow;
	// FlowerのViewGroup
	private RelativeLayout flowerLayout;
	// 親のBlossomオブジェクト
	private Context context;
	
	public BLKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.popupWindow = new PopupWindow(context);
		this.context = context;
	}
	
	public BLKeyboardView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		this.popupWindow = new PopupWindow(context);
		this.context = context;
	}
	
	public void makePopupWindow(View v){
		// PopupWindowにContentViewをセット
		popupWindow.setContentView(v);
		popupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}
	
	public void showPopupWindow(int x, int y){
		if (popupWindow.isShowing()){
			// 表示中だったら場所を更新
			View v = popupWindow.getContentView();
			int w = v.getMeasuredWidth();
			int h = v.getMeasuredHeight();
			popupWindow.update(x, y, w, h);
		}else{
			// 非表示中だったらば相対位置に表示
			popupWindow.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
		}
	}
	
	public void dismissPopupWindow(){
		// 消す
		if(popupWindow.isShowing()) {
			popupWindow.dismiss();
		}
	}
}
