package me.keroxp.app.blossom;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

/*
 * パイメニュー用のViewクラス
 * BLKeyboardとかからpopupWindowを作ってsubViewにこいつをいれこむ。 
 */

enum PieceDirection {Up, UpRight, BottomRight, BottomLeft, UpLeft};
public class BLPieView extends View{
	
	private String[] pieces;
	private String center;
	private View centerView;
	private View[] pieceViews;

	// メインコンストラクタ
	public BLPieView(Context context, AttributeSet attr, String key) {
		super(context, attr);
		try {
			JSONObject localDict = this.getDictionary().getJSONObject(key);
			if(localDict != null){
				this.center = localDict.getString("center");
				this.centerView = new BLPieCenterView(context, this.center);
				JSONArray pieces = localDict.getJSONArray("pieces");
				for(int i = 0 ; i < pieces.length() ; i++) {
					this.pieces[i] = pieces.getString(i);
					this.pieceViews[i] = new BLPiePieceView(context, this.pieces[i], i);
				}
			}else{
				Log.d("BLPieView","missing get json...");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		
	}
	
	// res/rawのjsonファイルからJSONObectを返すメソッド
	private JSONObject getDictionary() throws IOException, JSONException {
		BufferedReader bufferedReader = null;
		try {
			InputStream inStream = getResources().openRawResource(R.raw.dictionary);
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