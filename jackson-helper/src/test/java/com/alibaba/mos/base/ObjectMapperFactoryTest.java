package com.alibaba.mos.base;

import com.alibaba.mos.base.ObjectMapperFactory;
import org.junit.Test;

public class ObjectMapperFactoryTest {
    @Test
    public void testGetInstance(){
        for(int i=0;i<100;i++){
            ObjectMapperFactory.getInstance().getObjectMapper();
        }
    }
}
