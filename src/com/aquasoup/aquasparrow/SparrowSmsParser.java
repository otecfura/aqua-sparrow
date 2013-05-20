package com.aquasoup.aquasparrow;

import java.util.ArrayList;

/**
 * Created by otec on 20.5.13.
 */
public class SparrowSmsParser {

    private enum SmsCodes{
        OPEN_VALVE("start!");

        private final String commandText;

        private SmsCodes(String commandText) {
            this.commandText = commandText;
        }

        private String getString() {
            return commandText;
        }

    }

    private ArrayList<SmsListener> listenerArrayList=new ArrayList<SmsListener>();

    public void addListener(SmsListener smsListener){
       listenerArrayList.add(smsListener);
    }

    public void checkSmsCode(String smsCode){
        if(smsIsTooShort(smsCode)){
            smsCodeIs(false);
        }else{
            String smsCodeObtained=smsCode.substring(0,6);
            smsCodeIs(smsCodeObtained.equals(SmsCodes.OPEN_VALVE.getString()));
        }
    }

    private boolean smsIsTooShort(String smsCode){
        return smsCode.length()<6;
    }

    private void smsCodeIs(boolean bool){
        for(SmsListener smsListener:listenerArrayList){
            if(bool){
                smsListener.openValve();
            }else{
                smsListener.badSmsCode();
            }
        }
    }

    public interface SmsListener{
        void openValve();
        void badSmsCode();
    }
}
