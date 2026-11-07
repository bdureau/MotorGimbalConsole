package com.motorgimbalconsole.flights;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.utils.Rocket;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import processing.android.PFragment;
import processing.core.PApplet;

/**
 * FlightPlayFragment.java
 *
 * Fragment handling the 3D playback simulation of rocket flight telemetry.
 * Animates rocket attitude (Euler angles: Pitch, Roll, Yaw) in synchronized time
 * using a Processing PApplet renderer.
 *
 * @author boris.dureau@neuf.fr
 */
public class FlightPlayFragment extends Fragment {

    private static final String TAG = "FlightPlayFragment";

    private ConsoleApplication consoleApp;
    private PApplet rocketRenderer;
    private PFragment processingFragment;
    private XYSeriesCollection allFlightData;
    private String flightName;

    private Button btnPlay;
    private View rootView;

    // Concurrency control: Lock object for pause/resume without CPU spinning
    private final Object playbackLock = new Object();
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Thread playbackThread;

    /**
     * Default empty constructor required by the Android framework.
     */
    public FlightPlayFragment() {
        super();
    }

    /**
     * Constructor used by ViewPager / Activity adapter.
     */
    public FlightPlayFragment(ConsoleApplication pBT, XYSeriesCollection pAllFlightData, String pFlightName) {
        this.consoleApp = pBT;
        this.allFlightData = pAllFlightData;
        this.flightName = pFlightName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_play_flight, container, false);
        btnPlay = rootView.findViewById(R.id.butPlay);

        if (consoleApp == null && getActivity() != null) {
            consoleApp = (ConsoleApplication) getActivity().getApplication();
        }

        if (allFlightData == null && consoleApp != null && flightName != null) {
            allFlightData = consoleApp.getFlightData().GetFlightData(flightName);
        }

        // Initialize Processing 3D canvas
        rocketRenderer = new Rocket();
        processingFragment = new PFragment(rocketRenderer);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.container, processingFragment)
                .commit();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
                btnPlay.setText(isPaused ? "PLAY" : "PAUSE");
            }
        });

        startPlaybackThread();

        return rootView;
    }

    /**
     * Toggles play/pause state safely using monitors, avoiding CPU-intensive spin loops.
     */
    private void togglePlayPause() {
        synchronized (playbackLock) {
            isPaused = !isPaused;
            if (!isPaused) {
                // Wake up thread sleeping in pause state
                playbackLock.notifyAll();
            }
        }
    }

    /**
     * Starts the telemetry playback thread with time synchronization.
     */
    private void startPlaybackThread() {
        stopPlaybackThread();

        isRunning = true;
        isPaused = false;

        playbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                executePlaybackLoop();
            }
        }, "TelemetryPlaybackThread");

        playbackThread.start();
    }

    /**
     * Main telemetry playback loop reading Euler curves and streaming angles to the 3D rocket model.
     */
    private void executePlaybackLoop() {
        if (allFlightData == null) {
            return;
        }

        XYSeries eulerX = allFlightData.getSeries("Euler X");
        XYSeries eulerY = allFlightData.getSeries("Euler Y");
        XYSeries eulerZ = allFlightData.getSeries("Euler Z");

        if (eulerX == null || eulerY == null || eulerZ == null) {
            Log.w(TAG, "Euler telemetry curves not found in flight dataset.");
            return;
        }

        int sampleCount = eulerX.getItemCount();
        if (sampleCount == 0) {
            return;
        }

        while (isRunning) {
            long prevTime = eulerX.getX(0).longValue();

            for (int i = 0; i < sampleCount; i++) {
                if (!isRunning) {
                    return;
                }

                // Thread-safe pause check without busy-waiting
                synchronized (playbackLock) {
                    while (isPaused && isRunning) {
                        try {
                            playbackLock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }

                long currentTime = eulerX.getX(i).longValue();
                long deltaTime = currentTime - prevTime;
                prevTime = currentTime;

                // Guard sleep duration: avoid negative intervals or huge delays on initial sample
                if (i > 0 && deltaTime > 0) {
                    try {
                        Thread.sleep(Math.min(deltaTime, 1000)); // Cap single sleep interval to 1s
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                float x = eulerX.getY(i).floatValue();
                float y = eulerY.getY(i).floatValue();
                float z = eulerZ.getY(i).floatValue();

                if (rocketRenderer instanceof Rocket) {
                    ((Rocket) rocketRenderer).setInputString(x, y, z, currentTime);
                }
            }
        }
    }

    /**
     * Safely halts the playback thread and wakes it if it was waiting on pause or sleep.
     */
    private void stopPlaybackThread() {
        isRunning = false;
        isPaused = false;

        synchronized (playbackLock) {
            playbackLock.notifyAll();
        }

        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (processingFragment != null) {
            processingFragment.onResume();
        }
        if (rocketRenderer != null) {
            rocketRenderer.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause playback when navigating away from the fragment
        synchronized (playbackLock) {
            isPaused = true;
        }
        if (processingFragment != null) {
            processingFragment.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlaybackThread();
        if (processingFragment != null) {
            processingFragment.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlaybackThread();
        if (rocketRenderer != null) {
            rocketRenderer.onDestroy();
            rocketRenderer = null;
        }
    }
}