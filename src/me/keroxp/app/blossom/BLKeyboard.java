/**
 * 
 */
package me.keroxp.app.blossom;

import android.inputmethodservice.Keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;

/**
 * @author keroxp
 *
 */
public class BLKeyboard extends Keyboard {

	public BLKeyboard(Context context, int layoutTemplateResId,
			CharSequence characters, int columns, int horizontalPadding) {
		super(context, layoutTemplateResId, characters, columns, horizontalPadding);
		// TODO Auto-generated constructor stub
	}
	
	
//	@Override protected Key createKeyFromXml(Resouece res, Keyboard.Row parend, int x, int y, XmlResourceParser, parser){
//		
//	}
}
