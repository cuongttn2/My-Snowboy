package com.example.mysnowboy

enum class MsgEnum {
    MSG_VAD_END,
    MSG_VAD_NOSPEECH,
    MSG_VAD_SPEECH,
    MSG_VOLUME_NOTIFY,
    MSG_WAV_DATAINFO,
    MSG_RECORD_START,
    MSG_RECORD_STOP,
    MSG_ACTIVE,
    MSG_ERROR,
    MSG_INFO;

    companion object {
        fun getMsgEnum(i: Int): MsgEnum? {
            return MsgEnum.entries[i]
        }
    }
}