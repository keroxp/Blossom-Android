/**
 * 
 */
package me.keroxp.app.blossom;

import java.lang.reflect.*;

import android.util.*;
import android.graphics.Rect;
import android.content.res.*;


/**
 * @author keroxp
 *
 */
class BLKey {
    Rect rect;
    String key;
    BLPie pie;
    Boolean isMetaKey;
    public BLKey(int x,int y,int w,int h,String key,BLPieDictionary dict){
    	this.rect = new Rect(x,y,x+w,y+h);
    	this.key = key;
    	this.pie = new BLPie(dict);    	
    	if ( key.length() > 2) {
    		isMetaKey = true;
    	}else{
    		isMetaKey = false;
    	}
    }
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
/*
dictionary = {
        "1" : {center : "1", pieces : ["","","","","",""] },
        "2" : {center : "2", pieces : ["","","","","",""] },
        "3" : {center : "3", pieces : ["","","","","",""] },
        "4" : {center : "4", pieces : ["","","","","",""] },
        "5" : {center : "5", pieces : ["","","","","",""] },
        "6" : {center : "6", pieces : ["","","","","",""] },
        "7" : {center : "7", pieces : ["","","","","",""] },
        "8" : {center : "8", pieces : ["","","","","",""] },
        "9" : {center : "9", pieces : ["","","","","",""] },
        "0" : {center : "0", pieces : ["","","","","",""] },
        "q" : {center : "q", pieces : ["くぁ", "くぃ", "く", "くぇ", "くぉ"] },
        "w" : {center : "w", pieces : ["わ", "うぃ", "う", "うぇ", "を"] },
        "e" : {center : "e", pieces : ["え", "え", "え", "え", "え"] },
        "r" : {center : "r", pieces : ["ら", "り", "る", "れ", "ろ"] },
        "t" : {center : "t", pieces : ["た", "ち", "つ", "て", "と"] },
        "y" : {center : "y", pieces : ["や", "い", "ゆ", "え", "よ"] },
        "u" : {center : "u", pieces : ["う", "う", "う", "う", "う"] },
        "i" : {center : "i", pieces : ["い", "い", "い", "い", "い"] },
        "o" : {center : "o", pieces : ["お", "お", "お", "お", "お"] },
        "p" : {center : "p", pieces : ["ぱ", "ぴ", "ぷ", "ぺ", "ぽ"] },
        "a" : {center : "a", pieces : ["あ", "あ", "あ", "あ", "あ"] },
        "s" : {center : "s", pieces : ["さ", "し", "す", "せ", "そ"] },
        "d" : {center : "d", pieces : ["だ", "ぢ", "づ", "で", "ど"] },
        "f" : {center : "f", pieces : ["ふぁ", "ふぃ", "ふ", "ふぇ", "ふぉ"] },
        "g" : {center : "g", pieces : ["が", "ぎ", "ぐ", "げ", "ご"] },
        "h" : {center : "h", pieces : ["は", "ひ", "ふ", "へ", "ほ"] },
        "j" : {center : "j", pieces : ["じゃ", "じ", "じゅ", "じぇ", "じょ"] },
        "k" : {center : "k", pieces : ["か", "き", "く", "け", "こ"] },
        "l" : {center : "l", pieces : ["ぁ", "ぃ", "ぅ", "ぇ", "ぉ"] },
        "z" : {center : "z", pieces : ["ざ", "じ", "ず", "ぜ", "ぞ"] },
        "x" : {center : "x", pieces : ["ぁ", "ぃ", "ぅ", "ぇ", "ぉ"] },
        "c" : {center : "c", pieces : ["つぁ", "つぃ", "つ", "つぇ", "つぉ"] },
        "v" : {center : "v", pieces : ["ヴぁ", "ヴぃ", "ヴ", "ヴぇ", "ヴぉ"] },
        "b" : {center : "b", pieces : ["ば", "び", "ぶ", "べ", "ぼ"] },
        "n" : {center : "n", pieces : ["な", "に", "ぬ", "ね", "の"] },
        "m" : {center : "m", pieces : ["ま", "み", "む", "め", "も"] },
        "," : {center : ",", pieces : ["、", "。", "ー", "！", "？"] },
        "." : {center : ".", pieces : ["", "", "", "", ""] },
        "-" : {center : "-", pieces : ["", "", "", "", ""] },
        "enter"   : {center : "⏎", pieces : ["","","","",""] },
        "command" : {center : "⌘", pieces : ["","","","",""] },
        "shift"   : {center : "⇧", pieces : ["","","","",""] },
        "space"   : {center : "space",  pieces : ["全角","","","",""] },
        "delete"  : {center : "⌫", pieces : ["","","","",""] },
        "num"     : {center : "123",pieces : ["","","","",""] }
}
*/
