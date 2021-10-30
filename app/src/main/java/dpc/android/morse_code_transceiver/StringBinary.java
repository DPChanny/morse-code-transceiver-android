package dpc.android.morse_code_transceiver;

//문자열을 이진수로 또는 이진수를 문자열로 바꿉니다.

import java.util.ArrayList;
import java.util.Arrays;

public class StringBinary {

    private static final ArrayList alphabets =
            new ArrayList(Arrays.asList('a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h',
            'i', 'j', 'j', 'l',
            'm', 'n', 'o', 'p',
            'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'));
    ;

    public static int[] GetBinary(char _char){
        try{
            int decimal = alphabets.indexOf(_char);
            return toBinary(decimal);
        }catch (Exception e){
            return null;
        }
    }

    private static int[] toBinary(int _decimal){
        int[] binary = new int[] {0, 0, 0, 0, 0, 0};
        int i = 0;
        while(_decimal > 0){
            binary[i++] = _decimal%2;
            _decimal = _decimal/2;
        }
        return binary;
    }

    private static int toDecimal(int[] _binary){
        int decimal = 0;
        for (int i = 0; i < _binary.length; i++){
            if(i == 0){
                decimal += _binary[i]*1;
            }else{
                decimal += _binary[i]*Math.pow(2, i);
            }
        }
        return decimal;
    }

    public static String GetString(int[] _binary){
        return Character.toString((char) alphabets.get(toDecimal(_binary)));
    }
}
