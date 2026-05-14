/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 *
 * This file is DroidBridge project code.
 * It is not part of Minecraft and does not grant rights to Minecraft,
 * Mojang, Microsoft, PojavLauncher, Zalith Launcher, or any third-party project.
 *
 * Files written entirely by DNA Mobile Applications are proprietary unless
 * a file header or separate license notice states otherwise.
 */

package ca.dnamobile.javalauncher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.Tools;

import java.util.ArrayList;
import java.util.List;

import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.feature.unpack.AbstractUnpackTask;
import ca.dnamobile.javalauncher.feature.unpack.Components;
import ca.dnamobile.javalauncher.feature.unpack.Jre;
import ca.dnamobile.javalauncher.feature.unpack.UnpackComponentsTask;
import ca.dnamobile.javalauncher.feature.unpack.UnpackJreTask;
import ca.dnamobile.javalauncher.feature.unpack.UnpackSingleFilesTask;
import ca.dnamobile.javalauncher.launcher.JavaLaunch;
import ca.dnamobile.javalauncher.utils.path.PathManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private TextView titleText;
    private TextView statusText;
    private volatile boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        titleText = findViewById(R.id.textTitle);
        statusText = findViewById(R.id.textStatus);
        titleText.setText(R.string.app_name);

        PathManager.initContextConstants(this);

        if (!Tools.checkStorageRoot()) {
            setStatus(getString(R.string.splash_screen_storage_unavailable));
            return;
        }

        startInstallThread();
    }

    @Override
    protected void onDestroy() {
        finished = true;
        super.onDestroy();
    }

    private void startInstallThread() {
        Thread installThread = new Thread(() -> {
            try {
                runInstallFlow();
                openMainActivity();
            } catch (Throwable throwable) {
                Logging.e("SplashActivity", "Launcher preparation failed", throwable);
                setStatus(getString(R.string.splash_screen_failed, throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName()));
            }
        }, "JavaLauncher Unpack");
        installThread.start();
    }

    private void runInstallFlow() {
        setStatus(getString(R.string.splash_screen_checking));

        List<TaskEntry> tasks = buildTasks();
        int total = tasks.size();
        int index = 0;

        for (TaskEntry entry : tasks) {
            index++;
            setStatus(getString(R.string.splash_screen_checking_item, index, total, entry.name));
            if (!entry.task.isNeedUnpack()) {
                continue;
            }

            setStatus(getString(R.string.splash_screen_installing_item, index, total, entry.name));
            entry.task.run();
        }

        setStatus(getString(R.string.splash_screen_finalizing));
        new UnpackSingleFilesTask(this).run();
    }

    @NonNull
    private List<TaskEntry> buildTasks() {
        ArrayList<TaskEntry> tasks = new ArrayList<>();

        for (Components component : Components.values()) {
            UnpackComponentsTask task = new UnpackComponentsTask(this, component);
            if (!task.isCheckFailed()) {
                tasks.add(new TaskEntry(component.displayName, task));
            }
        }

        for (Jre jre : Jre.values()) {
            UnpackJreTask task = new UnpackJreTask(this, jre);
            if (!task.isCheckFailed()) {
                tasks.add(new TaskEntry(jre.jreName, task));
            }
        }

        return tasks;
    }

    private void openMainActivity() {
        if (finished) return;
        runOnUiThread(() -> {
            if (finished) return;
            setStatus(getString(R.string.splash_screen_done));
            startActivity(new Intent(this, JavaLaunch.class));
            finish();
        });
    }

    private void setStatus(@NonNull String status) {
        if (finished) return;
        runOnUiThread(() -> {
            if (!finished && statusText != null) {
                statusText.setText(status);
            }
        });
    }

    private static final class TaskEntry {
        final String name;
        final AbstractUnpackTask task;

        TaskEntry(@NonNull String name, @NonNull AbstractUnpackTask task) {
            this.name = name;
            this.task = task;
        }
    }
}
