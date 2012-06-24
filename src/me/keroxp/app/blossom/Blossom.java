/**
 * Blossom
 * A Japanese text input system for Android
 * Developed by Yusuke Sakurai as Keio Univ SFC Masui Lab.
 */
package me.keroxp.app.blossom;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.widget.Button;
import android.util.Log;

import android.widget.AbsoluteLayout; 

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.ClipboardManager;

public final class Blossom extends InputMethodService {
	
	public BLKey[][] keys;
	
	private final String[][] rows = {
			{"1","2","3","4","5","6","7","8","9","0"},
			{"q","w","e","r","t","y","u","i","o","p"},
		    {"a","s","d","f","g","h","j","k","l","enter"},
		    {"shift","z","x","c","v","b","n","m",",",".","-"},
		    {"command","space","num"}
	};
	/**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
	@Override public void onCreate(){
		super.onCreate();
		
	}
	
	/**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
	@Override public void onInitializeInterface () {
		XmlResourceParser parser = (XmlResourceParser)getResources().getXml(R.xml.dictionary);
		for(int i = 0 , max = rows.length ; i < max ; i++){
			for(int j = 0 , maxx = rows[i].length ; j < maxx ; j++){
				BLPieDictionary dict = this.getDictionaryFromXml(rows[i][j], parser);
				keys[i][j] = new BLKey(0,0,10,10,rows[i][j],dict);
			}
		}
	}
	
	/**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
	@Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
    }
	
	/**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
	@Override public View onCreateInputView() {
		View someView = new View(null);
		return someView;
	}

	@Override public void onStartInputView(EditorInfo info, boolean restarting) {
	// // 別アプリに切り替わったときなど初期化する
		getCurrentInputConnection().commitText("",1);
		onFinishInput();
		//keyController.resetInput();
    }

    @Override public void onFinishInput() {
    	super.onFinishInput();
    }

    public void input(String s){
    	getCurrentInputConnection().commitText(s,1); // 入力貼り付け
    }

    public void keyDownUp(int keyEventCode) { // キー入力      
    	getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    public void showComposingText(){
    }

    public void hide(){
    	requestHideSelf(0); // // IMEのViewを隠す
    }

    //
    // 新規登録用にクリップボードの単語を返す
    //
    public void clearRegWord(){
	
    }    
    
    // XMLからキーのデータを取得
    private BLPieDictionary getDictionaryFromXml (String key, XmlResourceParser parser){
		BLPieDictionary dict;
		try {
		    int eventType;
		    String center = "";
		    String[] pieces = new String[5];
		    eventType = parser.getEventType();
		    while (eventType != parser.END_DOCUMENT) {
		        if(eventType == parser.START_DOCUMENT) {
		            Log.d("XmlPullParserSample", "Start document");
		        } else if(eventType == parser.END_DOCUMENT) {
		            Log.d("XmlPullParserSample", "End document");
		        } else if(eventType == parser.START_TAG) {
		        	// タグの名前がkey かつ id属性がkeyと一致した場合    		        	
		        	if("key".equals(parser.getName()) && key.equals(parser.getAttributeValue(null, "id"))){
		        		int eventTypeForKey = parser.next();
		        		while(eventTypeForKey != parser.END_TAG && "key".equals(parser.getName())){
		        			if(eventTypeForKey == parser.START_TAG && "center".equals(parser.getName())){
    		        			// centerを取得
    		        			center = (String)parser.nextText();    		        			
		        			}else if(eventTypeForKey == parser.START_TAG && "pieces".equals(parser.getName())){
		        				int eventTypeForPieces = parser.next();
		        				int i = 0;
		        				while(eventTypeForPieces != parser.END_TAG && "pieces".equals(parser.getName())){
		        					// pieceを取得
		        					pieces[i] = parser.nextText();
		        					i++;
		        					eventTypeForPieces = parser.next();
		        				}
		        			}    		        			
		        			eventTypeForKey = parser.next();
		        		}
		        		if(eventTypeForKey == parser.END_TAG && "key".equals(parser.getName())){    		        			
		        			return new BLPieDictionary(key, center, pieces);	    		        			
		        		}
		        	}    		        	
		            Log.d("XmlPullParserSample", "Start tag "+parser.getName());
		        } else if(eventType == parser.END_TAG) {
		            Log.d("XmlPullParserSample", "End tag "+parser.getName());
		        } else if(eventType == parser.TEXT) {
		            Log.d("XmlPullParserSample", "Text "+parser.getText());
		        }
		        eventType = parser.next();
		    }
		} catch (Exception e) {
		     Log.d("XmlPullParserSample", "Error");    		     
		}
		return null;
	}
}