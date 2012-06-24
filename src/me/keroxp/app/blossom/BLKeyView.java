/**
 * 
 */
package me.keroxp.app.blossom;

import android.view.View;
import android.view.MotionEvent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint.FontMetrics;
import android.content.res.Resources;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

/**
 * @author keroxp
 *
 */
public class BLKeyView extends View {
	private Bitmap keybg53x53;
	private Paint keyPaint;
	private final int keyBoardWidth = 320;
	private final int keyWidth = 28;
	private final int keyHeight = 28;
	private final int keyMarginWidth = 5;
	private final int keyMarginHeight = 5;
	
	public float zoomScale = (float)1.0;
	
	public BLKeyView (Context ctx, AttributeSet attrset){
		super(ctx,attrset);
		
		//キー画像の読み込み
		Resources res = ctx.getResources();
		keybg53x53 = BitmapFactory.decodeResource(res,R.drawable.keybg53x53);
	}
	
	private void initGraphics(){
		keyPaint = new Paint();
		keyPaint.setAntiAlias(true);
	}
	

}
