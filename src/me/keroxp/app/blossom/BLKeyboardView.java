/**
 * 
 */
package me.keroxp.app.blossom;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

/**
 * @author keroxp
 *
 */
public class BLKeyboardView extends KeyboardView {

	private PopupWindow popupWindow;
	private BLPieView pieView;
	
	public BLKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		popupWindow = new PopupWindow();
	    popupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);	    	   
	}
	
	public BLKeyboardView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}
	
	public void showPopup(int x, int y){
		
	}
	
	public void dismissPopup(){
		
	}

}
