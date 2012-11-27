/**
 * 虎の巻
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.html
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.Key.html
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.Row.html
 * http://developer.android.com/reference/android/inputmethodservice/KeyboardView.html
 */
package me.keroxp.app.blossom;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.anim;
import android.R.integer;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

public class BLKeyboard extends Keyboard {
	
    private Key mEnterKey;
    private Key mSpaceKey;
    private Blossom mBlossom;

    public BLKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
        this.mBlossom = (Blossom)context;
    }

    public BLKeyboard(Context context, int layoutTemplateResId, CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
        this.mBlossom = (Blossom)context;
    }

    /**
    KeyオブジェクトをXMLから生成するメソッド
    Resourceはres/xml/qwerty.xml
    Xmlのparseとかは記法に則って書けば自動的にやってくれるらしい
    記法はここ http://developer.android.com/reference/android/inputmethodservice/Keyboard.Key.htm 
    **/
    
    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        BLKey key = new BLKey(res, parent, x, y, parser);
        
        if (key.codes[0] == 10) {
            mEnterKey = key;
        } else if (key.codes[0] == ' ') {
            mSpaceKey = key;
        }
        return key;
    }
    
    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null) {
            return;
        }
        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_next_key);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_send_key);
                break;
            default:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
        }
    }

    void setSpaceIcon(final Drawable icon) {
        if (mSpaceKey != null) {
            mSpaceKey.icon = icon;
        }
    }
    
    static class BLKey extends Keyboard.Key {    
    	// ピースの配列
    	private String mKeyString;
        private JSONArray pieces;        
        // メタキーか否か
        private Boolean metaKey;
        
        public BLKey(Resources res, Keyboard.Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }              
        
        //Returns the drawable state for the key, based on the current state and type of the key.        
        @Override
        public int[] getCurrentDrawableState(){
        	return super.getCurrentDrawableState();
        }

        // Detects if a point falls inside this key.
        @Override
        public boolean isInside(int x, int y) {
            return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
        }
        
        // Informs the key that it has been pressed, in case it needs to change its appearance or state.
        @Override
        public void onPressed(){        
        	super.onPressed(); 	
        }               
        
        // Changes the pressed state of the key.
        @Override
        public void onReleased(boolean inside){
        	super.onReleased(inside);
        }

		public JSONArray getPieces() {
			return pieces;
		}

		public void setPieces(JSONArray pieces) {
			this.pieces = pieces;
		}                                               
    }
    
    // 数字キー
    final static int zeroKey=100;
    final static int oneKey=101;
    final static int twoKey=102;
    final static int threeKey=103;
    final static int fourKey=104;
    final static int fiveKey=105;
    final static int sixKey=106;
    final static int sevenKey=106;
    final static int eightKey=107;
    final static int nineKey=108;

    // 文字キー
    final static int qKey=200;
    final static int wKey=201;
    final static int eKey=202;
    final static int rKey=203;
    final static int tKey=204;
    final static int yKey=205;
    final static int uKey=206;
    final static int iKey=207;
    final static int oKey=208;
    final static int pKey=209;
    final static int aKey=210;
    final static int sKey=211;
    final static int dKey=212;
    final static int fKey=213;
    final static int gKey=214;
    final static int hKey=215;
    final static int jKey=216;
    final static int kKey=217;
    final static int lKey=218;
    final static int zKey=219;
    final static int xKey=220;
    final static int cKey=221;
    final static int vKey=222;
    final static int bKey=223;
    final static int nKey=224;
    final static int mKey=225;

    // 記号キー
    final static int commaKey=300;
    final static int periodKey=301;
    final static int hyphenKey=302;

    // メタキー
    final static int deleteKey=400;
    final static int commandKey=401;
    final static int shiftKey=402;
    final static int enterKey=403;
    final static int spaceKey=404;
    final static int numKey=405;
    final static int smallKey=406;
}
