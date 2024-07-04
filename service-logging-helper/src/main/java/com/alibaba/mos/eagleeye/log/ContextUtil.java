package com.alibaba.mos.eagleeye.log;

import com.taobao.eagleeye.EagleEye;

/**
 * @author: Aci
 * @date: 9/8/21
 **/
public class ContextUtil {

    private static final String TESTING_FLAG = "t";
    private static final String TESTING_FLAG_VALUE = "1";

    /**
     * 是否测试：带有测试标
     * @return: result of is testing or not
     */
    public static boolean isTesting () {
        return TESTING_FLAG_VALUE.equals(EagleEye.getUserData(TESTING_FLAG));
    }
}
