package com.infiniteloop.mytasks.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.infiniteloop.mytasks.R;
import com.infiniteloop.mytasks.data.CheckList;
import com.infiniteloop.mytasks.data.CheckListItem;
import com.infiniteloop.mytasks.data.Task;
import com.infiniteloop.mytasks.data.TaskLab;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by theotherside on 17/04/15.
 */
public class CheckListFragment extends Fragment {

    private static final String TAG = CheckListFragment.class.getSimpleName();
    public static final String EXTRA_CHECKLIST="com.mytask.checklistIntent";

    private ArrayList<CheckListItem> mChecklistItems;
    private EditText mChecklistTitle;
    private Task mTask;
    private CheckList mChecklist;
    private boolean hasDataChanged=false;


    public static CheckListFragment newInstance(Object obj){
        Task task;
        CheckList checkList;

        Bundle args = new Bundle();

        //If object received is task
        if(obj instanceof Task) {
            task = (Task) obj;
            args.putParcelable(DetailTaskFragment.EXTRA_TASK,task);
        }

        //if object received is checklist
        if(obj instanceof CheckList){
            checkList = (CheckList)obj;
            args.putParcelable(EXTRA_CHECKLIST,checkList);
        }

        CheckListFragment fragment = new CheckListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mTask = args.getParcelable(DetailTaskFragment.EXTRA_TASK);
        mChecklist = args.getParcelable(EXTRA_CHECKLIST);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.new_checklist,container,false);

        mChecklistTitle =(EditText)rootView.findViewById(R.id.checklistTitle);
        mChecklistItems = new ArrayList<CheckListItem>();

        //A current checklist has been passed, display it on screen
        if(mChecklist!=null){
            mChecklistTitle.setText(mChecklist.getName());
            mChecklistItems = TaskLab.get(getActivity()).getChecklistItems(mChecklist.getId());
            mChecklist.setChecklistItems(mChecklistItems);
        }


        //get listView from rootlayout and set the adapter
        ListView checklistItems = (ListView)rootView.findViewById(R.id.checklist_item_list);
        final CheckListAdapter adapter = new CheckListAdapter(getActivity(),R.layout.checklist_item_view,mChecklistItems);
        checklistItems.setAdapter(adapter);

        final EditText newChecklistItem = (EditText)rootView.findViewById(R.id.newChecklistItem);

        ImageButton addItem = (ImageButton)rootView.findViewById(R.id.addItemButton);

        //Add item to checklist
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item = newChecklistItem.getText().toString();
                if(!item.matches("")){
                    CheckListItem checkListItem = new CheckListItem();
                    checkListItem.setItem(item);
                    checkListItem.setCompleted(false);
                    mChecklistItems.add(checkListItem);
                    newChecklistItem.setText("");
                    adapter.notifyDataSetChanged();
                    hasDataChanged=true;

                }
            }
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_note:
                final String title =mChecklistTitle.getText().toString().trim();
                    if(!title.matches("")){
                        //New Checklist to be saved
                        if(mTask!=null){
                            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                            dialog.setMessage(getString(R.string.save_checklist));
                            dialog.setPositiveButton(R.string.save,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(saveChecklist(title)){
                                        getActivity().setResult(Activity.RESULT_OK);
                                        getActivity().finish();
                                    };
                                }
                            });
                            dialog.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            dialog.show();
                        }

                        //if Checklist is being saved
                        if(mChecklist!=null){
                           //Data has changed , update database
                            if((!title.equals(mChecklist.getName())) || hasDataChanged){
                                mChecklist.setName(title);
                                mChecklist.setChecklistItems(mChecklistItems);
                                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                                dialog.setMessage(getString(R.string.save_checklist));
                                dialog.setPositiveButton(R.string.save,new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int result =TaskLab.get(getActivity()).updateCheckList(mChecklist);
                                        if(result !=0){
                                            getActivity().setResult(Activity.RESULT_OK);
                                            getActivity().finish();
                                        }
                                    }
                                });
                                dialog.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                                dialog.show();

                            }else{
                                //data unchanged
                                getActivity().setResult(Activity.RESULT_OK);
                                getActivity().finish();
                            }
                        }
                    }else{
                        Toast.makeText(getActivity(),getString(R.string.empty_checklist_title),Toast.LENGTH_SHORT).show();
                    }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean saveChecklist(String title){

        //Create new Checklist object and set values
        CheckList newChecklist = new CheckList();
        newChecklist.setChecklistItems(mChecklistItems);
        newChecklist.setCreatedDate(new Date());
        newChecklist.setEditedDate(new Date());
        newChecklist.setName(title);
        newChecklist.setTaskId(mTask.getId());
        long result =TaskLab.get(getActivity()).createCheckList(newChecklist);
        if(result !=-1)
            return true;
        return false;
    }

    private class CheckListAdapter extends ArrayAdapter<CheckListItem> {
        public CheckListAdapter(Context context, int resource, List<CheckListItem> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            //If we were not given a view, inflate one
            if(convertView==null){
                convertView=getActivity().getLayoutInflater()
                        .inflate(R.layout.checklist_item_view, parent, false);
            }

            //Configure the  view for the Checklist
            final CheckListItem item = getItem(position);

            CheckBox checklistItem = (CheckBox)convertView.findViewById(R.id.checklist_item);
            checklistItem.setText(item.getItem());
            checklistItem.setChecked(item.isCompleted());
            checklistItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setCompleted(isChecked);
                    hasDataChanged=true;
                }
            });
            return convertView;

        }
    }
}
