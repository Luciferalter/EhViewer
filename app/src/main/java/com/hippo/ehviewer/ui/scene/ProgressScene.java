/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.ExceptionUtils;
import com.hippo.vector.VectorDrawable;
import com.hippo.view.ViewTransition;
import com.hippo.widget.SimpleImageView;

/**
 * Only show a progress with jobs in background
 */
public final class ProgressScene extends BaseScene implements View.OnClickListener {

    public static final String KEY_ACTION = "action";
    public static final String ACTION_GALLERY_TOKEN = "gallery_token";

    private static final String KEY_VALID = "valid";
    private static final String KEY_ERROR = "error";

    public static final String KEY_GID = "gid";
    public static final String KEY_PTOKEN = "ptoken";
    public static final String KEY_PAGE = "page";

    private boolean mValid;
    private String mError;

    private String mAction;

    private int mGid;
    private String mPToken;
    private int mPage;

    private View mTip;
    private TextView mTipText;
    private ViewTransition mViewTransition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private boolean doJobs() {
        if (ACTION_GALLERY_TOKEN.equals(mAction)) {
            if (mGid == -1 || mPToken == null || mPage == -1) {
                return false;
            }

            EhRequest request = new EhRequest()
                    .setMethod(EhClient.METHOD_GET_GALLERY_TOKEN)
                    .setArgs(mGid, mPToken, mPage)
                    .setCallback(new GetGalleryTokenListener(getContext(),
                            ((StageActivity) getActivity()).getStageId(), getTag()));
            EhApplication.getEhClient(getContext()).execute(request);
            return true;
        }
        return false;
    }

    private boolean handleArgs(Bundle args) {
        if (args == null) {
            return false;
        }

        mAction = args.getString(KEY_ACTION);
        if (ACTION_GALLERY_TOKEN.equals(mAction)) {
            mGid = args.getInt(KEY_GID, -1);
            mPToken = args.getString(KEY_PTOKEN, null);
            mPage = args.getInt(KEY_PAGE, -1);
            if (mGid == -1 || mPToken == null || mPage == -1) {
                return false;
            }
            return true;
        }

        return false;
    }

    private void onInit() {
        mValid = handleArgs(getArguments());
        if (mValid) {
            mValid = doJobs();
        }
        if (!mValid) {
            mError = getString(R.string.error_something_wrong_happened);
        }
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mValid = savedInstanceState.getBoolean(KEY_VALID);
        mError = savedInstanceState.getString(KEY_ERROR);

        mAction = savedInstanceState.getString(KEY_ACTION);

        mGid = savedInstanceState.getInt(KEY_GID, -1);
        mPToken = savedInstanceState.getString(KEY_PTOKEN, null);
        mPage = savedInstanceState.getInt(KEY_PAGE, -1);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VALID, mValid);
        outState.putString(KEY_ERROR, mError);

        outState.putString(KEY_ACTION, mAction);

        outState.putInt(KEY_GID, mGid);
        outState.putString(KEY_PTOKEN, mPToken);
        outState.putInt(KEY_PAGE, mPage);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_progress, container, false);
        View progress = view.findViewById(R.id.progress);
        mTip = view.findViewById(R.id.tip);
        SimpleImageView tipImage = (SimpleImageView) mTip.findViewById(R.id.tip_image);
        mTipText = (TextView) mTip.findViewById(R.id.tip_text);

        mTip.setOnClickListener(this);
        tipImage.setDrawable(VectorDrawable.create(getContext(), R.xml.sadpanda_head));
        mTipText.setText(mError);

        mViewTransition = new ViewTransition(progress, mTip);

        if (mValid) {
            mViewTransition.showView(0, false);
        } else {
            mViewTransition.showView(1, false);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        if (mTip == v) {
            if (doJobs()) {
                mValid = true;
                // Show progress
                mViewTransition.showView(0, true);
            }
        }
    }

    private void onGetGalleryTokenSuccess(String result) {
        Bundle arg = new Bundle();
        arg.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
        arg.putInt(GalleryDetailScene.KEY_GID, mGid);
        arg.putString(GalleryDetailScene.KEY_TOKEN, result);
        arg.putInt(GalleryDetailScene.KEY_PAGE, mPage);
        startScene(new Announcer(GalleryDetailScene.class).setArgs(arg));
        finish();
    }

    private void onGetGalleryTokenFailure(Exception e) {
        mValid = false;
        mError = ExceptionUtils.getReadableString(getContext(), e);

        if (isViewCreated()) {
            // Show tip
            mViewTransition.showView(1);
            mTipText.setText(mError);
        }
    }

    private static class GetGalleryTokenListener extends EhCallback<ProgressScene, String> {

        public GetGalleryTokenListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(String result) {
            ProgressScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryTokenSuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            ProgressScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryTokenFailure(e);
            }
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof ProgressScene;
        }
    }
}