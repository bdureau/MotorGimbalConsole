package com.motorgimbalconsole;

/**
 *
 *   @description: Gimbal configuration
 *   @author: boris.dureau@neuf.fr
 *
 **/
public class GimbalConfigData {

    //Gimbal config variables
    private String AltimeterName = "RocketGimbal";
    private long ax_offset =0;
    private long ay_offset =0;
    private long az_offset =0;
    private long gx_offset =0;
    private long gy_offset =0;
    private long gz_offset =0;
    private double KpX= 0.0;
    private double KiX= 0.0;
    private double KdX= 0.0;
    private double KpY= 0.0;
    private double KiY= 0.0;
    private double KdY= 0.0;
    private int servoXMin = 0;
    private int servoXMax=120;
    private int servoYMin = 0;
    private int servoYMax=120;
    private long connectionSpeed= 38400;
    private int altimeterResolution = 0;
    private int eepromSize= 512;
    private int minorVersion =0;
    private int majorVersion =0;
    private int units = 0;
    private int endRecordAltitude =3;
    private int beepingFrequency = 440;
    private int liftOffDetect =0; //0 = barometer 1 =accelerometer

    public GimbalConfigData()
    {

    }

    public void setAltimeterName(String value)
    {
        AltimeterName = value;
    }
    public String getAltimeterName()
    {
        return AltimeterName;
    }

    public void setAxOffset(long value) {ax_offset = value;}
    public long getAxOffset()
    {
        return ax_offset;
    }

    public void setAyOffset(long value) {ay_offset = value;}
    public long getAyOffset()
    {
        return ay_offset;
    }

    public void setAzOffset(long value) {az_offset = value;}
    public long getAzOffset()
    {
        return az_offset;
    }

    public void setGxOffset(long value) {gx_offset = value;}
    public long getGxOffset()
    {
        return gx_offset;
    }

    public void setGyOffset(long value) {gy_offset = value;}
    public long getGyOffset()
    {
        return gy_offset;
    }

    public void setGzOffset(long value) {gz_offset = value;}
    public long getGzOffset()
    {
        return gz_offset;
    }

    public void setKpX(double value) {KpX =value;}
    public double getKpX()
    {
        return KpX;
    }

    public void setKiX(double value) {KiX =value;}
    public double getKiX()
    {
        return KiX;
    }

    public void setKdX(double value) {KdX =value;}
    public double getKdX()
    {
        return KdX;
    }

    public void setKpY(double value) {KpY =value;}
    public double getKpY()
    {
        return KpY;
    }

    public void setKiY(double value) {KiY =value;}
    public double getKiY()
    {
        return KiY;
    }

    public void setKdY(double value) {KdY =value;}
    public double getKdY()
    {
        return KdY;
    }
    public int getAltiMinorVersion()
    {
        return minorVersion;
    }
    public int getAltiMajorVersion()
    {
        return majorVersion;
    }
    public void setAltiMinorVersion(int value)
    {
        minorVersion=value;
    }
    public void setAltiMajorVersion(int value)
    {
        majorVersion=value;
    }



    //altimeter baud rate
    public void setConnectionSpeed(long value) {connectionSpeed = value;}
    public long getConnectionSpeed() {return connectionSpeed;}

    //Sensor resolution
    public void setAltimeterResolution(int value) {altimeterResolution = value;}
    public int getAltimeterResolution() {return altimeterResolution;}

    //eeprom size
    public void setEepromSize(int value) {eepromSize = value;}
    public int getEepromSize() {return eepromSize;}



    //index in an array
    public int arrayIndex (String stringArray[], String pattern) {

        for (int i =0; i < stringArray.length ; i++) {
            if(stringArray[i].equals(pattern))
                return i;
        }
        return -1;
    }

    //servos positions
    public void setServoXMin(int value)
    {
        servoXMin = value;
    }
    public int getServoXMin()
    {
        return servoXMin;
    }

    public void setServoXMax(int value)
    {
        servoXMax = value;
    }
    public int getServoXMax()
    {
        return servoXMax;
    }

    public void setServoYMin(int value)
    {
        servoYMin = value;
    }
    public int getServoYMin()
    {
        return servoYMin;
    }

    public void setServoYMax(int value) { servoYMax = value; }
    public int getServoYMax()
    {
        return servoYMax;
    }

    public void setUnits(int value)
    {
        units = value;
    }

    public int getUnits()
    {
        return units;
    }

    //beepingFrequency
    public void setBeepingFrequency(int value)
    {
        beepingFrequency = value;
    }
    public int getBeepingFrequency()
    {
        return beepingFrequency;
    }

    //Minimum recording Altitude
    public void setEndRecordAltitude(int value) {endRecordAltitude =value;}
    public int getEndRecordAltitude(){return endRecordAltitude;}

    //LiftOff detect
    public void setLiftOffDetect (int value)
    {
        liftOffDetect = value;
    }
    public int getLiftOffDetect()
    {
        return liftOffDetect;
    }


}
