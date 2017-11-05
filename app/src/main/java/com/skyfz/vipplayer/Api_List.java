package com.skyfz.vipplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.skyfz.vipplayer.SQHelper.ApiField;
import com.skyfz.vipplayer.SQHelper.DatabaseHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Api_List extends AppCompatActivity {

    private ListView lv;
    private ArrayList<HashMap<String, Object>> listItem;
    private int selectedId;
    private SimpleAdapter mSimpleAdapter;
    private DatabaseHandler db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api__list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.analysis_api);

        prefs = getSharedPreferences(FIELDS.SP_NAME, Context.MODE_PRIVATE);
        db = new DatabaseHandler(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View layout = getLayoutInflater().inflate(R.layout.addapi, null);
                final EditText APIName = (EditText) layout.findViewById(R.id.apiname);
                final EditText APIUrl = (EditText) layout.findViewById(R.id.apiurl);
                new AlertDialog.Builder(Api_List.this).setTitle(R.string.addapi).setView(layout)
                        .setPositiveButton(R.string.addBtn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String dName = APIName.getText().toString().trim();
                                String dUrl = APIUrl.getText().toString().trim();
                                if (!dUrl.startsWith("http:") && !dUrl.startsWith("https:")) {
                                    Toast.makeText(
                                            Api_List.this,
                                            R.string.wrongapimsg,
                                            Toast.LENGTH_SHORT).show();
                                }else {
                                    long dId = db.addAPI(new ApiField(dName, dUrl));
                                    HashMap<String, Object> map = new HashMap<String, Object>();
                                    map.put("ItemImage", R.drawable.check_mark_dark);
                                    map.put("ItemTitle", dName);
                                    map.put("ItemText", dUrl);
                                    map.put("ItemId", dId);
                                    listItem.add(0, map);
                                    mSimpleAdapter.notifyDataSetChanged();
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancelBtn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        long curId = prefs.getLong(FIELDS.SP_API_ID, (long)-1);
        if(curId == -1){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(FIELDS.SP_API_ID, (long)0);
            editor.apply();
            curId = 0;
        }

        lv = (ListView) findViewById(R.id.lv);
        listItem = new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> defaultApi = new HashMap<String, Object>();
        if(curId==0) {
            defaultApi.put("ItemImage", R.drawable.check_mark);
        }else{
            defaultApi.put("ItemImage", R.drawable.check_mark_dark);
        }
        defaultApi.put("ItemTitle", getString(R.string.defaultapi));
        defaultApi.put("ItemText", getString(R.string.defaultDesc));
        defaultApi.put("ItemId", (long)0);
        listItem.add(defaultApi);

        List<ApiField> APIList = db.getAllAPI();
        int i = 0;
        for (ApiField api : APIList) {
            i++;
            HashMap<String, Object> map = new HashMap<String, Object>();
            if(curId==api.getID()) {
                map.put("ItemImage", R.drawable.check_mark);
            }else{
                map.put("ItemImage", R.drawable.check_mark_dark);
            }
            map.put("ItemTitle", api.getName());
            map.put("ItemText", api.getURL());
            map.put("ItemId", api.getID());
            listItem.add(map);
        }

        mSimpleAdapter = new SimpleAdapter(this,listItem,R.layout.item,
                new String[] {"ItemImage", "ItemTitle", "ItemText", "ItemId"},
                new int[] {R.id.ItemImage,R.id.ItemTitle,R.id.ItemText,R.id.ItemId});

        lv.setAdapter(mSimpleAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedId = position;
                setAPIByPos(selectedId);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long id) {
                selectedId = position;
                String name = (String) listItem.get(selectedId).get("ItemTitle");
                String url = (String) listItem.get(selectedId).get("ItemText");
                new AlertDialog.Builder(Api_List.this).setTitle(R.string.setapi).setMessage(getResources().getString(R.string.apiname) + " " + name + "\n" + getResources().getString(R.string.apiurl) + " " + url)
                        .setPositiveButton(R.string.setBtn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setAPIByPos(selectedId);
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.deleteBtn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                long aid= (long) listItem.get(selectedId).get("ItemId");
                                long curId = prefs.getLong(FIELDS.SP_API_ID, (long)0);

                                db.deleteAPIById(aid);
                                listItem.remove(selectedId);
                                if(curId == aid){
                                    setAPIById(0);
                                }else {
                                    mSimpleAdapter.notifyDataSetChanged();
                                }

                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancelBtn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();

                return true;
            }
        });

//        List<DNSField> DNSList = db.getAllDNS();
//        for (DNSField dns : DNSList) {
//        }
    }

    private int getPosById(long id){
        int i, l = listItem.size();
        for(i=0; i<l; i++) {
            long aid= (long) listItem.get(i).get("ItemId");
           if(aid == id){
               return i;
           }
        }
        return -1;
    }

    private void setItemImage(int pos, int img){
//        View v = lv.getAdapter().getView(pos, null, null);
//        ((ImageView) v.findViewById(R.id.ItemImage)).setImageResource(img);
        listItem.get(pos).put("ItemImage", img);
    }

    private void setAPIByPos(int pos){
//        String name = (String) listItem.get(pos).get("ItemTitle");
        String url = (String) listItem.get(pos).get("ItemText");
        long aid= (long) listItem.get(pos).get("ItemId");
        long curId = prefs.getLong(FIELDS.SP_API_ID, (long)0);
        if(curId != aid){
            int curPid = getPosById(curId);
            if(curPid != -1) {
                setItemImage(curPid, R.drawable.check_mark_dark);
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(FIELDS.SP_API_ID, aid);
            editor.putString(FIELDS.SP_API_URL, url);
            editor.apply();
            setItemImage(pos, R.drawable.check_mark);
            mSimpleAdapter.notifyDataSetChanged();
        }
    }

    private  void setAPIById(long aid){
//        String name = (String) listItem.get(pos).get("ItemTitle");
        int pos = getPosById(aid);
        if(pos == -1) return;
        String url = (String) listItem.get(pos).get("ItemText");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(FIELDS.SP_API_ID, aid);
        editor.putString(FIELDS.SP_API_URL, url);
        editor.apply();
        setItemImage(pos, R.drawable.check_mark);
        mSimpleAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
