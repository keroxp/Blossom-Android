/**
 * 虎の巻
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.html
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.Key.html
 * http://developer.android.com/reference/android/inputmethodservice/Keyboard.Row.html
 * http://developer.android.com/reference/android/inputmethodservice/KeyboardView.html
 */
package me.keroxp.app.blossom;

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
    
    public BLKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public BLKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    // KeyオブジェクトをXMLから生成するメソッド
    // Resourceはres/xml/qwerty.xml
    // Xmlのparseとかは記法に則って書けば自動的にやってくれるらしい
    // 記法はここ http://developer.android.com/reference/android/inputmethodservice/Keyboard.Key.html
    
    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        Key key = new BLKey(res, parent, x, y, parser);
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
        //BLPie pie;
        Boolean isMetaKey;
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
        	// ここでPieの表示を行う？
        //	Log.d("BLKeyboard.BLKey","key is pressed : " + this.codes);
        }
        
        // Changes the pressed state of the key.
        @Override
        public void onReleased(boolean inside){
        	// ここでPieの非表示を行う？
        //	Log.d("BLKeyboard.BLKey", "key is released : " + this.codes);
        }
        
        // Pieのクラス。ここに書く意味があるのかは分からない
        /*
        private class BLPie{    	
        	BLPiePiece center;
        	BLPiePiece[] pieces;
        	public BLPie(BLPieDictionary dict){    		    	    	
        		if(dict != null){
        			// センターオブジェクトを生成
            		this.center = new BLPiePiece(dict.center,0);
            		// ピースオブジェクトを生成
            		for(int i = 0 , max = dict.pieces.length ; i < max ; i++){    			
                		BLPiePiece piece = new BLPiePiece(dict.pieces[i],i);
                		this.pieces[i] = piece;
                	}
        		}else{
        			Log.d("BLPieConstructor","probably dic is null");
        		}
        	}
        	private class BLPiePiece{
            	String key;
            	int index;
            	BLPiePiece(String key, int index){
            		this.key = key;    		
            		this.index = index;
            	}
            }
        }
        */                      
    }
}
