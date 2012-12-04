/**
 * 
 */
package me.keroxp.app.blossom;

import org.json.JSONArray;
import org.json.JSONException;

import android.R.string;
import android.annotation.SuppressLint;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
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
  private BLFlowerLayout flowerLayout;

  public BLFlowerLayout getFlowerLayout() {
    return flowerLayout;
  }

  // 表示用のPieceArray
  private JSONArray piecesArray;

  public JSONArray getPiecesArray() {
    return piecesArray;
  }

  public void setPiecesArray(JSONArray piecesArray) {
    this.piecesArray = piecesArray;
  }

  // 親のBlossomオブジェクト
  private Blossom mBlossom;

  public BLKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (!isInEditMode()) {
      mBlossom = (Blossom) context;
      makePopupWindow(context);      
    }      
  }

  public BLKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    if (!isInEditMode()) {
      mBlossom = (Blossom) context;
      makePopupWindow(context);      
    }    
  }

  @Override
  public void onDraw(android.graphics.Canvas canvas) {
    super.onDraw(canvas);
    // PopupWindowのサイズをキーボードの40%に変更
    this.popupWindow.setWidth(this.getMeasuredWidth() * 4 / 10);
    this.popupWindow.setHeight(this.getMeasuredWidth() * 4 / 10);
  }

  public void makePopupWindow(Context context) {
    // PopupWindowを構築
    this.popupWindow = new PopupWindow(context);
    // FlowerLayoutをLayoutファイルから作成
    this.flowerLayout = (BLFlowerLayout) this.mBlossom.getLayoutInflater().inflate(R.layout.flower, null);
    // PopupWindowにセット
    popupWindow.setContentView(this.flowerLayout);
    popupWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    popupWindow.setBackgroundDrawable(null);
  }

  public void showPopupWindow(int x, int y) {
    JSONArray ps = this.getPiecesArray();
    BLFlowerLayout fl = this.getFlowerLayout();
    Log.d("showPopupWindow", fl.getWidth() + " : " + fl.getHeight());
    Log.d("showPopupWIndow", String.valueOf(fl.getChildCount()));
    for (int i = 0; i < ps.length(); i++) {
      TextView tView = (TextView) fl.getChildAt(i + 1);
      String pString;
      try {
        pString = ps.getString(i);
        tView.setText(pString);
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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
