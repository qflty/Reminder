package com.dizsun.renminers;

import android.app.Dialog;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.BoolRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RemindersActivity extends AppCompatActivity {
    private ListView lstView;
    private RemindersDbAdapter mDbAdapter;
    private ReminderSimpleCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        lstView = (ListView) findViewById(R.id.reminders_list_view);
        lstView.setDivider(null);
        //点击某个条目,弹出选择框:编辑或者删除
        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int masterPosition, long id) {
//                Toast.makeText(RemindersActivity.this,"点击了"+position,Toast.LENGTH_SHORT).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(RemindersActivity.this);
                ListView modeListView = new ListView(RemindersActivity.this);
                String[] modes = new String[]{"编辑条目","删除条目"};
                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(RemindersActivity.this,
                        android.R.layout.simple_list_item_1,android.R.id.text1,modes);
                modeListView.setAdapter(modeAdapter);
                builder.setView(modeListView);
                final Dialog dialog = builder.create();
                dialog.show();
                //当点击编辑或者删除选项时
                modeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int innerPosition, long id) {
                        if(innerPosition==0){
                            int nId = getIdFromPosition(masterPosition);
                            Reminder reminder = mDbAdapter.fetchReminderById(nId);
                            fireCustomDialog(reminder);
                        }
                        else {
                            mDbAdapter.deleteReminderById(getIdFromPosition(masterPosition));
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                        }
                        dialog.dismiss();
                    }
                });
            }
        });

        mDbAdapter = new RemindersDbAdapter(this);
        mDbAdapter.open();
        mDbAdapter.deleteAllReminders();
        mDbAdapter.createReminder("明天上课",true);
        mDbAdapter.createReminder("玩游戏",false);
        mDbAdapter.createReminder("交作业",true);
        mDbAdapter.createReminder("买衣服",false);
        mDbAdapter.createReminder("运动",false);
        Cursor cursor = mDbAdapter.fetchAllReminders();
        String[] from = new String[]{RemindersDbAdapter.COL_CONTENT};
        int[] to = new int[]{R.id.row_text};
        mCursorAdapter = new ReminderSimpleCursorAdapter(
                RemindersActivity.this,
                R.layout.reminders_row,
                cursor, from, to, 0
        );
        lstView.setAdapter(mCursorAdapter);
        //版本保护
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
            lstView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            lstView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean isChecked) {
                }

                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    MenuInflater inflater = actionMode.getMenuInflater();
                    inflater.inflate(R.menu.cam_menu,menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }
                //批量删除
                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    switch (menuItem.getItemId()){
                        case R.id.menu_item_delete_reminder:
                            for (int nC = mCursorAdapter.getCount()-1;nC>=0;nC--){
                                if(lstView.isItemChecked(nC)){
                                    mDbAdapter.deleteReminderById(getIdFromPosition(nC));
                                }
                            }
                            actionMode.finish();
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                            return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {

                }
            });
        }
    }
    //插入或者编辑某一条
    private void fireCustomDialog(final Reminder reminder){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom);

        TextView titleView = (TextView)dialog.findViewById(R.id.custom_title);
        final EditText editCustom = (EditText)dialog.findViewById(R.id.custom_edit_reminder);
        Button commitButton = (Button)dialog.findViewById(R.id.custom_button_commit);
        final CheckBox checkBox = (CheckBox)dialog.findViewById(R.id.custom_check_box);
        LinearLayout rootLayout = (LinearLayout)dialog.findViewById(R.id.custom_root_layout);
        final boolean isEditOperation = (reminder!=null);
        if(isEditOperation){
            titleView.setText("编辑条目");
            checkBox.setChecked(reminder.getmImportant()==1);
            editCustom.setText(reminder.getmContent());
            rootLayout.setBackgroundColor(getColor(R.color.colorBlue));
        }
        commitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String reminderText = editCustom.getText().toString();
                if(isEditOperation){
                    Reminder reminderEdited = new Reminder(reminder.getmId(),
                            reminderText,checkBox.isChecked()?1:0);
                    mDbAdapter.updateReminder(reminderEdited);
                }
                else {
                    mDbAdapter.createReminder(reminderText,checkBox.isChecked());
                }
                mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                dialog.dismiss();
            }
        });
        Button buttonCancel = (Button)dialog.findViewById(R.id.custom_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reminders, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_new:
                fireCustomDialog(null);
                return true;
            case R.id.action_exit:
                finish();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private int getIdFromPosition(int nC){
        return (int)mCursorAdapter.getItemId(nC);
    }
}
