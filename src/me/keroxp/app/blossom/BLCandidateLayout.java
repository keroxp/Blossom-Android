package me.keroxp.app.blossom;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

public class BLCandidateLayout extends TableLayout implements View.OnClickListener {

  private String[] candidateStrings;  
  private InputMethodService service;

  public BLCandidateLayout(Context context) {
    super(context);
    if (!isInEditMode()) {      
      setService((InputMethodService) context);
    }
  }

  public BLCandidateLayout(Context context, AttributeSet attr) {
    super(context, attr);
    if (!isInEditMode()) {
      setService((InputMethodService) context);
    }
  }
 
  public String[] getCandidateStrings() {
    return candidateStrings;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    Log.v("BLCandidateLayout", "onSizeChanged");
  };

  public void setCandidateStrings(String[] candidateStrings) {
    this.candidateStrings = candidateStrings;
    this.removeAllViews();
    int length = candidateStrings.length;
    if (length > 0) {      
      for (int i = 0; i < Math.ceil(length / 3); i++) {
        // 行を作成
        TableRow row = new TableRow(getContext());                
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (int j = 0; j < 3; j++) {
          int index = i * 3 + j;
          if (index < length) {
            // 列を作成
            BLCandidate candidate = new BLCandidate(service);
            candidate.setOnClickListener(this);
            candidate.setIndex(index);           
            candidate.setText(candidateStrings[index]);            
            row.addView(candidate, j);
          }
        }
        this.addView(row, i);
        getLayoutParams();
        requestLayout();
      }      
    }
  }

  public InputMethodService getService() {
    return service;
  }

  public void setService(InputMethodService service) {
    this.service = service;
  }

  public void onClick(View v) {
    int i = ((BLCandidate)v).getIndex();
    String s = candidateStrings[i];
    Log.v("onClick", s + " is Clicked");
    ((Blossom)service).candidateSelected(s);
  }
  
  private class BLCandidate extends Button{
    private int index;
    public int getIndex() {
      return index;
    }
    public void setIndex(int index) {
      this.index = index;
    }
    public BLCandidate(Context context) {
      super(context);
      this.setGravity(Gravity.CENTER);      
      this.setBackgroundResource(R.drawable.candidatebg);            
      this.setWidth(0);    
      this.setPadding(0,0,0,0);
      TableRow.LayoutParams tLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT);
      this.setLayoutParams(tLayoutParams);
    }    
  }

}
