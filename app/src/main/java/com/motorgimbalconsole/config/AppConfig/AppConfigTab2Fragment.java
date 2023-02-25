package com.motorgimbalconsole.config.AppConfig;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import java.util.Locale;

public class AppConfigTab2Fragment extends Fragment {
    private CheckBox cbDrogueEvent, cbAltitudeEvent, cbLandingEvent;
    private CheckBox cbBurnoutEvent, cbWarningEvent, cbApogeeAltitude;
    private CheckBox cbLiftOffEvent;
    private ConsoleApplication BT;
    private Button btnTestVoice;
    private TextToSpeech mTTS;
    private Spinner spTelemetryVoice;
    private int nbrVoices = 0;
    private boolean ViewCreated = false;

    public AppConfigTab2Fragment(ConsoleApplication lBT) {
        BT = lBT;
    }

    public boolean getDrogueEvent() {
        return cbDrogueEvent.isChecked();
    }

    public void setDrogueEvent(boolean value) {
        cbDrogueEvent.setChecked(value);
    }

    public boolean getAltitudeEvent() {
        return cbAltitudeEvent.isChecked();
    }

    public void setAltitudeEvent(boolean value) {
        cbAltitudeEvent.setChecked(value);
    }

    public boolean getLandingEvent() {
        return cbLandingEvent.isChecked();
    }

    public void setLandingEvent(boolean value) {
        cbLandingEvent.setChecked(value);
    }

    public boolean getBurnoutEvent() {
        return cbBurnoutEvent.isChecked();
    }

    public void setBurnoutEvent(boolean value) {
        cbBurnoutEvent.setChecked(value);
    }

    public boolean getWarningEvent() {
        return cbWarningEvent.isChecked();
    }

    public void setWarningEvent(boolean value) {
        cbWarningEvent.setChecked(value);
    }

    public boolean getApogeeAltitude() {
        return cbApogeeAltitude.isChecked();
    }

    public void setApogeeAltitude(boolean value) {
        cbApogeeAltitude.setChecked(value);
    }

    public boolean getLiftOffEvent() {
        return cbLiftOffEvent.isChecked();
    }

    public void setLiftOffEvent(boolean value) {
        cbLiftOffEvent.setChecked(value);
    }

    public void setVoices(String itemsVoices[]) {
        nbrVoices = itemsVoices.length;
        ArrayAdapter<String> adapterVoice = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, itemsVoices);
        spTelemetryVoice.setAdapter(adapterVoice);
        if (BT.getAppConf().getTelemetryVoice() < nbrVoices)
            spTelemetryVoice.setSelection(BT.getAppConf().getTelemetryVoice());
    }

    public void setTelemetryVoice(int value) {
        if (value < nbrVoices)
            this.spTelemetryVoice.setSelection(value);
    }

    public int getTelemetryVoice() {
        return (int) this.spTelemetryVoice.getSelectedItemId();
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_app_config_part2, container, false);
        //cbMainEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent1);
        cbDrogueEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent2);
        cbAltitudeEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent3);
        cbLandingEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent4);
        cbBurnoutEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent5);
        cbWarningEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent6);
        cbApogeeAltitude = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent7);
        //cbMainAltitude = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent8);
        cbLiftOffEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent9);
        spTelemetryVoice = (Spinner) view.findViewById(R.id.spinnerTelemetryVoice);

        cbDrogueEvent.setChecked(BT.getAppConf().getDrogue_event());

        cbAltitudeEvent.setChecked(BT.getAppConf().getAltitude_event());

        cbLandingEvent.setChecked(BT.getAppConf().getLanding_event());

        cbBurnoutEvent.setChecked(BT.getAppConf().getBurnout_event());

        cbWarningEvent.setChecked(BT.getAppConf().getWarning_event());

        cbApogeeAltitude.setChecked(BT.getAppConf().getApogee_altitude());

        //cbLiftOffEvent
        cbLiftOffEvent.setChecked(BT.getAppConf().getLiftOff_event());

        btnTestVoice = (Button) view.findViewById(R.id.butTestVoice);
        btnTestVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //init text to speech
                mTTS = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            int result = 0;

                            if (Locale.getDefault().getLanguage().equals("en"))
                                result = mTTS.setLanguage(Locale.ENGLISH);
                            else if (Locale.getDefault().getLanguage().equals("fr"))
                                result = mTTS.setLanguage(Locale.FRENCH);

                            else if (Locale.getDefault().getLanguage().equals("nl"))
                                result = mTTS.setLanguage(new Locale("nl_NL"));
                            else if (Locale.getDefault().getLanguage().equals("it"))
                                result = mTTS.setLanguage(getResources().getConfiguration().locale);
                            else if (Locale.getDefault().getLanguage().equals("ru"))
                                result = mTTS.setLanguage(getResources().getConfiguration().locale);
                            else
                                result = mTTS.setLanguage(Locale.ENGLISH);

                            if (BT.getAppConf().getTelemetryVoice() !=-1 ) {
                                Log.d("Voice", BT.getAppConf().getTelemetryVoice()+"");
                                try {
                                    for (Voice tmpVoice : mTTS.getVoices()) {
                                        Log.d("Voice", tmpVoice.getName());
                                        if (tmpVoice.getName().equals(spTelemetryVoice.getSelectedItem().toString())) {
                                            mTTS.setVoice(tmpVoice);
                                            Log.d("Voice", "Found voice");
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    msg(Locale.getDefault().getLanguage());
                                }
                            }
                            mTTS.setPitch(1.0f);
                            mTTS.setSpeechRate(1.0f);
                            if (Locale.getDefault().getLanguage().equals("en"))
                                mTTS.speak("Bearaltimeter altimeters are the best", TextToSpeech.QUEUE_FLUSH, null);

                            if (Locale.getDefault().getLanguage().equals("fr"))
                                mTTS.speak("Les altimètres Bearaltimeter sont les meilleurs", TextToSpeech.QUEUE_FLUSH, null);
                            if (Locale.getDefault().getLanguage().equals("es"))
                                mTTS.speak("Los altimietros Bearaltimeter son los mejores", TextToSpeech.QUEUE_FLUSH, null);

                            if (Locale.getDefault().getLanguage().equals("nl"))
                                mTTS.speak("De Bearaltimeter-hoogtemeters zijn de beste", TextToSpeech.QUEUE_FLUSH, null);
                            if (Locale.getDefault().getLanguage().equals("it"))
                                mTTS.speak("Gli altimetri Bearaltimeter sono i migliori", TextToSpeech.QUEUE_FLUSH, null);
                            if (Locale.getDefault().getLanguage().equals("ru"))
                                mTTS.speak("Медвежатник - это лучшее", TextToSpeech.QUEUE_FLUSH, null);
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("TTS", "Language not supported");
                            } else {
                                //msg(Locale.getDefault().getLanguage());
                            }
                        } else {
                            Log.e("TTS", "Init failed");
                        }
                    }
                });

            }
        });
        ViewCreated = true;
        return view;
    }
    private void msg(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
    }
}
