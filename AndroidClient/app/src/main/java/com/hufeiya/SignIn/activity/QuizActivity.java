/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hufeiya.SignIn.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hufeiya.SignIn.R;
import com.hufeiya.SignIn.application.MyApplication;
import com.hufeiya.SignIn.fragment.CameraPreviewfFragment;
import com.hufeiya.SignIn.fragment.CourseInfoFragment;
import com.hufeiya.SignIn.helper.ApiLevelHelper;
import com.hufeiya.SignIn.jsonObject.JsonSignInfo;
import com.hufeiya.SignIn.model.Category;
import com.hufeiya.SignIn.net.AsyncHttpHelper;
import com.hufeiya.SignIn.widget.TextSharedElementCallback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class QuizActivity extends AppCompatActivity implements LocationListener, CameraPreviewfFragment.OnRecognizedFaceListener {

    private static final String QI_NIU_DOMAIN = "http://7xp62n.com1.z0.glb.clouddn.com/";//the domain to save the photo on Qi Niu
    private static final String TAG = "QuizActivity";
    private static final String IMAGE_CATEGORY = "image_category_";
    private static final String STATE_IS_PLAYING = "isPlaying";
    private static final int REQUEST_CODE_FOR_POSITON = 88;
    private static final int REQUEST_CAMARA_PERMISSION = 89;
    private Location location;

    private Interpolator mInterpolator;
    private FloatingActionButton mQuizFab;
    private boolean mSavedStateIsPlaying;
    private View mToolbarBack;
    private static Category category;
    private CourseInfoFragment courseInfoFragment;
    private CameraPreviewfFragment cameraPreviewfFragment;
    private LocationManager locationManager;
    private ProgressBar progressBar;
    private Fragment mContent;
    private boolean isUploading = false;
    private boolean isShootingAccomplished = false;
    private int sid = 0;//signID for student user


    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.back:
                    onBackPressed();
                    break;
                case R.id.fab_quiz:
                    fabButtonPress();
                    break;
            }
        }
    };

    public static Intent getStartIntent(Context context, Category category) {
        Intent starter = new Intent(context, QuizActivity.class);
        QuizActivity.category = category;
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mInterpolator = new FastOutSlowInInterpolator();
        if (null != savedInstanceState) {
            mSavedStateIsPlaying = savedInstanceState.getBoolean(STATE_IS_PLAYING);
        }
        super.onCreate(savedInstanceState);
        populate();
        setEnterSharedElementCallback();
        inflateFragment();
        getLocation();

    }

    private void setEnterSharedElementCallback() {
        int categoryNameTextSize = getResources()
                .getDimensionPixelSize(R.dimen.category_item_text_size);
        int paddingStart = getResources().getDimensionPixelSize(R.dimen.spacing_double);
        final int startDelay = getResources().getInteger(R.integer.toolbar_transition_duration);
        ActivityCompat.setEnterSharedElementCallback(this,
                new TextSharedElementCallback(categoryNameTextSize, paddingStart) {
                    @Override
                    public void onSharedElementStart(List<String> sharedElementNames,
                                                     List<View> sharedElements,
                                                     List<View> sharedElementSnapshots) {
                        super.onSharedElementStart(sharedElementNames,
                                sharedElements,
                                sharedElementSnapshots);
                        mToolbarBack.setScaleX(0f);
                        mToolbarBack.setScaleY(0f);
                    }

                    @Override
                    public void onSharedElementEnd(List<String> sharedElementNames,
                                                   List<View> sharedElements,
                                                   List<View> sharedElementSnapshots) {
                        super.onSharedElementEnd(sharedElementNames,
                                sharedElements,
                                sharedElementSnapshots);
                        // Make sure to perform this animation after the transition has ended.
                        ViewCompat.animate(mToolbarBack)
                                .setStartDelay(startDelay)
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f);
                    }
                });
    }


    private void inflateFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (AsyncHttpHelper.user.getUserType()) {//student
            if (category.getId().equals("addcourse")) {

            } else {
                courseInfoFragment = CourseInfoFragment.newInstance(category.getName());
                transaction.replace(R.id.content, courseInfoFragment);
            }

        } else {//teacher

        }
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_IS_PLAYING, mQuizFab.getVisibility() == View.GONE);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mQuizFab == null) {
            // Skip the animation if icon or fab are not initialized.
            super.onBackPressed();
            return;
        } else if (mContent != null && mContent == cameraPreviewfFragment) {
            switchContent(cameraPreviewfFragment, courseInfoFragment);
            mContent = courseInfoFragment;
            return;
        }

        ViewCompat.animate(mToolbarBack)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(100)
                .start();


        ViewCompat.animate(mQuizFab)
                .scaleX(0f)
                .scaleY(0f)
                .setInterpolator(mInterpolator)
                .setStartDelay(100)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onAnimationEnd(View view) {
                        if (isFinishing() ||
                                (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1)
                                        && isDestroyed())) {
                            return;
                        }
                        QuizActivity.super.onBackPressed();
                    }
                })
                .start();
    }


    @SuppressLint("NewApi")
    public void setToolbarElevation(boolean shouldElevate) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            mToolbarBack.setElevation(shouldElevate ?
                    getResources().getDimension(R.dimen.elevation_header) : 0);
        }
    }


    @SuppressLint("NewApi")
    private void populate() {
        setTheme(category.getTheme().getStyleId());
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this,
                    category.getTheme().getPrimaryDarkColor()));
        }
        initLayout(category.getId());
        initToolbar(category);
    }

    private void initLayout(String categoryId) {
        setContentView(R.layout.activity_quiz);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        mQuizFab = (FloatingActionButton) findViewById(R.id.fab_quiz);
        mQuizFab.setImageResource(R.drawable.ic_play);
        if (mSavedStateIsPlaying) {
            mQuizFab.hide();
        } else {
            mQuizFab.show();
        }
        mQuizFab.setOnClickListener(mOnClickListener);
    }

    private void initToolbar(Category category) {
        mToolbarBack = findViewById(R.id.back);
        mToolbarBack.setOnClickListener(mOnClickListener);
        TextView titleView = (TextView) findViewById(R.id.category_title);
        if (category.getName().equals("添加课程")) {
            titleView.setText(category.getName());
        } else {
            titleView.setText(AsyncHttpHelper.user.getJsonCoursesMap().get(Integer.parseInt(category.getName())).getCourseName());
        }

        titleView.setTextColor(ContextCompat.getColor(this,
                category.getTheme().getTextPrimaryColor()));
        if (mSavedStateIsPlaying) {
            // the toolbar should not have more elevation than the content while playing
            setToolbarElevation(false);
        }
    }

    private void fabButtonPress() {
        if (AsyncHttpHelper.user.getUserType()) {//student user
            if (!category.getId().equals("addcourse")) {
                if (isShootingAccomplished) {
                    isShootingAccomplished = false;
                    progressBar.setVisibility(View.VISIBLE);
                    cameraPreviewfFragment.shooting();
                    Toast.makeText(this, "正在上传哦", Toast.LENGTH_LONG).show();
                } else if (isRightTimeToSign()) {
                    if (this.location == null) {
                        Toast.makeText(this, "还没获取到位置,抱歉蛤", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("quiz", "start send location");
                        if (isUploading) {
                            Toast.makeText(this, "正在检查位置哦", Toast.LENGTH_SHORT).show();
                        } else {
                            isUploading = true;
                            progressBar.setVisibility(View.VISIBLE);
                            AsyncHttpHelper.uploadLocation(this, category.getName(), location.getLatitude(), location.getLongitude());
                        }

                    }
                } else {
                    Toast.makeText(QuizActivity.this, "还没到签到时间哦", Toast.LENGTH_SHORT).show();
                }
            } else {

            }
        } else {//teacher user
            if (category.getId().equals("addcourse")) {

            } else {

            }
        }
    }

    private boolean isRightTimeToSign() {
        //TODO
        return true;
    }


    private void getLocation() {
        locationManager = (LocationManager) MyApplication.getContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyApplication.getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(QuizActivity.this, "我没有位置权限 ( > c < ) ", Toast.LENGTH_SHORT).show();
            String locationPermissions[] = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= 23) {
                requestPermissions(locationPermissions, REQUEST_CODE_FOR_POSITON);
            }
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
    }

    private boolean getCameraPremission() {
        if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(QuizActivity.this, "我没有拍照权限 ( > c < ) ", Toast.LENGTH_SHORT).show();
            String locationPermissions[] = {Manifest.permission.CAMERA};
            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= 23) {
                requestPermissions(locationPermissions, REQUEST_CAMARA_PERMISSION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_FOR_POSITON:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);

                } else {
                    // TODO permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            case REQUEST_CAMARA_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setStateBeforeCameraPreview();
                    startCameraPreview();
                }
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    //When your location is in the right scale.Called by AsyncHttpHelper.
    public void startSign() {
        if (getCameraPremission()) {
            setStateBeforeCameraPreview();
            startCameraPreview();
        } else {
            //I have no idea.I WANT the CAMERA!
        }

    }

    private void setStateBeforeCameraPreview() {
        mQuizFab.hide();
        isUploading = false;
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void toastNetUnavalible() {
        Toast.makeText(this, "蛤(-__-),网络连接失败.", Toast.LENGTH_SHORT).show();
    }

    public void startCameraPreview() {
        if (cameraPreviewfFragment == null) {
            cameraPreviewfFragment = CameraPreviewfFragment.newInstance();
        }
        switchContent(courseInfoFragment, cameraPreviewfFragment);
    }

    public void switchContent(Fragment from, Fragment to) {
        if (mContent != to) {
            mContent = to;
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            //.setCustomAnimations(android.R.anim.fade_in, android.R.anim.slide_out_right);
            if (!to.isAdded()) {    // 先判断是否被add过
                transaction.hide(from).add(R.id.content, to).commit(); // 隐藏当前的fragment，add下一个到Activity中
            } else {
                transaction.hide(from).show(to).commit(); // 隐藏当前的fragment，显示下一个
            }
        }
    }

    @Override
    public void onRecognizedFace() {
        isShootingAccomplished = true;
        mQuizFab.show();
    }

    @Override
    public void onNotRedcognizedFace() {
        isShootingAccomplished = false;
        mQuizFab.hide();
    }

    //When the Photo is taken by CameraPreviewFragment,this method called.
    @Override
    public void onPhotoTaken(byte[] data) {
        Map signInfos = AsyncHttpHelper.user.getJsonCoursesMap().get(Integer.parseInt(category.getName())).getSignInfos();
        Iterator iterator = signInfos.entrySet().iterator();
        while (iterator.hasNext()) {
            sid = (Integer) ((Map.Entry) iterator.next()).getKey();
        }
        AsyncHttpHelper.getUptokenAndUploadPhoto(this, data, Integer.toString(sid));
    }

    public void uploadPhotoSuccess() {
        //TODO
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "上传照片成功咯.开始上传签到信息", Toast.LENGTH_LONG).show();
        generateSignInfoAndUpload();

    }

    public void generateSignInfoAndUpload(){
        int cid = Integer.parseInt(category.getName());
        JsonSignInfo jsonSignInfo = AsyncHttpHelper.user.getJsonCoursesMap().get(cid).getSignInfos().get(sid);
        String signDetail = jsonSignInfo.getSignDetail();
        if(signDetail == null || signDetail.equals("")){
            jsonSignInfo.setSignDetail(Long.toString(System.currentTimeMillis()/1000));
        }else{
            jsonSignInfo.setSignDetail(signDetail+","+System.currentTimeMillis()/1000);
        }
        jsonSignInfo.setLastSignPhoto(QI_NIU_DOMAIN + sid);
        jsonSignInfo.setSignTimes(jsonSignInfo.getSignId() + 1);
        AsyncHttpHelper.uploadSignInfo(this,jsonSignInfo);
    }

    /**when sign done,The @AsyncHttpHelper calls it.*/
    public void onSignSuccess(){
        Toast.makeText(this, "签到成功咯!", Toast.LENGTH_LONG).show();
    }

    public void uploadPhotoFail() {
        //TODO
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "上传照片失败啦抱歉,再试试吧", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("quiz", "onDestory() executed!!");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}
