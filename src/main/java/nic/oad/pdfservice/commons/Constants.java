package nic.oad.pdfservice.commons;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Constants {
    final String SIGN_ALIGNMENT_RIGHT_TO_LEFT = "RTL";
    final String SIGN_ALIGNMENT_LEFT_TO_RIGHT = "LTR";

    final String SIGN_REMARKS_NONE = "NONE";
    final String SIGN_REMARKS_LENGTH_SHORT = "SHORT";
    final String SIGN_REMARKS_LENGTH_MEDIUM = "MEDIUM";
    final String SIGN_REMARKS_LENGTH_LONG = "LONG";

    final int SIGN_REMARKS_LENGTH_SHORT_HEIGHT = 40;
    final int SIGN_REMARKS_LENGTH_MEDIUM_HEIGHT = 60;
    final int SIGN_REMARKS_LENGTH_LONG_HEIGHT = 80;

    final List<String> VALID_SIGN_REMARKS = Arrays.asList(SIGN_REMARKS_NONE, SIGN_REMARKS_LENGTH_SHORT, SIGN_REMARKS_LENGTH_MEDIUM, SIGN_REMARKS_LENGTH_LONG);

    final Map<String,Integer> SIGN_REMARKS_LENGTH_MAP = new HashMap<String, Integer>(){{
        put(SIGN_REMARKS_LENGTH_SHORT,SIGN_REMARKS_LENGTH_SHORT_HEIGHT);
        put(SIGN_REMARKS_LENGTH_MEDIUM,SIGN_REMARKS_LENGTH_MEDIUM_HEIGHT);
        put(SIGN_REMARKS_LENGTH_LONG,SIGN_REMARKS_LENGTH_LONG_HEIGHT);
    }};
}
