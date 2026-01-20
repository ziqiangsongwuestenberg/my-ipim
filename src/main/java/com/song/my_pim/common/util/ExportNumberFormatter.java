package com.song.my_pim.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ExportNumberFormatter {

    private ExportNumberFormatter() {}

    public static String decimal2(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String decimal4(BigDecimal v) {
        return v == null ? null : v.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
}

