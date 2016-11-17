package edu.arizona.netstats.fragments;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.arizona.netstats.R;
import edu.arizona.netstats.custom.UsageListAdapter;
import netstatbackend.AppObject;
import netstatbackend.AppStatsRepository;
import netstatbackend.NetworkStat;
import netstatbackend.NetworkStatistic;
import netstatbackend.Persistence;
import netstatbackend.UsageStat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SecondFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Persistence db;

    public SecondFragment() {
        // Required empty public constructor

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    @Override
    public void onViewCreated(View view,Bundle savedInstanceState){
        mRecyclerView = (RecyclerView)view.findViewById(R.id.apps_list);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(view.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

//        AsyncTaskRunner task = new AsyncTaskRunner();
//        task.execute(view.getContext());

        UsageTaskRunner task = new UsageTaskRunner();
        task.execute(view.getContext());
    }



    private void setUsageListData(ArrayList<UsageStat> usageStats){
        UsageListAdapter adapter = new UsageListAdapter(usageStats);
        mRecyclerView.setAdapter(adapter);
    }
    private class AsyncTaskRunner extends AsyncTask<Context,String,String>{

        private AppStatsRepository repository;
        private ArrayList<NetworkStat> appStats;
        @Override
        protected String doInBackground(Context... params) {
            try {
                Context context = params[0];
                appStats = new ArrayList<>();
                repository = new AppStatsRepository(context);
                PackageManager manager = context.getPackageManager();
                List<PackageInfo> packages = manager.getInstalledPackages(PackageManager.GET_PERMISSIONS | PackageManager.GET_PROVIDERS);
                for (PackageInfo pack : packages) {
                    ApplicationInfo info = pack.applicationInfo;
                    String[] permissions = pack.requestedPermissions;
                    if (permissions != null) {
                        for (int i = 0; i < permissions.length; i++) {
                            if (permissions[i].equals("android.permission.INTERNET")) {
                                NetworkStatistic stat = repository.getDataStats(info.uid, pack.lastUpdateTime);
                                if (stat.usageInBytes <= 1024 * 1024) {
                                    NetworkStat appStat = new NetworkStat();
                                    appStat.app = new AppObject();
                                    appStat.app.applicationIcon = manager.getApplicationIcon(pack.packageName);
                                    appStat.app.packageName = pack.packageName;
                                    appStat.app.appName = manager.getApplicationLabel(info).toString();
                                    appStat.totalUsageInBytes = stat.usageInBytes;
                                    appStats.add(appStat);
                                }
                            }
                        }
                    }
                   // setListData(appStats);

                }
            }
            catch (Exception e){
                Log.e("Error",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute (String result){
            
        }
    }

    private class UsageTaskRunner extends AsyncTask<Context,String,String>{

        private ArrayList<UsageStat> usageStats;
        @Override
        protected String doInBackground(Context... params) {
            Context context = (Context)params[0];
            usageStats = new ArrayList<>();
            try{
                PackageManager manager = context.getPackageManager();
                AppStatsRepository repository = new AppStatsRepository(context);
                List<PackageInfo>  packages = manager.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_PROVIDERS|PackageManager.GET_META_DATA);
                for(PackageInfo pack:packages){
                    ApplicationInfo info = manager.getApplicationInfo(pack.packageName,0);
                    if(isUserApp(info)) {
                        UsageStat stat = repository.getUsageStat(pack.packageName, pack.lastUpdateTime, System.currentTimeMillis());
                        stat.appName = manager.getApplicationLabel(info).toString();
                        stat.icon = manager.getApplicationIcon(info);
                        usageStats.add(stat);
                    }
                }
            }
            catch (Exception e){
                Log.e("Error",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            Collections.sort(usageStats,getComparator());
            setUsageListData(usageStats);
        }

        boolean isUserApp(ApplicationInfo ai) {
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (ai.flags & mask) == 0;
        }

        private Comparator<UsageStat> getComparator(){
            Comparator comp = new Comparator<UsageStat>(){

                @Override
                public int compare(UsageStat lhs, UsageStat rhs) {
                    if(lhs.foregroundEvents>rhs.foregroundEvents)
                        return 1;
                    else return -1;
                }
            };
            return comp;
        }
    }

}
