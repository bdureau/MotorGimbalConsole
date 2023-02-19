package com.motorgimbalconsole.config.AppConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.config.AppConfigData;

public class AppConfigTab1Fragment extends Fragment {
    private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
    private Spinner spConnectionType,spGraphicsLibType;
    private CheckBox cbFullUSBSupport;
    private ConsoleApplication BT;
    private AppConfigData appConfigData;

    public AppConfigTab1Fragment(ConsoleApplication lBT) {
        BT = lBT;
    }
    public long getAppLanguage() {
        return spAppLanguage.getSelectedItemId();
    }
    public void setAppLanguage(int value) {
        spAppLanguage.setSelection(value);
    }

    public long getAppUnit(){
        return spAppUnit.getSelectedItemId();
    }
    public void setAppUnit(int value) {
        spAppUnit.setSelection(value);
    }

    public long getGraphColor() {
        return spGraphColor.getSelectedItemId();
    }
    public void setGraphColor(int value) {
        spGraphColor.setSelection(value);
    }

    public long getGraphBackColor() {
        return spGraphBackColor.getSelectedItemId();
    }
    public void setGraphBackColor(int value) {
        spGraphBackColor.setSelection(value);
    }

    public long getFontSize() {
        return spFontSize.getSelectedItemId();
    }
    public void setFontSize(int value) {
        spFontSize.setSelection(value);
    }

    public long getBaudRate() {
        return spBaudRate.getSelectedItemId();
    }
    public void setBaudRate(int value) {
        spBaudRate.setSelection(value);
    }

    public long getConnectionType() {
        return spConnectionType.getSelectedItemId();
    }
    public void setConnectionType(int value) {
        spConnectionType.setSelection(value);
    }

    public long getGraphicsLibType() {
        return spGraphicsLibType.getSelectedItemId();
    }
    public void setGraphicsLibType(int value) {
        spGraphicsLibType.setSelection(value);
    }
    public String getFullUSBSupport() {
        if (cbFullUSBSupport.isChecked())
            return "true";
        else
            return "false";
    }

    public void setFullUSBSupport(boolean value) {
        cbFullUSBSupport.setChecked(value);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_app_config_part1, container, false);
        // get the data for all the drop down
        appConfigData = new AppConfigData(view.getContext());
        //Language
        spAppLanguage = (Spinner)view.findViewById(R.id.spinnerLanguage);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
        spAppLanguage.setAdapter(adapter);

        // graph color
        spGraphColor = (Spinner)view.findViewById(R.id.spinnerGraphColor);
        // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphColor.setAdapter(adapterColor);
        // graph back color
        spGraphBackColor = (Spinner)view.findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

        spGraphBackColor.setAdapter(adapterGraphColor);
        //units
        spAppUnit = (Spinner)view.findViewById(R.id.spinnerUnits);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
        spAppUnit.setAdapter(adapter2);

        //font size
        spFontSize = (Spinner)view.findViewById(R.id.spinnerFontSize);

        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner)view.findViewById(R.id.spinnerBaudRate);

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner)view.findViewById(R.id.spinnerConnectionType);

        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
        spConnectionType.setAdapter(adapterConnectionType);

        //Graphics lib type
        spGraphicsLibType = (Spinner)view.findViewById(R.id.spinnerGraphicLibType);
        ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
        spGraphicsLibType.setAdapter(adapterGraphicsLibType);
        //Allow only telemetry via USB
        cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

        spAppLanguage.setSelection(Integer.parseInt(BT.getAppConf().getApplicationLanguage()));
        spAppUnit.setSelection(Integer.parseInt(BT.getAppConf().getUnits()));
        spGraphColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphColor()));
        spGraphBackColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphBackColor()));
        spFontSize.setSelection((Integer.parseInt(BT.getAppConf().getFontSize())-8));
        spBaudRate.setSelection(Integer.parseInt(BT.getAppConf().getBaudRate()));
        spConnectionType.setSelection(Integer.parseInt(BT.getAppConf().getConnectionType()));
        spGraphicsLibType.setSelection(Integer.parseInt(BT.getAppConf().getGraphicsLibType()));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //if android ver = 8 or greater use the MPlib so disable the choice and for it to use MP
            spGraphicsLibType.setSelection(1);
            spGraphicsLibType.setEnabled(false);
        }


        if (BT.getAppConf().getFullUSBSupport().equals("true")) {
            cbFullUSBSupport.setChecked(true);
        } else {
            cbFullUSBSupport.setChecked(false);
        }
        return view;
    }
}
