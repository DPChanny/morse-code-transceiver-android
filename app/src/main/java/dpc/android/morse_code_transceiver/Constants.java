package dpc.android.morse_code_transceiver;

//상수 정의

public class Constants {
    //문장의 시작과 끝을 나타내는 신호 시간
    public static final long sentence_start_end = 4000;
    //단어의 시작과 끝을 나타내는 신호 시간
    public static final long word_start_end = 3000;
    //0을 나타내는 신호 시간
    public static final long zero_bit = 1000;
    //1을 나타내는 신호 시간
    public static final long one_bit = 2000;
    //신호사이의 간격
    public static final long term = 500;
}