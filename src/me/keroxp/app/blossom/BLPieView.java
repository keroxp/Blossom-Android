package me.keroxp.app.blossom;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/*
 * パイメニュー用のViewクラス
 * BLKeyboardとかからpopupWindowを作ってsubViewにこいつをいれこむ。 
 */

enum PieceDirection {Up, UpRight, BottomRight, BottomLeft, UpLeft};

public class BLPieView extends View{
	
	public String[] pieces;
	public String center;
	private View centerView;
	private View[] pieceViews;
	
	public BLPieView(Context context, AttributeSet attr) {
		super(context, attr);			
	}
	
	// メインコンストラクタ
	public BLPieView(Context context, AttributeSet attr, String key) {
		super(context, attr);
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
	}
		
	@Override
	public void onDraw(Canvas canvas) {
	}
	
	private class BLPieCenterView extends View {
		public String key;
		public TextView keyView;
		
		public BLPieCenterView(Context context, String key) {
			super(context);
			this.key = key;
			this.keyView = new TextView(context);
			this.keyView.setText(key);
		}
	}
	
	private class BLPiePieceView extends View {
		public String key;
		public int direction;
		public TextView keyView;
		
		public BLPiePieceView(Context context, String key, int dir) {
			super(context);
			// TODO Auto-generated constructor stub
			this.key = key;
			this.direction = dir;
			this.keyView = new TextView(context);
			this.keyView.setText(key);
		}
		public BLPiePieceView(Context context, AttributeSet attr, String key, int dir) {
			super(context,attr);
			this.key = key;
			this.direction = dir;
			this.keyView = new TextView(context);
			this.keyView.setText(key);
		}
	}
}