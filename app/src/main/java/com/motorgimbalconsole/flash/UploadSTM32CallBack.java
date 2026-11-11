package com.motorgimbalconsole.flash;

/**
 * UploadSTM32CallBack.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Callbacks of the STM32 firmware upload, used by FlashFirmware to update its
 * progress dialog (start, progress, messages, end, cancel, error).
 */
public interface UploadSTM32CallBack {

    /*
     * Callback methods
     */
    void onPreUpload();
    void onUploading(int value);
    void onInfo(String value);
    void onPostUpload(boolean success);
    void onCancel();
    void onError(UploadSTM32Errors err);

}
