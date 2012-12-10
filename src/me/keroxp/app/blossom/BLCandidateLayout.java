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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

public class BLCandidateLayout extends ScrollView implements View.OnClickListener {

  private TableLayout tableLayout;
  private ArrayList<String> candidates;
  private InputMethodService service;

  public BLCandidateLayout(Context context) {
    super(context);
  }

  public BLCandidateLayout(Context context, AttributeSet attr) {
    super(context, attr);
  }

  @Override
  protected void onFinishInflate() {
    this.tableLayout = (TableLayout) findViewById(R.id.candidateTable);
  };

  public InputMethodService getService() {
    return service;
  }

  public void setService(InputMethodService service) {
    this.service = service;
  }

  public void onClick(View v) {
    int i = ((BLCandidate) v).getIndex();
    // String s = candidateStrings[i];
    String s = candidates.get(i);
    ((Blossom) service).candidateSelected(s);
  }

  public ArrayList<String> getCandidates() {
    return candidates;
  }

  public void setCandidates(ArrayList<String> candidates) {
    this.candidates = candidates;
    tableLayout.removeAllViews();
    if (candidates != null) {
      int length = candidates.size();
      if (length > 0) {
        ArrayList<BLCandidate> cs = new ArrayList<BLCandidateLayout.BLCandidate>(length);
        for (int i = 0; i < length; i++) {
          // 列を作成
          BLCandidate candidate = new BLCandidate(service, this);
          candidate.setOnClickListener(this);
          candidate.setIndex(i);
          candidate.setText(candidates.get(i));
          cs.add(candidate);
        }
        for (int i = 0; i < cs.size(); i++) {
          BLCandidate c = cs.get(i);
          // 行を作成
          TableRow row = new TableRow(getContext());
          row.setLayoutParams(new LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
              TableLayout.LayoutParams.WRAP_CONTENT));
          row.addView(c);
        }
        for (int i = 0; i < Math.ceil(length / 3); i++) {
          // 行を作成
          TableRow row = new TableRow(getContext());
          row.setLayoutParams(new LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
              TableLayout.LayoutParams.WRAP_CONTENT));
          for (int j = 0; j < 3; j++) {
            int index = i * 3 + j;
            if (index < length) {
              // 列を作成
              BLCandidate candidate = new BLCandidate(service, this);
              candidate.setOnClickListener(this);
              candidate.setIndex(index);
              candidate.setWidth(getMeasuredWidth() / 3);
              candidate.setText(candidates.get(index));
              row.addView(candidate, j);
            }
          }
          tableLayout.addView(row, i);
          getLayoutParams();
          requestLayout();
        }
      }
    }
  }

  private class BLCandidate extends Button {
    private int index;

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public BLCandidate(Context context, View parent) {
      super(context);
      this.setGravity(Gravity.CENTER);
      // this.setWidth(0);
      // this.setPadding(0,0,0,0);
      TableRow.LayoutParams tLayoutParams = new TableRow.LayoutParams(parent.getMeasuredWidth() / 3,
          TableRow.LayoutParams.WRAP_CONTENT);
      this.setLayoutParams(tLayoutParams);
      this.setBackgroundResource(R.drawable.candidatebg);
    }
  }

}
