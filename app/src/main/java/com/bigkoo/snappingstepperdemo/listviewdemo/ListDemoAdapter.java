package com.bigkoo.snappingstepperdemo.listviewdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bigkoo.snappingstepper.SnappingStepper;
import com.bigkoo.snappingstepperdemo.R;

/**
 * Created by Sai on 16/2/18.
 */
public class ListDemoAdapter extends BaseAdapter {

    private Context context;

    public ListDemoAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return 100;
    }

    @Override
    public Object getItem(int position) {
        return "";
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.adapter_listdemo, null);
            holder = new ViewHolder();
            holder.stepperCustom = (SnappingStepper) convertView.findViewById(R.id.stepperCustom);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(position == 5){
            holder.stepperCustom.setMinValue(0);
            holder.stepperCustom.setValue(10);
        }
        return convertView;
    }

    private class ViewHolder {
        SnappingStepper stepperCustom;
    }
}
