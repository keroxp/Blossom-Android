package me.keroxp.app.blossom;

import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class BLFlowerLayout extends ViewGroup {

	private ArrayList<TextView> pieces;

	public ArrayList<TextView> getPieces() {
		if (pieces == null) {
			// 周りのピースを構築
			pieces = new ArrayList<TextView>(5);
			for (int i = 0; i < 5; i++) {				
				pieces.add((TextView)getChildAt(i+1));
			}
		}
		return pieces;
	}

	private ImageView backgroundImageView;

	public ImageView getBackgrImageView() {
		return this.backgroundImageView;
	}

	public BLFlowerLayout(Context context) {
		super(context);
		this.commonInit(context);
	}

	public BLFlowerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.commonInit(context);
	}

	private void commonInit(Context context) {
		
	}
	
	// 指定方向のピースをハイライト
	public void hilightPiece(int i){
		for (int j = 0; j < this.getPieces().size(); j++) {
			TextView piece = this.getPieces().get(j);
			piece.setBackgroundResource(0);
		}
		this.getPieces().get(i).setBackgroundResource(R.drawable.piepiecebg);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);// 高さのモードを取得
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);// 幅のモードを取得
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);// 幅を取得
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);// 高さを取得
		setMeasuredDimension(widthSize, heightSize);
		for (int i = 0; i < this.getChildCount(); i++) {
			View view = getChildAt(i);
			view.measure(widthSize,heightSize);
			if (view.getClass() == TextView.class) {
				int w = widthSize / 6;
				int h = heightSize / 6;
				view.measure(w, h);
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final double PI = Math.PI;
		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);
			if (view.getClass() == ImageView.class) {
				view.layout(getLeft(), getTop(), getRight(), getBottom());
			} else if (view.getClass() == TextView.class) {
				int d = getMeasuredWidth() / 3;
				int cx = getMeasuredWidth() / 2;
				int cy = getMeasuredHeight() / 2;
				int w = view.getMeasuredWidth();
				int h = view.getMeasuredHeight();
				int offsx = (int) (d * Math.cos((72 * (i - 1) - 90) * PI / 180));
				int offsy = (int) (d * Math.sin((72 * (i - 1) - 90) * PI / 180));
				int left = cx - w / 2 + offsx;
				int top = cy - h / 2 + offsy;
				int right = cx + w / 2 + offsx;
				int bottom = cy + h / 2 + offsy;
				view.layout(left, top, right, bottom);
			}
		}
	}

	public class BLFlowerPiece extends TextView {

		private Boolean hilighted;

		public BLFlowerPiece(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

		public BLFlowerPiece(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
		};

		public Boolean getHilighted() {
			return hilighted;
		}

		public void setHilighted(Boolean hilighted) {
			this.hilighted = hilighted;
			this.setBackgroundResource(R.drawable.piepiecebg);
		}
	}
}
